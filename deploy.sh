#!/bin/bash
# fanhub 一键部署脚本 (纯 Docker 版本)

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

echo "🚀 fanhub 一键部署 (纯 Docker)"
echo "=============================="
echo ""

# 检查 Docker
if ! command -v docker &> /dev/null; then
    echo "❌ 错误：Docker 未安装"
    exit 1
fi

echo "✅ Docker 版本：$(docker --version)"
echo ""

# 创建存储目录
echo "📁 创建存储目录..."
mkdir -p storage/database storage/thumbnails

# 停止并删除旧容器
echo "🛑 停止旧容器..."
docker stop fanhub 2>/dev/null || true
docker rm fanhub 2>/dev/null || true

# 构建镜像
echo "🔨 构建 Docker 镜像（首次构建约需 5-10 分钟）..."
docker build -t fanhub:latest .

# 启动容器
echo "🚀 启动容器..."
docker run -d \
    --name fanhub \
    --restart unless-stopped \
    -p 8080:8080 \
    -v "$(pwd)/storage:/app/storage" \
    -v /media:/media:ro \
    -e FLASK_ENV=production \
    -e SECRET_KEY=fanhub-secret-key-change-in-production \
    -e DATABASE_PATH=/app/storage/database/fanhub.db \
    -e THUMBNAIL_FOLDER=/app/storage/thumbnails \
    -e CORS_ORIGINS=http://localhost,http://127.0.0.1,http://192.168.31.133:8080,http://192.168.31.133:5000 \
    -p 5000:5000 \
    fanhub:latest

# 等待服务就绪
echo ""
echo "⏳ 等待服务启动..."
sleep 10

# 健康检查
echo "🏥 健康检查..."
MAX_RETRIES=10
RETRY=0
while [ $RETRY -lt $MAX_RETRIES ]; do
    if curl -s http://localhost:8080/api/health > /dev/null 2>&1; then
        echo "✅ 服务健康检查通过！"
        break
    fi
    RETRY=$((RETRY + 1))
    echo "   等待中... ($RETRY/$MAX_RETRIES)"
    sleep 3
done

if [ $RETRY -eq $MAX_RETRIES ]; then
    echo "⚠️  健康检查超时，但服务可能仍在启动中"
    echo "   查看日志：docker logs fanhub"
fi

echo ""
echo "==================================="
echo "🎉 部署完成！"
echo "==================================="
echo ""
echo "📍 访问地址：http://localhost:8080"
echo ""
echo "常用命令："
echo "  查看状态：docker ps | grep fanhub"
echo "  查看日志：docker logs fanhub"
echo "  停止服务：docker stop fanhub"
echo "  重启服务：docker restart fanhub"
echo "  删除容器：docker rm -f fanhub"
echo ""
