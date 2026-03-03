# FunHub Docker 部署指南

🚀 **单容器部署方案** - 前后端一体化，简单快捷

---

## 📋 前置要求

- Docker 20.10+
- 至少 2GB 可用内存
- 至少 10GB 磁盘空间

---

## 🎯 快速部署（推荐）

### 一键部署

```bash
cd /home/gmk/funhub

# 一键部署
chmod +x deploy.sh
./deploy.sh

# 访问应用
# 浏览器打开：http://localhost:8080
```

### 验证部署

```bash
# 检查容器状态
docker ps | grep funhub

# 检查健康状态
curl http://localhost:8080/api/health

# 查看实时日志
docker logs funhub -f
```

---

## 📦 手动部署步骤

### 步骤 1：准备媒体目录（可选）

如果需要访问本地媒体文件，确保目录存在：

```bash
mkdir -p /media
```

### 步骤 2：创建存储目录

```bash
mkdir -p storage/database storage/thumbnails
```

### 步骤 3：构建镜像

```bash
# 首次构建（约 5-10 分钟）
docker build -t funhub:latest .
```

### 步骤 4：启动容器

```bash
docker run -d \
    --name funhub \
    --restart unless-stopped \
    -p 8080:8080 \
    -v "$(pwd)/storage:/app/storage" \
    -v /media:/media:ro \
    -e FLASK_ENV=production \
    -e SECRET_KEY=funhub-secret-key-change-in-production \
    -e DATABASE_PATH=/app/storage/database/funhub.db \
    -e THUMBNAIL_FOLDER=/app/storage/thumbnails \
    -e CORS_ORIGINS=http://localhost,http://127.0.0.1 \
    funhub:latest
```

### 步骤 5：初始化数据库

首次启动时会自动初始化数据库。如需手动初始化：

```bash
docker exec -it funhub /app/venv/bin/python /app/backend/migrate.py
```

---

## 🔧 常用命令

### 启动/停止

```bash
# 启动
docker start funhub

# 停止
docker stop funhub

# 重启
docker restart funhub

# 重新构建并启动
docker build -t funhub:latest . && docker rm -f funhub && docker run -d --name funhub -p 8080:8080 -v "$(pwd)/storage:/app/storage" funhub:latest
```

### 查看状态

```bash
# 容器状态
docker ps | grep funhub

# 资源使用
docker stats funhub

# 健康检查
curl http://localhost:8080/api/health
```

### 查看日志

```bash
# 实时日志
docker logs funhub -f

# 最近 100 行
docker logs funhub --tail=100

# 仅后端日志
docker exec funhub cat /var/log/supervisor/flask.out.log

# 仅前端日志
docker exec funhub cat /var/log/supervisor/nginx.out.log
```

### 进入容器

```bash
# 进入容器 shell
docker exec -it funhub bash

# 查看目录结构
ls -la /app

# 检查服务状态
supervisorctl status
```

---

## 🗄️ 数据持久化

### 存储目录结构

```
storage/
├── database/
│   └── funhub.db      # SQLite 数据库
└── thumbnails/         # 视频缩略图缓存
```

### 备份数据

```bash
# 备份数据库
cp storage/database/funhub.db storage/database/funhub.db.backup

# 备份缩略图
tar -czf thumbnails-backup.tar.gz storage/thumbnails/
```

### 恢复数据

```bash
# 停止容器
docker stop funhub

# 恢复数据库
cp storage/database/funhub.db.backup storage/database/funhub.db

# 重启容器
docker start funhub
```

---

## 🌐 网络配置

### 默认端口

| 服务 | 端口 | 说明 |
|------|------|------|
| Web 界面 | 8080 | 浏览器访问 |
| API | 8080/api/ | 后端接口 |

### 修改端口

```bash
# 使用不同端口（例如 3000）
docker run -d --name funhub -p 3000:8080 ...
```

### 域名访问

1. 配置 DNS 指向服务器 IP
2. 修改 `CORS_ORIGINS` 环境变量：
   ```bash
   -e CORS_ORIGINS=https://your-domain.com
   ```

---

## 🐛 故障排查

### 容器无法启动

```bash
# 查看详细错误
docker logs funhub

# 检查端口占用
netstat -tlnp | grep 8080

# 检查 Docker 日志
journalctl -u docker
```

### 无法访问 Web 界面

```bash
# 检查容器是否运行
docker ps | grep funhub

# 检查健康状态
curl -v http://localhost:8080/api/health

# 检查防火墙
sudo ufw status
sudo ufw allow 8080
```

### 后端 API 错误

```bash
# 查看后端日志
docker exec funhub cat /var/log/supervisor/flask.err.log

# 重启后端服务
docker exec funhub supervisorctl restart flask

# 检查数据库
docker exec funhub ls -la /app/storage/database/
```

### 前端页面空白

```bash
# 查看前端日志
docker exec funhub cat /var/log/supervisor/nginx.err.log

# 重建镜像
docker build -t funhub:latest . --no-cache

# 清除浏览器缓存后重试
```

### 缩略图无法生成

```bash
# 检查 ffmpeg
docker exec funhub ffmpeg -version

# 检查存储权限
docker exec funhub ls -la /app/storage/

# 修复权限
docker exec funhub chmod -R 755 /app/storage/
```

---

## 🔐 安全建议

### 生产环境配置

1. **修改密钥**
   ```bash
   -e SECRET_KEY=$(openssl rand -hex 32)
   ```

2. **限制 CORS**
   ```bash
   -e CORS_ORIGINS=https://your-domain.com
   ```

3. **使用 HTTPS**（需要反向代理）
   ```
   浏览器 ← HTTPS → Nginx 反向代理 ← HTTP → FunHub 容器
   ```

4. **定期更新**
   ```bash
   docker build -t funhub:latest .
   docker stop funhub && docker rm funhub
   docker run -d --name funhub -p 8080:8080 ...
   ```

---

## 📊 性能优化

### 内存限制

如果内存不足，限制容器内存：

```bash
docker run -d --name funhub --memory="2g" --memory-reservation="1g" ...
```

### 缩略图缓存

定期清理过期缩略图：

```bash
docker exec funhub find /app/storage/thumbnails -mtime +7 -delete
```

---

## 📝 环境变量

| 变量名 | 默认值 | 说明 |
|--------|--------|------|
| `FLASK_ENV` | `production` | Flask 运行环境 |
| `SECRET_KEY` | `funhub-secret-key...` | Flask 密钥 |
| `DATABASE_PATH` | `/app/storage/database/funhub.db` | 数据库路径 |
| `THUMBNAIL_FOLDER` | `/app/storage/thumbnails` | 缩略图目录 |
| `CORS_ORIGINS` | `http://localhost,...` | 允许的跨域来源 |

---

## 🆘 获取帮助

```bash
# 查看部署状态
./status.sh

# 查看快速帮助
cat DEPLOY.md

# 查看容器信息
docker inspect funhub
```

---

**部署完成后，访问 http://localhost:8080 开始使用 FunHub！** 🎉
