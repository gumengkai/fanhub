#!/bin/bash
# FunHub 后端开发启动脚本
# 启动 Flask 后端服务（开发模式）

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"
BACKEND_DIR="$PROJECT_DIR/backend"

echo "🚀 FunHub 后端开发服务"
echo "======================"
echo "📁 项目目录：$PROJECT_DIR"
echo ""

# 检查虚拟环境
if [ ! -d "$BACKEND_DIR/venv" ]; then
    echo "⚠️  虚拟环境不存在，正在创建..."
    cd "$BACKEND_DIR"
    python3 -m venv venv
    ./venv/bin/pip install --upgrade pip
    ./venv/bin/pip install -r requirements.txt
fi

# 激活虚拟环境
source "$BACKEND_DIR/venv/bin/activate"

# 设置环境变量
export FLASK_APP=run.py
export FLASK_ENV=development
export PYTHONUNBUFFERED=1
export DATABASE_PATH="$PROJECT_DIR/storage/database/funhub.db"
export THUMBNAIL_FOLDER="$PROJECT_DIR/storage/thumbnails"
export CORS_ORIGINS="http://localhost:5173,http://127.0.0.1:5173,http://localhost:8080,http://127.0.0.1:8080"
export SECRET_KEY="dev-secret-key-for-local-development"

# 创建必要的目录
mkdir -p "$PROJECT_DIR/storage/database"
mkdir -p "$PROJECT_DIR/storage/thumbnails"
mkdir -p "$PROJECT_DIR/logs"

echo "✅ 环境变量已设置"
echo "   - DATABASE_PATH: $DATABASE_PATH"
echo "   - THUMBNAIL_FOLDER: $THUMBNAIL_FOLDER"
echo "   - CORS_ORIGINS: $CORS_ORIGINS"
echo ""

# 初始化数据库（如果不存在）
if [ ! -f "$DATABASE_PATH" ]; then
    echo "📦 初始化数据库..."
    cd "$BACKEND_DIR"
    python3 -c "from app import create_app, db; app = create_app(); db.create_all()"
    echo "✅ 数据库初始化完成"
    echo ""
fi

# 启动后端服务
echo "🔥 启动 Flask 开发服务器..."
echo "   - 地址：http://localhost:5000"
echo "   - 调试模式：启用"
echo "   - 自动重载：启用"
echo ""
echo "按 Ctrl+C 停止服务"
echo "======================"

cd "$BACKEND_DIR"
python3 run.py
