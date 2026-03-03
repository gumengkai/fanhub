
---

## 📱 Android 客户端配置

### 后端 API 端口

Docker 镜像现在暴露了两个端口：

| 端口 | 服务 | 用途 |
|------|------|------|
| `8080` | Nginx | Web 界面 + API 代理 |
| `5000` | Flask | 后端 API（移动端直连） |

### Android 客户端连接

**方式一：通过 Nginx 代理（推荐）**
```
API 地址：http://<NAS IP>:8080/api/
```

**方式二：直接连接后端**
```
API 地址：http://<NAS IP>:5000/api/
```

### 启动容器（双端口映射）

```bash
docker run -d \
    --name funhub \
    --restart unless-stopped \
    -p 8080:8080 \
    -p 5000:5000 \
    -v /volume1/docker/funhub/storage:/app/storage \
    -v /volume1/media:/media:ro \
    -e FLASK_ENV=production \
    -e SECRET_KEY=$(openssl rand -hex 32) \
    -e DATABASE_PATH=/app/storage/database/funhub.db \
    -e THUMBNAIL_FOLDER=/app/storage/thumbnails \
    -e CORS_ORIGINS="http://localhost:5173,http://127.0.0.1:5173,http://localhost:8080,http://127.0.0.1:8080,http://<你的 NAS IP>:8080,http://<你的 NAS IP>:5000" \
    funhub:latest
```

### 验证 API 访问

```bash
# 通过 Nginx 访问
curl http://<NAS IP>:8080/api/health

# 直接访问后端
curl http://<NAS IP>:5000/api/health

# 获取视频列表
curl http://<NAS IP>:5000/api/videos
```
