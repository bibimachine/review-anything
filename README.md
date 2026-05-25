# Review Anything

<p align="center">
  <b>Markdown 笔记 AI 复习工具</b> — 把你的笔记变成智能复习卡片
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Web-React%20+%20FastAPI-blue" alt="Web">
  <img src="https://img.shields.io/badge/Android-Jetpack%20Compose-green" alt="Android">
  <img src="https://img.shields.io/badge/Windows-WPF%20.NET%208-purple" alt="Windows">
  <img src="https://img.shields.io/badge/LLM-DeepSeek%20%7C%20OpenAI%20Compatible-orange" alt="LLM">
  <img src="https://img.shields.io/badge/License-MIT-yellow" alt="License">
</p>

---

## ✨ 功能特性

- **📚 笔记上传** — 拖拽 ZIP 包，自动解压并解析 Markdown 文件
- **🤖 AI 生成 QA** — 调用大语言模型，为每个知识点生成复习问答对
- **🔄 间隔重复** — 基于复习次数的智能调度算法（1天 → 3天 → 7天 → 14天 → 30天）
- **📊 打卡统计** — 可视化日历记录每日复习进度
- **⚡ 增量更新** — 内容哈希去重，只处理变更部分，节省 API 费用
- **🛡️ 失败容错** — LLM 生成失败自动创建占位卡片，后台持续重试
- **📱 跨平台** — Web / Android / Windows 三端数据互通

---

## 📦 下载安装

| 平台 | 下载 | 说明 |
|------|------|------|
| **Web** | [Releases](https://github.com/bibimachine/review-anything/releases) | 解压后 `./start.sh` 一键启动 |
| **Android** | [Releases](https://github.com/bibimachine/review-anything/releases) | 下载 APK 直接安装 |
| **Windows** | [Releases](https://github.com/bibimachine/review-anything/releases) | 解压后运行 EXE |

---

## 🚀 快速开始

### Web 版（推荐，功能最全）

**前置要求**：Python 3.12+、uv (`pip install uv`)

```bash
# 1. 从 Releases 下载 Web 版 ZIP，解压
cd review-anything-web

# 2. 启动（Linux/Mac）
./start.sh

# 3. 浏览器访问 http://localhost:8000
```

Windows 用户直接双击 `start.bat`。首次启动会自动安装依赖，请保持网络畅通。

### Android 版

```bash
# 从 Releases 下载 APK，安装到手机
# 或使用 adb
adb install ReviewAnything-Android-v0.1.x.apk
```

### Windows 版

```bash
# 从 Releases 下载 ZIP，解压后运行 ReviewAnything.exe
```

---

## 📖 使用教程

### 第一步：配置 LLM（可选）

首次打开 Web 或 Windows 版本会弹出配置窗口：

| 配置项 | 示例值 |
|--------|--------|
| API Base URL | `https://api.deepseek.com` 或 `https://api.openai.com/v1` |
| API Key | 你的 API Key |
| 模型名称 | `deepseek-chat` / `gpt-4` 等 |
| 每日复习数量 | 默认 10 题 |

> 💡 点击「暂不配置」也能使用，系统会用本地规则生成基础复习卡片（效果不如 LLM）。

### 第二步：上传笔记

1. 切换到「笔记管理」页
2. 将你的 Markdown 笔记打包成 **ZIP 文件**
3. 拖拽或点击上传
4. 系统会自动：解压 → 按标题分块 → 生成 QA 复习卡片

> 📁 ZIP 内的文件夹名会被识别为「板块」，方便按主题复习。

### 第三步：开始复习

1. 回到「每日复习」页
2. 选择复习范围（全部 / 某个板块 / 随机）
3. 点击「开始复习」
4. 看问题 → 回忆答案 → 点击「查看答案」→ 标记「记住了」或「忘记了」

**间隔重复算法**：
- ✅ 记住 → 1天 → 3天 → 7天 → 14天 → 30天
- ❌ 忘记 → 1小时后再次复习

### 第四步：坚持打卡

左侧边栏有打卡按钮和日历，记录你的复习坚持天数 🔥

---

## 🏗️ 技术架构

### 整体架构

```
┌─────────────┐   ┌─────────────┐   ┌─────────────┐
│   Web       │   │  Android    │   │   Windows   │
│  React 18   │   │  Compose    │   │    WPF      │
│  Vite       │   │  Room       │   │  EF Core    │
│  Tailwind   │   │  Retrofit   │   │ HttpClient  │
└──────┬──────┘   └──────┬──────┘   └──────┬──────┘
       │                 │                 │
       └─────────────────┼─────────────────┘
                         │  REST API
                         ▼
              ┌─────────────────────┐
              │   FastAPI Backend   │
              │   SQLAlchemy        │
              │   SQLite (WAL)      │
              │   AsyncOpenAI SDK   │
              └─────────────────────┘
```

### 三端技术栈对比

| 维度 | Web | Android | Windows |
|------|-----|---------|---------|
| UI | React 18 + Tailwind CSS | Jetpack Compose | WPF (XAML) |
| 状态管理 | React Hooks | ViewModel + StateFlow | MVVM + INotifyPropertyChanged |
| 本地存储 | SQLite (后端) | Room (SQLite) | EF Core + SQLite |
| 网络 | Axios + SSE | Retrofit + OkHttp | HttpClient |
| 构建工具 | Vite | Gradle | .NET SDK |

---

## 📁 项目结构

```
review-anything/
├── web/                    # Web 全栈应用
│   ├── backend/            # FastAPI + SQLAlchemy + SQLite
│   │   ├── app/routers/    # REST API 路由
│   │   ├── app/services/   # LLM调用、Markdown解析、ZIP解压
│   │   └── pyproject.toml  # Python 依赖
│   └── frontend/           # React 18 + Vite + Tailwind CSS
│       ├── src/components/ # 复习卡片、上传面板、设置页、打卡日历
│       └── src/api.js      # Axios API 封装
│
├── android/                # Android 原生应用
│   └── app/src/main/java/com/reviewanything/app/
│       ├── data/           # Room 实体 + DAO + Repository
│       ├── service/        # LLM调用 + Markdown解析 + ZIP解压
│       ├── ui/screens/     # 复习/上传/设置/打卡/笔记管理
│       └── viewmodel/      # ViewModel + Factory
│
├── windows/                # Windows 桌面应用
│   └── ReviewAnything/
│       ├── Models/         # EF Core 实体
│       ├── Services/       # DbContext + LLM + Review + 解析
│       ├── ViewModels/     # MVVM 视图模型
│       └── Views/          # MainWindow + 5个 Page
│
├── shared/                 # 共用资源
│   ├── api-spec/           # OpenAPI 规范
│   └── db-schema/          # SQLite 表结构定义
│
└── .github/workflows/      # CI/CD 自动构建 Release
```

---

## 🧠 核心流程

### 1. 上传笔记 → 生成复习卡片

```
用户上传 ZIP
  → 后端解压遍历 .md 文件
  → Markdown 按标题分块 (≤800字符)
  → 内容哈希对比，跳过未变更部分
  → 调用 LLM 生成 QA 对 (串行，最多2并发)
  → 保存到 SQLite
  → SSE 实时推送进度到前端
```

### 2. 每日复习 → 间隔重复

```
用户打开复习页面
  → 后端按优先级调度:
    ① 易忘项 (is_hard) 优先
    ② 到期项 (next_review_at ≤ now)
    ③ 新项 (review_count = 0)
  → 显示问题卡片
  → 用户操作 remember/forget
  → 更新间隔: forget→1小时后, remember→1/3/7/14/30天
```

### 3. 增量更新

```
重新上传同一文件
  → 对比 content_hash
  → 未变更: 完全跳过
  → 有变更: 只处理变更的 chunk
  → 未变更 chunk 的 ReviewItem 自动保留
```

---

## ⚙️ 开发指南

### 启动 Web 开发环境

```bash
cd web/backend && uv run uvicorn app.main:app --reload   # 后端 :8000
cd web/frontend && npm run dev                            # 前端 :5173
```

### 启动 Android

```bash
cd android
./gradlew assembleDebug
```

### 启动 Windows

> ⚠️ WPF 需在 Windows 环境编译运行

```bash
cd windows
dotnet run --project ReviewAnything/ReviewAnything.csproj
```

---

## 📝 待完善

- [ ] 各平台应用图标 / 启动图精细化
- [ ] Android 发布签名 (keystore)
- [ ] Windows 安装包 (MSI / Setup.exe)
- [ ] 单元测试覆盖
- [ ] 支持更多笔记格式 (.txt, .org)

---

## 📄 License

MIT License

---

<p align="center">
  Made with ❤️ for learners who want to remember anything.
</p>
