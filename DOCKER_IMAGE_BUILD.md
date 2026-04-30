# Docker 镜像打包指南

## 前提条件

确保 Docker Desktop 已启动并运行。

## 构建并打包镜像

### 方式一：直接构建并打包（推荐）

```bash
cd /home/gmk/fanhub

# 1. 确保使用最新代码
git pull origin master

# 2. 构建镜像（使用缓存）
docker build -t fanhub:latest .

# 或完全重新构建（不使用缓存）
docker build --no-cache -t fanhub:latest .

# 3. 打包为 tar 文件
docker save -o fanhub-latest.tar fanhub:latest

# 4. 验证打包文件
ls -lh fanhub-latest.tar
tar -tvf fanhub-latest.tar | head -10
```

### 方式二：通过 Windows Docker Desktop

如果在 WSL 中无法访问 Docker，可以在 Windows PowerShell 中执行：

```powershell
cd C:\path\to\fanhub

# 构建镜像
docker build -t fanhub:latest .

# 打包镜像
docker save -o fanhub-latest.tar fanhub:latest
```

## 镜像信息

- **镜像名称**: fanhub:latest
- **打包文件**: fanhub-latest.tar
- **大小**: 约 546MB (原始) / 133MB (压缩 tar)
- **包含内容**:
  - Python 3.12 运行时
  - Flask 后端应用
  - React 前端应用（已构建）
  - Nginx 反向代理
  - Supervisor 进程管理
  - 所有依赖项

## 部署镜像

### 从 tar 文件加载镜像

```bash
# 加载镜像
docker load -i fanhub-latest.tar

# 验证加载
docker images | grep fanhub

# 运行容器
docker run -d \
    --name fanhub \
    --restart unless-stopped \
    -p 8080:8080 \
    -p 5000:5000 \
    -v "$(pwd)/storage:/app/storage" \
    -v /media:/media:ro \
    fanhub:latest
```

## 当前版本

- **代码版本**: 抖音库功能 (2026-04-29)
- **镜像版本**: fanhub:latest (a880d5106a37)
- **镜像大小**: 569MB (压缩后 138MB)
- **打包文件**: fanhub-latest.tar
- **打包时间**: 2026-04-29 21:48
- **主要更新**:
  - 新增抖音库功能（独立于视频库）
  - Source.media_type 扩展支持 'douyin'
  - 抖音库专用 API `/api/douyin/*`
  - Web 抖音库页面（沉浸式播放、双击喜欢）
  - FanTok Android 极简抖音应用
  - 个人中心重构（两级 Tab 结构）

## 打包信息

```
镜像 ID: sha256:a880d5106a37e2a88bf36e6416fcd4779b7d665e5184ba76b43e0e4d810505ee
打包时间：2026-04-29 21:48
打包文件：fanhub-latest.tar (138MB)
输出位置：/mnt/fan/apk/
构建方式：docker commit 方式更新镜像
```

## 验证镜像

```bash
# 运行测试容器
docker run -d --name fanhub-test --rm -p 8080:8080 fanhub:latest

# 等待启动后测试健康检查
sleep 5
curl http://localhost:8080/api/health

# 停止测试容器
docker stop fanhub-test
```

## 故障排除

### Docker Desktop 未启动

```bash
# Windows 上启动 Docker Desktop
# 点击 Docker Desktop 图标或运行:
"C:\Program Files\Docker\Docker\Docker Desktop.exe"
```

### WSL 中无法访问 Docker

确保 Docker Desktop 设置中启用了 WSL 2 集成：
1. 打开 Docker Desktop
2. 设置 → Resources → WSL Integration
3. 启用 Ubuntu 集成

### 镜像构建失败

```bash
# 清理 Docker 缓存
docker system prune -a

# 重新构建
docker build --no-cache --pull -t fanhub:latest .
```

### 部署后代码不一致

如果部署后的镜像与最新代码不一致，请检查：

1. **确认使用最新的 tar 文件**
   ```bash
   ls -lh fanhub-latest.tar
   # 检查文件时间是否是最新的
   ```

2. **重新构建镜像**
   ```bash
   docker build --no-cache -t fanhub:latest .
   docker save -o fanhub-latest.tar fanhub:latest
   ```

3. **验证镜像内容**
   ```bash
   # 检查镜像中的代码
   docker run --rm fanhub:latest cat /app/backend/app/routes/videos.py | head -20
   ```

4. **清理旧镜像**
   ```bash
   docker rmi fanhub:latest
   docker load -i fanhub-latest.tar
   ```

## 2026-04-29 更新

- 镜像版本: fanhub:latest (fb3513ca3cab)
- 重要: 需要对现有数据库运行迁移以添加 is_liked 列
- 迁移命令: `python3 backend/migrate.py` (需要设置 DATABASE_PATH 环境变量)
