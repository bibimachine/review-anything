# Review Anything - 项目开发指南

> 本文档由代码分析生成，供开发者快速理解项目架构与核心逻辑。

---

## 一、项目概述

**Review Anything** 是一个基于 Markdown 笔记的 AI 驱动复习工具。

**核心流程**：用户上传 Obsidian 格式的笔记（zip 包）→ 系统自动按标题分块 → 调用大语言模型生成问答复习卡片 → 通过间隔重复算法帮助用户记忆。

**技术栈**：
- **后端**：FastAPI + SQLAlchemy + SQLite + aiohttp
- **前端**：React 18 + Vite + Tailwind CSS + Axios
- **启动**：`./start.sh` 同时启动后端（:8000）和前端（:5173）

---

## 二、后端架构

### 2.1 目录结构

```
backend/
├── app/
│   ├── __init__.py
│   ├── main.py           # FastAPI 应用入口，注册路由和 CORS
│   ├── database.py       # SQLite 引擎、Session、Base
│   ├── models.py         # 4 个数据模型
│   ├── routers/
│   │   ├── config.py     # LLM 配置 CRUD
│   │   ├── notes.py      # 笔记上传、解析、管理
│   │   └── review.py     # 复习调度、动作处理、统计
│   └── services/
│       ├── llm.py        # 调用大模型生成 QA
│       └── parser.py     # Markdown 分块与哈希
├── pyproject.toml        # 依赖：fastapi, uvicorn, sqlalchemy, pydantic, aiohttp, aiofiles
└── .venv/
```

### 2.2 数据模型（models.py）

```
Config (1条记录)
  ├── api_base_url, api_key, model_name

Note (笔记文件)
  ├── file_path, file_name, section(板块), content, content_hash
  └── chunks → [Chunk]

Chunk (内容分块)
  ├── note_id, content, content_hash, heading_path("第一章/第一节")
  └── review_items → [ReviewItem]

ReviewItem (复习卡片)
  ├── chunk_id, question, answer
  ├── is_hard(易忘标记), review_count, llm_failed
  ├── next_review_at, last_reviewed_at
```

**关系**：Note → 多个 Chunk → 多个 ReviewItem（级联删除）

### 2.3 核心服务详解

#### parser.py - Markdown 解析与分块

| 函数 | 职责 |
|------|------|
| `parse_markdown(content)` | 按 `#` 标题层级分块，记录 `heading_path`，再调用 `split_by_paragraphs` 切小 |
| `split_by_paragraphs(text, heading_path)` | 将大块按段落/句子切分，每块 ≤ 800 字符 |
| `extract_section_from_path(file_path)` | 从文件路径取倒数第二个文件夹名作为板块 |
| `compute_hash(text)` | SHA256 哈希，用于内容变更检测 |

**分块策略**：
1. 先按 Markdown 标题分块（记录层级路径）
2. 每个标题块再按段落累积，超过 800 字符则按句子切分
3. 保证每个 chunk 都是语义完整的片段

#### llm.py - 大模型调用

| 函数 | 职责 |
|------|------|
| `get_llm_config(db)` | 从 Config 表读取 API 配置 |
| `generate_qa(chunk, heading, db)` | 生成单个问答（temperature=0.7） |
| `generate_multiple_qa(chunk, heading, db, max_qa=3)` | 生成最多 3 个不同角度的问答（temperature=0.8） |
| `parse_json_response(content)` | 从 LLM 返回中提取 JSON（支持代码块、纯文本） |

**Prompt 设计**：
- System Prompt 要求 LLM 考察理解而非简单记忆
- 必须返回 JSON 格式：`[{question, answer}, ...]`
- 超时：单 QA 60s，多 QA 90s

### 2.4 路由详解

#### config.py - 配置管理

```
GET  /api/config/     → {configured: bool, config?: {...}}
POST /api/config/     → 保存/更新配置（只存1条）
DELETE /api/config/   → 删除配置
```

#### notes.py - 笔记管理

```
POST   /api/notes/upload              → 上传 zip，解压解析所有 md
GET    /api/notes/sections            → 获取所有板块名（去重）
GET    /api/notes/section/{name}      → 获取某板块下所有笔记
DELETE /api/notes/{id}                → 删除笔记（级联删除 chunk + review_item）
PUT    /api/notes/{id}                → 更新单个笔记（重新上传 md）
```

**上传处理流程**：
```
接收 zip → 保存到 ./uploads → 解压到 ./uploads/extracted
→ 遍历所有 .md 文件
  → 计算 content_hash
  → 检查是否已存在（同 file_path）
    → 存在且 hash 相同：跳过（unchanged）
    → 存在但 hash 不同：删除旧 chunks，重新解析（updated）
    → 不存在：新建 Note，解析分块（created）
  → 对每个 chunk：
    → 检查是否有相同 hash 的现有 chunk
      → 有且 heading_path 相同：保留原有 review_items（复用）
      → 无或不同：调用 LLM 生成最多 3 个 QA
        → 成功：创建 ReviewItem
        → 失败：创建占位 ReviewItem（llm_failed=True）
→ commit → 清理临时文件
```

#### review.py - 复习调度

```
GET  /api/review/next?section=xxx     → 获取下一个复习项
POST /api/review/{id}/action          → 提交动作（remember/forget）
GET  /api/review/stats                → 获取统计信息
```

**获取下一个复习项的优先级**：
1. `is_hard=True` 且 `llm_failed=False` → 按 `last_reviewed_at` 升序（nulls first）
2. `next_review_at <= now` 且 `llm_failed=False` → 按 `next_review_at` 升序
3. `review_count=0` 且 `llm_failed=False` → 新项
4. 无 → 返回 "没有更多需要复习的内容"

**每次获取前**：自动重试最多 3 个 `llm_failed=True` 的项

**复习动作处理**：
| 动作 | 效果 |
|------|------|
| `forget` | `is_hard=True`，`next_review_at = now + 1小时` |
| `remember` | `is_hard=False`，按复习次数安排：1天→3天→7天→14天→30天 |

**统计信息**：
- `total`: 总 ReviewItem 数
- `hard`: 易忘项数
- `today_due`: 已到复习时间的项数
- `failed`: LLM 生成失败的项数

---

## 三、前端架构

### 3.1 目录结构

```
frontend/
├── src/
│   ├── main.jsx              # React 入口
│   ├── App.jsx               # 根组件：三栏布局 + 标签页
│   ├── api.js                # Axios 封装，所有 API 调用
│   ├── index.css             # Tailwind 基础样式
│   └── components/
│       ├── ConfigModal.jsx   # 首次配置弹窗
│       ├── Sidebar.jsx       # 左侧边栏：统计 + 板块选择
│       ├── ReviewCard.jsx    # 复习卡片：问题/答案/操作
│       ├── UploadPanel.jsx   # 拖拽上传 zip
│       ├── NotesManager.jsx  # 笔记列表：更新/删除
│       └── SettingsPanel.jsx # 设置页：查看/删除配置
├── package.json              # react, react-router-dom, axios, tailwindcss, vite
├── vite.config.js
├── tailwind.config.js
└── index.html
```

### 3.2 组件详解

#### App.jsx - 根组件

**状态管理**：
```
configured        → 是否已完成 LLM 配置
selectedSection   → 当前选中板块（null 表示全部）
currentItem       → 当前显示的复习项
activeTab         → 'review' | 'manage' | 'settings'
refreshKey        → 用于强制刷新 Sidebar 的数据
```

**布局**：
```
┌─────────────────────────────────────────┐
│  Sidebar (w-64)  │  顶部导航 (review/manage/settings)  │
│                  ├─────────────────────────────────────┤
│  📚 每日复习      │                                     │
│  ┌───┬───┬───┐  │         主内容区                     │
│  │总 │易 │待 │  │    (ReviewCard / NotesManager /     │
│  │ 50│ 5 │ 12│  │     SettingsPanel)                  │
│  └───┴───┴───┘  │                                     │
│  ─────────────   │                                     │
│  全部板块        │                                     │
│  前端            │                                     │
│  后端            │                                     │
│  算法            │                                     │
└─────────────────────────────────────────┘
```

#### api.js - API 封装

```javascript
API_BASE = 'http://localhost:8000'

configApi = {
  get: () => GET /api/config/
  save: (data) => POST /api/config/
  delete: () => DELETE /api/config/
}

notesApi = {
  upload: (file) => POST /api/notes/upload (multipart)
  getSections: () => GET /api/notes/sections
  getNotesBySection: (section) => GET /api/notes/section/{section}
  deleteNote: (id) => DELETE /api/notes/{id}
  updateNote: (id, file) => PUT /api/notes/{id} (multipart)
}

reviewApi = {
  getNext: (section) => GET /api/review/next?section={section}
  action: (id, action) => POST /api/review/{id}/action {action}
  getStats: () => GET /api/review/stats
}
```

#### ReviewCard.jsx - 复习卡片

**交互流程**：
```
显示问题
  → 点击"😵 忘记了" → 立即标记 forget → 显示答案 → 点击"继续下一个"
  → 点击"😊 记住了" → 显示答案 → 点击"继续下一个" → 标记 remember
```

**显示信息**：
- 板块标签（蓝色）
- 易忘标签（红色，仅 is_hard 时）
- 标题路径（灰色，如"第一章/第一节"）
- 问题内容
- 答案区域（绿色背景）
- 笔记出处（蓝色背景）

#### Sidebar.jsx - 侧边栏

**功能**：
- 每 5 秒轮询刷新统计数据和板块列表
- 显示三个统计卡片：总条目 / 易忘 / 待复习
- 板块列表："全部板块" + 各板块名
- 点击板块 → 触发 `onSelectSection` → App 更新 `selectedSection`

#### NotesManager.jsx - 笔记管理

**功能**：
- 根据 `selectedSection` 加载该板块下的笔记列表
- 显示每个笔记的文件名和 chunk 数量
- 支持重新上传单个 `.md` 文件更新笔记
- 支持删除笔记（带确认对话框）

#### ConfigModal.jsx - 配置弹窗

**行为**：
- 组件挂载时自动检查配置（`GET /api/config/`）
- 未配置 → 弹出模态框，要求填写 API Base URL、API Key、模型名称
- 已配置 → 调用 `onConfigured()` → App 开始加载复习项

#### SettingsPanel.jsx - 设置页

**功能**：
- 显示当前 LLM 配置（只读）
- 支持删除配置（二次确认）
- 删除后触发 `onConfigDeleted()` → App 刷新页面

---

## 四、核心数据流

### 4.1 首次使用流程

```
打开页面
  → ConfigModal 检查配置
    → 未配置：弹出配置窗
      → 用户填写 → POST /api/config/
      → onConfigured() → App 调用 loadNextItem()
    → 已配置：直接进入复习
```

### 4.2 上传笔记流程

```
用户拖拽/选择 zip 文件
  → UploadPanel 调用 notesApi.upload()
  → 后端解压、解析、生成 QA
  → 返回导入结果
  → alert 成功信息
  → onUploadSuccess() → App 增加 refreshKey → Sidebar 刷新
```

### 4.3 复习流程

```
用户点击"每日复习"或"下一个"
  → GET /api/review/next?section=xxx
    → 后端自动重试 failed 项
    → 按优先级返回下一个复习项
  → ReviewCard 显示问题
    → 用户操作 → POST /api/review/{id}/action
      → 后端更新复习状态（is_hard, next_review_at）
    → ReviewCard 调用 onNext() → 加载下一项
```

### 4.4 笔记更新流程

```
用户在 NotesManager 点击更新图标
  → 选择新的 .md 文件
  → PUT /api/notes/{id}
  → 后端对比 content_hash
    → 相同：返回"未变更"
    → 不同：删除旧 chunks，重新解析，复用未变更 chunk 的 QA
  → 刷新笔记列表
```

---

## 五、关键设计决策

### 5.1 内容哈希复用机制

**目的**：避免重复调用 LLM，节省 API 费用和时间。

**实现**：
- Note 级别：上传时对比 `content_hash`，未变更则跳过整个文件
- Chunk 级别：解析后对比 `content_hash` + `heading_path`，未变更则保留原有 `ReviewItem`
- 更新笔记时：只重新处理变更的 chunk

### 5.2 LLM 失败容错

**场景**：网络问题、API 限流、返回格式错误

**处理**：
- 生成失败时创建占位 `ReviewItem`（`llm_failed=True`）
- 占位内容：`question="[生成失败，将自动重试]"`，`answer=原始内容`
- 每次获取下一个复习项时，自动重试最多 3 个失败项
- 重试成功后替换为正常 QA，失败则保持占位状态

### 5.3 间隔重复算法

**简化版 SM-2**：
```
复习次数: 1   2   3   4   5+
间隔天数: 1   3   7   14  30
```

**易忘项**：无论复习次数，标记后 1 小时再次复习

### 5.4 分块大小控制

- 最大 chunk 大小：800 字符
- 先按标题分块，再按段落累积，最后按句子切分
- 保证每个 chunk 语义完整，适合生成 QA

---

## 六、扩展建议

| 方向 | 建议 |
|------|------|
| 支持更多格式 | 在 `parser.py` 中添加 `.txt`、`.org` 解析 |
| 更复杂的间隔算法 | 实现完整的 SM-2 或 FSRS 算法 |
| 复习历史 | 新增 `ReviewHistory` 模型记录每次复习详情 |
| 多用户支持 | 为所有模型添加 `user_id` 外键 |
| 前端优化 | 添加复习进度条、今日完成数量统计 |
| 导入导出 | 支持 Anki 格式导入导出 |

---

## 七、快速参考

### 启动项目
```bash
./start.sh
# 后端: http://localhost:8000
# 前端: http://localhost:5173
# API 文档: http://localhost:8000/docs
```

### 数据库位置
```
backend/review_anything.db  (SQLite)
```

### 上传文件临时目录
```
backend/uploads/
```

### 关键常量
```python
# parser.py
MAX_CHUNK_SIZE = 800

# llm.py
generate_qa timeout = 60s
generate_multiple_qa timeout = 90s
max_qa = 3

# review.py
retry_failed_limit = 3
intervals = [1, 3, 7, 14, 30]  # 天
```
