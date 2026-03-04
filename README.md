# FunHub - 娱乐中心应用

<div align="center">

🎬 一个现代化的本地媒体管理中心，支持视频库和图片库管理

[![Python](https://img.shields.io/badge/Python-3.13-blue.svg)](https://python.org)
[![Flask](https://img.shields.io/badge/Flask-3.x-green.svg)](https://flask.palletsprojects.com)
[![React](https://img.shields.io/badge/React-18-blue.svg)](https://react.dev)
[![Ant Design](https://img.shields.io/badge/AntD-5-blue.svg)](https://ant.design)
[![Docker](https://img.shields.io/badge/Docker-Single_Container-blue.svg)](https://docker.com)

</div>

---

## 📖 项目简介

FunHub 是一个参考主流视频网站设计的娱乐中心应用，支持视频库和图片库的现代化管理。数据来源支持本地文件系统和 NAS，提供类似 Bilibili 的浏览体验和抖音风格的短视频模式。

### ✨ 核心特性

- 🎬 **视频库管理** - 浏览、搜索、排序、分页、收藏筛选
- 🖼️ **图片库管理** - 幻灯片播放、缩放、全屏浏览
- 📱 **抖音风格短视频** - 沉浸式全屏播放、上下滑动切换
- 🏷️ **标签系统** - 自定义标签、颜色、基于标签的推荐
- 📜 **播放历史** - 断点续播、进度记忆、观看统计
- 💗 **收藏功能** - 视频/图片统一收藏管理
- 🤖 **Android 客户端** - 直接连接后端 API（端口 5000）⭐新增
- 🖥️ **多源支持** - 本地目录、NAS 网络存储
- 🐳 **Docker 单容器** - 前后端一体化部署 ⭐新增
- 🎨 **B 站主题** - 粉色主题 (#fb7299)、毛玻璃效果、流畅动画

---

## 🚀 快速部署（Docker 推荐）

### 一键部署

```bash
# 克隆项目
git clone <your-repo-url>
cd funhub

# 一键部署（纯 Docker）
chmod +x deploy.sh
./deploy.sh
```

### 访问应用

浏览器打开：**http://localhost:8080**

### 查看状态

```bash
./status.sh
```

---

## 📦 Docker 部署

### 前置要求

- Docker 20.10+
- 至少 2GB 可用内存

### 快速启动

```bash
# 构建镜像
docker build -t funhub:latest .

# 启动容器（单端口模式）
docker run -d \
    --name funhub \
    --restart unless-stopped \
    -p 8080:8080 \
    -v "$(pwd)/storage:/app/storage" \
    -v /media:/media:ro \
    funhub:latest

# 查看日志
docker logs funhub -f

# 停止服务
docker stop funhub
```

### Android 客户端支持 ⭐新增

Docker 镜像暴露两个端口，支持移动端直连：

```bash
# 启动容器（双端口模式）
docker run -d \
    --name funhub \
    --restart unless-stopped \
    -p 8080:8080 \
    -p 5000:5000 \
    -v "$(pwd)/storage:/app/storage" \
    -v /media:/media:ro \
    funhub:latest
```

| 端口 | 服务 | 用途 |
|------|------|------|
| `8080` | Nginx | Web 界面 + API 代理 |
| `5000` | Flask | 后端 API（移动端直连） |

### 数据持久化

```bash
# 存储目录（数据库和缩略图）
-v "$(pwd)/storage:/app/storage"

# 媒体文件目录（可选）
-v /media:/media:ro
```

### Android 客户端支持 ⭐新增

Docker 镜像暴露两个端口，支持 Android 客户端直接访问：

| 端口 | 服务 | 用途 |
|------|------|------|
| `8080` | Nginx | Web 界面 + API 代理 |
| `5000` | Flask | 后端 API（移动端直连） |

启动容器（双端口映射）：
```bash
docker run -d \
    --name funhub \
    --restart unless-stopped \
    -p 8080:8080 \
    -p 5000:5000 \
    -v "$(pwd)/storage:/app/storage" \
    -v /media:/media:ro \
    funhub:latest
```

Android 客户端连接：
- 通过 Nginx: `http://<NAS IP>:8080/api/`
- 直接连接后端: `http://<NAS IP>:5000/api/`

详细 NAS 部署说明请参考 **[NAS-DEPLOY.md](./NAS-DEPLOY.md)**

---

## 💻 本地开发

### 前置要求

- Python 3.13+
- Node.js 18+
- ffmpeg (视频缩略图生成)

### 后端开发

```bash
cd backend
python -m venv venv
source venv/bin/activate
pip install -r requirements.txt
python migrate.py
python run.py
# 后端运行在 http://localhost:5000
```

### 前端开发

```bash
cd frontend
npm install
npm run dev
# 前端运行在 http://localhost:5173
```

---

## 📁 项目结构

```
funhub/
├── backend/                    # Flask 后端
│   ├── app/
│   │   ├── __init__.py        # Flask 应用初始化
│   │   ├── models.py          # 数据库模型
│   │   ├── routes/            # API 路由
│   │   ├── services/          # 业务服务
│   │   └── utils/             # 工具函数
│   ├── config.py
│   ├── requirements.txt
│   ├── migrate.py             # 数据库迁移
│   └── run.py
├── frontend/                   # React 前端
│   ├── src/
│   │   ├── components/        # UI 组件
│   │   ├── pages/             # 页面组件
│   │   ├── services/          # API 客户端
│   │   ├── App.jsx
│   │   └── main.jsx
│   ├── package.json
│   └── vite.config.js
├── storage/                    # 数据存储
│   ├── thumbnails/            # 缩略图缓存
│   └── database/              # SQLite 数据库
├── Dockerfile                  # 单容器 Docker 构建
├── nginx-single.conf           # Nginx 配置（单容器）
├── supervisord.conf            # 进程管理配置
├── deploy.sh                   # 一键部署脚本
├── status.sh                   # 状态检查脚本
└── DEPLOY.md                   # 详细部署文档
```

---

## 🔌 API 接口

### 视频 API

| 方法 | 端点 | 描述 |
|------|------|------|
| GET | `/api/videos` | 视频列表 (分页、搜索、排序) |
| GET | `/api/videos/<id>` | 视频详情 |
| PUT | `/api/videos/<id>` | 更新视频信息 |
| GET | `/api/videos/<id>/stream` | 视频流播放 |
| POST | `/api/videos/<id>/favorite` | 收藏/取消收藏 |
| GET | `/api/videos/<id>/related` | 相关视频推荐 |
| POST | `/api/videos/<id>/history` | 更新观看进度 |

### 图片 API

| 方法 | 端点 | 描述 |
|------|------|------|
| GET | `/api/images` | 图片列表 |
| GET | `/api/images/all` | 获取所有图片 (幻灯片) |
| GET | `/api/images/<id>/file` | 获取原图 |
| POST | `/api/images/<id>/favorite` | 收藏/取消收藏 |

### 其他 API

- **播放历史**: `/api/history/*`
- **来源配置**: `/api/sources/*`
- **标签管理**: `/api/tags/*`
- **收藏管理**: `/api/favorites/*`

完整 API 文档请参考 [CLAUDE.md](./CLAUDE.md)

---

## 🎯 功能详情

### 视频播放功能

| 功能 | 描述 |
|------|------|
| 自定义播放器 | 播放/暂停、进度条、音量、倍速、全屏 |
| 画中画模式 | 后台继续播放视频 |
| 进度记忆 | 自动保存观看进度，断点续播 |
| 观看历史 | 自动记录、统计、快速续播 |
| 播放统计 | 观看次数追踪 |

### 短视频模式 (抖音风格)

- 📱 沉浸式全屏播放
- 👆 上下滑动切换视频
- ⌨️ 键盘快捷键支持
- 🎲 顺序/随机播放模式
- 💗 快捷收藏 (快捷键 C)
- 🏷️ 快速标签管理 (快捷键 L)

### 图片幻灯片

- 🖼️ 全屏幻灯片播放
- 🔀 顺序/随机模式
- ⏱️ 可调节播放间隔 (1-30 秒)
- 🔍 图片缩放 (50%-300%)
- ⌨️ 键盘控制支持

### 标签系统

- 🏷️ 创建、编辑、删除标签
- 🎨 自定义标签颜色
- 🔗 基于标签的视频推荐
- 📊 标签筛选和搜索

---

## 🗄️ 数据库模型

### 核心模型

- **Video** - 视频信息 (标题、路径、时长、分辨率、收藏状态等)
- **Image** - 图片信息 (标题、路径、尺寸、收藏状态等)
- **Source** - 媒体来源配置 (本地/NAS)
- **Tag** - 标签定义 (名称、颜色)
- **WatchHistory** - 播放历史记录 (进度、完成状态)
- **ThumbnailCache** - 缩略图缓存

---

## 🎨 页面路由

| 路由 | 描述 |
|------|------|
| `/` | 首页仪表盘 |
| `/videos` | 视频库 |
| `/videos/:id` | 视频播放页 |
| `/short-video` | 抖音风格短视频 |
| `/images` | 图片库 |
| `/sources` | 来源配置 |
| `/favorites` | 我的收藏 |

---

## ⚡ 性能优化

### 后端优化

- 视频流块大小优化 (16KB)
- 缩略图缓存机制 (7 天有效期)
- 快速 ffmpeg seek
- 数据库索引优化
- 批量缩略图生成

### 前端优化

- 图片/视频预加载
- 组件懒加载
- 防抖/节流处理
- 键盘事件优化
- 响应式设计

---

## 🛠️ 常用命令

### Docker 部署

```bash
# 构建镜像
docker build -t funhub:latest .

# 启动容器
docker run -d --name funhub -p 8080:8080 -v "$(pwd)/storage:/app/storage" funhub:latest

# 查看状态
docker ps | grep funhub

# 查看日志
docker logs funhub -f

# 停止服务
docker stop funhub

# 重启服务
docker restart funhub

# 删除容器
docker rm -f funhub
```

### 本地开发

```bash
# 后端
./scripts/start-backend.sh

# 前端
./scripts/start-frontend.sh
```

---

## 📝 更新日志

### 2026-03-03

**🐳 Docker 部署优化**
- 单容器部署方案（前后端一体化）
- 简化部署流程，一键部署脚本
- 纯 Docker 部署（不使用 docker-compose）
- 更新部署文档

**🤖 Android 客户端支持**
- 后端暴露 5000 端口支持移动端直连
- CORS 配置优化
- NAS 部署文档更新

### 2026-03-01

**✨ 新增功能**
- 抖音风格短视频模式 (`/short-video`)
- 图片幻灯片播放功能
- 播放历史记录系统
- 播放进度记忆功能
- 画中画播放支持
- 批量缩略图生成 API
- 观看次数统计

**🎨 UI 优化**
- Bilibili 粉色主题 (#fb7299)
- 渐变进度条
- 毛玻璃效果控件
- 移动端触摸手势支持

**🐛 修复**
- 视频流播放性能问题
- 缩略图生成速度优化

---

## 📋 待开发功能

- [ ] 弹幕功能
- [ ] 用户系统 (多用户支持)
- [ ] NAS 扫描完整实现
- [ ] 视频转码支持
- [ ] 移动端 App 封装
- [ ] 视频上传功能
- [ ] 播放列表管理
- [ ] 字幕支持

---

## 📄 许可证

MIT License

---

<div align="center">

**FunHub** - 让本地媒体管理更有趣 🎉

**[部署文档](./DEPLOY.md)** | **[API 文档](./CLAUDE.md)**

</div>
