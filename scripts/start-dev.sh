#!/bin/bash
# FunHub 开发模式启动脚本
# 同时启动前端和后端开发服务

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"

echo "🚀 FunHub 开发环境启动"
echo "======================"
echo "📁 项目目录：$PROJECT_DIR"
echo ""

# 检查并创建必要目录
mkdir -p "$PROJECT_DIR/storage/database"
mkdir -p "$PROJECT_DIR/storage/thumbnails"
mkdir -p "$PROJECT_DIR/logs"

# 颜色定义
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# 清理函数
cleanup() {
    echo ""
    echo -e "${YELLOW}正在停止服务...${NC}"
    
    if [ -n "$BACKEND_PID" ] && kill -0 "$BACKEND_PID" 2>/dev/null; then
        echo "  停止后端服务 (PID: $BACKEND_PID)"
        kill "$BACKEND_PID" 2>/dev/null || true
    fi
    
    if [ -n "$FRONTEND_PID" ] && kill -0 "$FRONTEND_PID" 2>/dev/null; then
        echo "  停止前端服务 (PID: $FRONTEND_PID)"
        kill "$FRONTEND_PID" 2>/dev/null || true
    fi
    
    echo -e "${GREEN}✅ 所有服务已停止${NC}"
    exit 0
}

# 捕获退出信号
trap cleanup SIGINT SIGTERM EXIT

# 启动后端服务
echo -e "${BLUE}[1/2] 启动后端服务...${NC}"
cd "$PROJECT_DIR/backend"

# 设置环境变量
export FLASK_APP=run.py
export FLASK_ENV=development
export PYTHONUNBUFFERED=1
export DATABASE_PATH="$PROJECT_DIR/storage/database/funhub.db"
export THUMBNAIL_FOLDER="$PROJECT_DIR/storage/thumbnails"
export CORS_ORIGINS="http://localhost:5173,http://127.0.0.1:5173,http://localhost:8080,http://127.0.0.1:8080"
export SECRET_KEY="dev-secret-key-for-local-development"

# 检查虚拟环境
if [ ! -d "$PROJECT_DIR/backend/venv" ]; then
    echo -e "${YELLOW}⚠️  虚拟环境不存在，正在创建...${NC}"
    python3 -m venv "$PROJECT_DIR/backend/venv"
    "$PROJECT_DIR/backend/venv/bin/pip" install --upgrade pip
    "$PROJECT_DIR/backend/venv/bin/pip" install -r "$PROJECT_DIR/backend/requirements.txt"
fi

# 启动后端
source "$PROJECT_DIR/backend/venv/bin/activate"
cd "$PROJECT_DIR/backend"
python3 run.py > "$PROJECT_DIR/logs/backend.log" 2>&1 &
BACKEND_PID=$!
echo -e "${GREEN}✅ 后端服务已启动 (PID: $BACKEND_PID)${NC}"
echo "   日志：$PROJECT_DIR/logs/backend.log"
echo ""

# 等待后端启动
echo "   等待后端服务就绪..."
for i in {1..30}; do
    if curl -s http://localhost:5000/api/health > /dev/null 2>&1; then
        echo -e "${GREEN}✅ 后端服务就绪${NC}"
        break
    fi
    if [ $i -eq 30 ]; then
        echo -e "${RED}❌ 后端服务启动超时${NC}"
        echo "   查看日志：tail -f $PROJECT_DIR/logs/backend.log"
        exit 1
    fi
    sleep 1
done
echo ""

# 启动前端服务
echo -e "${BLUE}[2/2] 启动前端服务...${NC}"
cd "$PROJECT_DIR/frontend"

# 检查 node_modules
if [ ! -d "$PROJECT_DIR/frontend/node_modules" ]; then
    echo -e "${YELLOW}⚠️  依赖未安装，正在安装...${NC}"
    cd "$PROJECT_DIR/frontend"
    npm install
fi

# 启动前端
cd "$PROJECT_DIR/frontend"
npm run dev > "$PROJECT_DIR/logs/frontend.log" 2>&1 &
FRONTEND_PID=$!
echo -e "${GREEN}✅ 前端服务已启动 (PID: $FRONTEND_PID)${NC}"
echo "   日志：$PROJECT_DIR/logs/frontend.log"
echo ""

# 等待前端启动
echo "   等待前端服务就绪..."
for i in {1..30}; do
    if curl -s http://localhost:5173 > /dev/null 2>&1; then
        echo -e "${GREEN}✅ 前端服务就绪${NC}"
        break
    fi
    if [ $i -eq 30 ]; then
        echo -e "${RED}❌ 前端服务启动超时${NC}"
        echo "   查看日志：tail -f $PROJECT_DIR/logs/frontend.log"
        exit 1
    fi
    sleep 1
done
echo ""

# 显示访问信息
echo "======================"
echo -e "${GREEN}🎉 开发环境启动完成！${NC}"
echo ""
echo -e "${BLUE}📍 访问地址:${NC}"
echo "   前端：http://localhost:5173"
echo "   后端：http://localhost:5000"
echo "   API:  http://localhost:5000/api/health"
echo ""
echo -e "${BLUE}📝 日志文件:${NC}"
echo "   后端：$PROJECT_DIR/logs/backend.log"
echo "   前端：$PROJECT_DIR/logs/frontend.log"
echo ""
echo -e "${YELLOW}按 Ctrl+C 停止所有服务${NC}"
echo "======================"
echo ""

# 等待用户中断
wait
