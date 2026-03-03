#!/bin/bash
# FunHub 前端开发启动脚本
# 启动 Vite 前端开发服务器（热重载模式）

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"
FRONTEND_DIR="$PROJECT_DIR/frontend"

echo "🚀 FunHub 前端开发服务"
echo "======================"
echo "📁 项目目录：$PROJECT_DIR"
echo ""

# 检查 Node.js
if ! command -v node &> /dev/null; then
    echo "❌ 错误：Node.js 未安装"
    echo "   请先安装 Node.js 20+"
    exit 1
fi

# 检查 npm
if ! command -v npm &> /dev/null; then
    echo "❌ 错误：npm 未安装"
    exit 1
fi

echo "✅ Node.js 版本：$(node --version)"
echo "✅ npm 版本：$(npm --version)"
echo ""

# 检查 node_modules
if [ ! -d "$FRONTEND_DIR/node_modules" ]; then
    echo "⚠️  依赖未安装，正在安装..."
    cd "$FRONTEND_DIR"
    npm install
fi

# 启动前端服务
echo "🔥 启动 Vite 开发服务器..."
echo "   - 地址：http://localhost:5173"
echo "   - 后端代理：http://localhost:5000"
echo "   - 热重载：启用"
echo ""
echo "按 Ctrl+C 停止服务"
echo "======================"

cd "$FRONTEND_DIR"
npm run dev
