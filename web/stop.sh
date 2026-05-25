#!/bin/bash

# 停止 Review Anything 所有服务

echo "================================"
echo "  停止 Review Anything 服务"
echo "================================"

# 停止后端 uvicorn（包括 uv run 启动的 Python 子进程）
BACKEND_PIDS=$(pgrep -f "uvicorn app.main:app")
if [ -n "$BACKEND_PIDS" ]; then
    echo "停止后端服务..."
    for PID in $BACKEND_PIDS; do
        echo "  发送终止信号到 PID: $PID"
        kill $PID 2>/dev/null
    done
    sleep 1
    # 检查是否还有残留进程
    for PID in $BACKEND_PIDS; do
        if kill -0 $PID 2>/dev/null; then
            kill -9 $PID 2>/dev/null
            echo "  PID $PID 已强制停止"
        fi
    done
    echo "后端服务已停止"
else
    echo "后端服务未运行"
fi

# 停止前端 vite
FRONTEND_PID=$(pgrep -f "vite" | head -1)
if [ -n "$FRONTEND_PID" ]; then
    echo "停止前端服务 (PID: $FRONTEND_PID)..."
    kill $FRONTEND_PID 2>/dev/null
    sleep 1
    if kill -0 $FRONTEND_PID 2>/dev/null; then
        kill -9 $FRONTEND_PID 2>/dev/null
        echo "前端服务已强制停止"
    else
        echo "前端服务已停止"
    fi
else
    echo "前端服务未运行"
fi

# 清理可能残留的数据库锁文件
WEB_DIR="$(cd "$(dirname "$0")" && pwd)"
if [ -f "$WEB_DIR/backend/review_anything.db-wal" ]; then
    echo "清理数据库 WAL 文件..."
    rm -f "$WEB_DIR/backend/review_anything.db-wal" "$WEB_DIR/backend/review_anything.db-shm" 2>/dev/null
fi

echo ""
echo "================================"
echo "  所有服务已停止"
echo "================================"
