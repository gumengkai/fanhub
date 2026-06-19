#!/bin/bash

# FanHub Docker 更新脚本
# 更新镜像时保留数据库和缩略图数据

set -e

IMAGE_FILE="${1:-fanhub-latest.tar.gz}"
CONTAINER_NAME="fanhub"
DATA_DIR="${HOME}/fanhub-data"

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${GREEN}=== FanHub Docker 更新脚本 ===${NC}"
echo ""

# 检查镜像文件
if [ ! -f "$IMAGE_FILE" ]; then
    echo -e "${RED}错误: 镜像文件不存在: $IMAGE_FILE${NC}"
    echo "使用方法: $0 <镜像文件路径>"
    echo "例如: $0 fanhub-20250522-scanfix.tar.gz"
    exit 1
fi

# 创建数据目录（如果不存在）
echo -e "${YELLOW}1. 检查数据目录...${NC}"
mkdir -p "$DATA_DIR/database"
mkdir -p "$DATA_DIR/thumbnails"
echo -e "${GREEN}   数据目录: $DATA_DIR${NC}"

# 备份现有数据库（如果存在）
echo -e "${YELLOW}2. 备份现有数据库...${NC}"
if [ -f "$DATA_DIR/database/fanhub.db" ]; then
    BACKUP_FILE="$DATA_DIR/database/fanhub-backup-$(date +%Y%m%d-%H%M%S).db"
    cp "$DATA_DIR/database/fanhub.db" "$BACKUP_FILE"
    echo -e "${GREEN}   数据库已备份到: $BACKUP_FILE${NC}"
else
    echo -e "${GREEN}   暂无现有数据库，跳过备份${NC}"
fi

# 停止并删除旧容器
echo -e "${YELLOW}3. 停止并删除旧容器...${NC}"
if docker ps -q -f name="$CONTAINER_NAME" | grep -q .; then
    docker stop "$CONTAINER_NAME"
    echo -e "${GREEN}   容器已停止${NC}"
fi

if docker ps -aq -f name="$CONTAINER_NAME" | grep -q .; then
    docker rm "$CONTAINER_NAME"
    echo -e "${GREEN}   容器已删除${NC}"
fi

# 加载新镜像
echo -e "${YELLOW}4. 加载新镜像...${NC}"
docker load < "$IMAGE_FILE"
echo -e "${GREEN}   镜像加载完成${NC}"

# 启动新容器
echo -e "${YELLOW}5. 启动新容器...${NC}"
docker run -d \
  --name "$CONTAINER_NAME" \
  --restart unless-stopped \
  -p 11303:8080 \
  -p 11304:5000 \
  -v "$DATA_DIR/database:/app/storage/database" \
  -v "$DATA_DIR/thumbnails:/app/storage/thumbnails" \
  -v /mnt/fan/videos:/mnt/fan/videos:ro \
  -e DATABASE_PATH=/app/storage/database/fanhub.db \
  -e THUMBNAIL_FOLDER=/app/storage/thumbnails \
  fanhub:latest

echo -e "${GREEN}   容器已启动${NC}"

# 等待服务启动
echo -e "${YELLOW}6. 等待服务启动...${NC}"
sleep 5

# 检查健康状态
if curl -sf http://localhost:11303/api/sources > /dev/null 2>&1; then
    echo -e "${GREEN}   服务运行正常!${NC}"
else
    echo -e "${RED}   服务可能未正常启动，请检查日志: docker logs $CONTAINER_NAME${NC}"
fi

echo ""
echo -e "${GREEN}=== 更新完成 ===${NC}"
echo ""
echo "访问地址:"
echo "  - Web 界面: http://localhost:11303"
echo "  - API 接口: http://localhost:11304"
echo ""
echo "数据存储位置:"
echo "  - 数据库: $DATA_DIR/database/fanhub.db"
echo "  - 缩略图: $DATA_DIR/thumbnails"
