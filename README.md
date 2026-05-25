# Review Anything

Markdown 笔记 AI 复习工具。支持 Web、Android、Windows 三端。

## 项目结构

```
review-anything/
├── web/              # Web 版本 (React + FastAPI) ✅
│   ├── backend/      # FastAPI + SQLAlchemy + SQLite
│   └── frontend/     # React 18 + Vite + Tailwind CSS
├── android/          # Android 版本 (Kotlin + Jetpack Compose) ✅
│   └── app/src/main/java/com/reviewanything/app/
│       ├── data/     # Room 数据库 (5实体 + 5DAO + Repository)
│       ├── service/  # LLM调用 + Markdown解析 + ZIP解压
│       ├── ui/screens/   # 复习/上传/设置/打卡/笔记管理
│       └── viewmodel/    # 7个ViewModel + Factory
├── windows/          # Windows 版本 (C# + WPF) ✅
│   └── ReviewAnything/
│       ├── Models/   # 5个EF Core实体
│       ├── Services/ # DbContext + LLM + Review + Markdown + ZIP
│       ├── ViewModels/   # Main + Review + Upload + Settings + CheckIn + Notes
│       └── Views/        # MainWindow + 5个Page
├── shared/           # 共用资源
│   ├── api-spec/     # OpenAPI 规范
│   └── db-schema/    # SQLite 表结构
└── scripts/          # 构建脚本
```

## 快速开始

### Web

```bash
cd web
./start.sh
```
访问 http://localhost:5173

### Android

```bash
cd android
export ANDROID_SDK_ROOT=$HOME/android-sdk
export PATH=$PATH:$ANDROID_SDK_ROOT/cmdline-tools/latest/bin
./gradlew assembleDebug
```
APK: `android/app/build/outputs/apk/debug/app-debug.apk`

### Windows

```bash
cd windows
dotnet build ReviewAnything.sln
```

## 功能清单

| 功能 | Web | Android | Windows |
|------|-----|---------|---------|
| 笔记上传 (ZIP) | ✅ | ✅ | ✅ |
| Markdown 解析 | ✅ | ✅ | ✅ |
| LLM 生成 QA | ✅ | ✅ | ✅ |
| 每日复习 | ✅ | ✅ | ✅ |
| 间隔重复算法 | ✅ | ✅ | ✅ |
| 打卡日历 | ✅ | ✅ | ✅ |
| 模型配置 | ✅ | ✅ | ✅ |
| 笔记管理 | ✅ | ✅ | ✅ |

## 技术栈

| 平台 | UI | 架构 | 数据库 | 网络 |
|------|-----|------|--------|------|
| Web | React + Tailwind | Component | SQLAlchemy + SQLite | axios + SSE |
| Android | Jetpack Compose | MVVM + ViewModel | Room (SQLite) | Retrofit + OkHttp |
| Windows | WPF (XAML) | MVVM | EF Core + SQLite | HttpClient |

## 待完善

- [ ] 各平台应用图标/启动图
- [ ] GitHub Actions CI/CD
- [ ] Android 发布签名 (keystore)
- [ ] Windows 安装包 (MSI/Setup.exe)
- [ ] 单元测试
