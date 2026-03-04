# Docker 镜像打包指南

## 前提条件

确保 Docker Desktop 已启动并运行。

## 构建并打包镜像

### 方式一：直接构建并打包（推荐）

```bash
cd /home/gmk/funhub

# 1. 构建镜像
docker build -t funhub:latest .

# 2. 打包为 tar 文件
docker save -o funhub-latest.tar funhub:latest

# 3. 验证打包文件
ls -lh funhub-latest.tar
tar -tvf funhub-latest.tar | head -10
```

### 方式二：通过 Windows Docker Desktop

如果在 WSL 中无法访问 Docker，可以在 Windows PowerShell 中执行：

```powershell
cd C:\path\to\funhub

# 构建镜像
docker build -t funhub:latest .

# 打包镜像
docker save -o funhub-latest.tar funhub:latest
```

## 镜像信息

- **镜像名称**: funhub:latest
- **打包文件**: funhub-latest.tar
- **大小**: 约 130-150MB
- **包含内容**:
  - Python 3.13 运行时
  - Flask 后端应用
  - React 前端应用（已构建）
  - Nginx 反向代理
  - Supervisor 进程管理
  - 所有依赖项

## 部署镜像

### 从 tar 文件加载镜像

```bash
# 加载镜像
docker load -i funhub-latest.tar

# 验证加载
docker images | grep funhub

# 运行容器
docker run -d \
    --name funhub \
    --restart unless-stopped \
    -p 8080:8080 \
    -p 5000:5000 \
    -v "$(pwd)/storage:/app/storage" \
    -v /media:/media:ro \
    funhub:latest
```

## 当前版本

- **代码版本**: 16c0d957 (2026-03-04)
- **镜像版本**: funhub:latest (3bdffd4a866a)
- **镜像大小**: 546MB (压缩后 133MB)
- **打包文件**: funhub-latest.tar

## 打包信息

```
镜像 ID: 3bdffd4a866a
打包时间：2026-03-04 23:07
打包文件：funhub-latest.tar (133MB)
包含层数：29 层
```



- **代码版本**: 5f95f890 (2026-03-04)
- **主要更新**:
  - 短视频模式优化（播放顺序/预加载）
  - Docker 单容器部署方案
  - Android 客户端支持
  - Bilibili 风格 UI

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
