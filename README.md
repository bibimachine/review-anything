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

## 🖼️ 界面预览

> 截图占位，欢迎补充实际运行截图

| Web (React) | Android (Compose) | Windows (WPF) |
|-------------|-------------------|---------------|
| 待补充 | 待补充 | 待补充 |

---

## 🚀 快速开始

### Web 版

```bash
cd web
./start.sh
```
- 前端: http://localhost:5173
- 后端 API: http://localhost:8000
- API 文档: http://localhost:8000/docs

### Android 版

```bash
cd android
export ANDROID_SDK_ROOT=$HOME/android-sdk
export PATH=$PATH:$ANDROID_SDK_ROOT/cmdline-tools/latest/bin
./gradlew assembleDebug
```
APK 输出: `android/app/build/outputs/apk/debug/app-debug.apk`

### Windows 版

> ⚠️ WPF 需在 Windows 环境编译运行，WSL 中仅能编写代码

```bash
cd windows
dotnet build ReviewAnything.sln
```

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
| 状态管理 | React Hooks + useState | ViewModel + StateFlow | MVVM + INotifyPropertyChanged |
| 本地存储 | SQLite (后端) | Room (SQLite) | EF Core + SQLite |
| 网络 | Axios + SSE | Retrofit + OkHttp | HttpClient |
| 构建工具 | Vite | Gradle | .NET SDK |

---

## 📁 项目结构

```
review-anything/
├── web/                    # Web 全栈应用
│   ├── backend/            # FastAPI + SQLAlchemy + SQLite
│   │   ├── app/routers/    # REST API 路由 (config, notes, review, checkin)
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
│       ├── Models/         # 5个 EF Core 实体
│       ├── Services/       # DbContext + LLM + Review + 解析
│       ├── ViewModels/     # MVVM 视图模型
│       └── Views/          # MainWindow + 5个 Page
│
├── shared/                 # 共用资源
│   ├── api-spec/           # OpenAPI 规范 (从 FastAPI 导出)
│   └── db-schema/          # SQLite 表结构定义
│
└── .gitignore              # 已配置 node_modules, .venv, bin, obj 等
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

## 📖 详细文档

- [PROJECT_GUIDE.md](./PROJECT_GUIDE.md) — 完整开发指南、API 文档、架构设计

---

## ⚙️ LLM 配置

支持任意 OpenAI 兼容 API，默认推荐 **DeepSeek**:

| 配置项 | 示例值 |
|--------|--------|
| API Base URL | `https://api.deepseek.com` |
| API Key | 你的 DeepSeek API Key |
| 模型名称 | `deepseek-v4-pro` |

> 后端会自动拼接 `/chat/completions` 路径，兼容带后缀和不带后缀的 URL。

---

## 📝 待完善

- [ ] 各平台应用图标 / 启动图
- [ ] GitHub Actions CI/CD (Android APK / Windows EXE 自动构建)
- [ ] Android 发布签名 (keystore)
- [ ] Windows 安装包 (MSI / Setup.exe)
- [ ] 单元测试覆盖
- [ ] 实际运行截图替换占位图

---

## 📄 License

MIT License

---

<p align="center">
  Made with ❤️ for learners who want to remember anything.
</p>
