#!/bin/bash

# 一键启动脚本
# 同时启动后端和前端服务

PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"
WEB_DIR="$PROJECT_DIR"

echo "================================"
echo "  Review Anything 启动脚本"
echo "================================"

# 检查 uv
echo "检查 uv 环境..."
if ! command -v uv &> /dev/null; then
    echo "错误: 未找到 uv，请先安装 uv"
    echo "安装方式: curl -LsSf https://astral.sh/uv/install.sh | sh"
    exit 1
fi

# 检查 Node.js
echo "检查 Node.js 环境..."
if ! command -v node &> /dev/null; then
    echo "错误: 未找到 node，请先安装 Node.js"
    exit 1
fi

# 安装后端依赖
echo ""
echo "安装后端依赖..."
cd "$WEB_DIR/backend"
if [ ! -d ".venv" ]; then
    uv venv
fi
uv sync

# 安装前端依赖
echo ""
echo "安装前端依赖..."
cd "$WEB_DIR/frontend"
if [ ! -d "node_modules" ]; then
    npm install
fi

# 启动后端
echo ""
echo "启动后端服务 (http://localhost:8000)..."
cd "$WEB_DIR/backend"
uv run uvicorn app.main:app --host 0.0.0.0 --port 8000 &
BACKEND_PID=$!

# 启动前端
echo "启动前端服务 (http://localhost:5173)..."
cd "$WEB_DIR/frontend"
npx vite dev --host &
FRONTEND_PID=$!

echo ""
echo "================================"
echo "  服务已启动!"
echo "  后端: http://localhost:8000"
echo "  前端: http://localhost:5173"
echo "================================"
echo ""
echo "按 Ctrl+C 停止所有服务"

# 捕获退出信号
trap "echo ''; echo '正在停止服务...'; kill \$BACKEND_PID \$FRONTEND_PID 2>/dev/null; exit 0" INT TERM

# 等待
wait
