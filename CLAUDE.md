# FanHub - 本地媒体娱乐中心

## 项目概述

FanHub 是一个现代化的本地媒体管理中心，参考主流视频网站设计，支持视频库和图片库管理。数据来源支持本地文件系统和 NAS，提供类似 Bilibili 的浏览体验和抖音风格的短视频模式。

## 核心特性

- 🎬 **视频库管理** - 浏览、搜索、排序、分页、收藏筛选
- 🖼️ **图片库管理** - 幻灯片播放、缩放、全屏浏览
- 📱 **抖音风格短视频** - 沉浸式全屏播放、上下滑动切换
- 🏷️ **标签系统** - 自定义标签、颜色、基于标签的推荐
- 📜 **播放历史** - 断点续播、进度记忆、观看统计
- 💗 **喜欢功能** - 快速表达喜爱（红心）
- ⭐ **收藏功能** - 保存到收藏夹（星星）
- 🤖 **Android 客户端** - 直接连接后端 API
- 🖥️ **多源支持** - 本地目录、NAS 网络存储
- 🐳 **Docker 单容器** - 前后端一体化部署
- 🎨 **B 站主题** - 粉色主题 (#fb7299)、毛玻璃效果

## 技术栈

- **后端**: Python 3.13 + Flask + SQLAlchemy + SQLite
- **前端**: React 18 + Ant Design 5 + React Router + Axios
- **媒体处理**: ffmpeg (视频缩略图), Pillow (图片处理)
- **Android**: Kotlin + Jetpack Compose + Media3 ExoPlayer + Hilt

## 项目结构

```
fanhub/
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
├── android/                    # Android 客户端
│   └── app/src/main/java/com/fanhub/app/
│       ├── data/              # 数据层
│       ├── player/            # 播放器管理
│       ├── ui/                # UI 层
│       └── MainActivity.kt
├── storage/                    # 数据存储
│   ├── thumbnails/            # 缩略图缓存
│   └── database/              # SQLite 数据库
├── scripts/                    # 启动脚本
├── Dockerfile                  # Docker 构建
├── nginx-single.conf           # Nginx 配置
├── supervisord.conf            # 进程管理
├── deploy.sh                   # 部署脚本
└── README.md                   # 项目说明
```

## 快速开始

### Docker 部署（推荐）

```bash
# 克隆项目
git clone https://github.com/gumengkai/fanhub.git
cd fanhub

# 一键部署
chmod +x deploy.sh
./deploy.sh

# 访问 http://localhost:8080
```

### 本地开发

```bash
# 启动前后端
./scripts/start-dev.sh

# 或分别启动
./scripts/start-backend.sh   # 后端 http://localhost:5000
./scripts/start-frontend.sh  # 前端 http://localhost:5173
```

## API 接口

### 视频 API

| 方法 | 端点 | 描述 |
|------|------|------|
| GET | `/api/videos` | 视频列表 (分页、搜索、排序、筛选) |
| GET | `/api/videos/<id>` | 视频详情 |
| PUT | `/api/videos/<id>` | 更新视频信息 |
| GET | `/api/videos/<id>/stream` | 视频流播放 |
| POST | `/api/videos/<id>/favorite` | 收藏/取消收藏 |
| POST | `/api/videos/<id>/like` | 喜欢/取消喜欢 |
| GET | `/api/videos/<id>/related` | 相关视频推荐 |
| POST | `/api/videos/<id>/history` | 更新观看进度 |

### 图片 API

| 方法 | 端点 | 描述 |
|------|------|------|
| GET | `/api/images` | 图片列表 |
| GET | `/api/images/all` | 获取所有图片 (幻灯片) |
| GET | `/api/images/<id>/file` | 获取原图 |
| POST | `/api/images/<id>/favorite` | 收藏/取消收藏 |
| POST | `/api/images/<id>/like` | 喜欢/取消喜欢 |

### 其他 API

- **播放历史**: `/api/history/*`
- **来源配置**: `/api/sources/*`
- **标签管理**: `/api/tags/*`
- **收藏管理**: `/api/favorites/*`
- **喜欢管理**: `/api/likes/*`

## 页面路由

| 路由 | 描述 |
|------|------|
| `/` | 首页仪表盘 |
| `/videos` | 视频库 |
| `/videos/:id` | 视频播放页 |
| `/short-video` | 抖音风格短视频 |
| `/images` | 图片库 |
| `/sources` | 来源配置 |
| `/favorites` | 个人中心（喜欢/收藏）|

## 数据库模型

### Video 模型
- id, title, path, source_id
- file_size, duration, width, height
- thumbnail_path, is_favorite, is_liked
- description, view_count
- tags (Many-to-Many)
- watch_history (One-to-Many)

### Image 模型
- id, title, path, source_id
- file_size, width, height
- thumbnail_path, is_favorite, is_liked
- view_count

### Source 模型
- id, name, type ('local', 'nas')
- path, nas_config, scan_interval
- is_active, last_scan_at

### Tag 模型
- id, name, color

### WatchHistory 模型
- id, video_id, playback_position
- is_completed, watched_at

## Android 客户端

### 技术栈
- **语言**: Kotlin
- **UI**: Jetpack Compose + Material3
- **架构**: MVVM + Hilt DI
- **网络**: Retrofit + OkHttp
- **播放器**: Media3 ExoPlayer
- **图片加载**: Coil

### 功能
- 首页快捷入口、热门视频、继续观看
- 抖音风格沉浸式短视频播放
- 视频库列表、标签筛选
- 个人中心（喜欢/收藏管理）
- 动态后端地址配置

### 构建
```bash
cd android
./gradlew assembleDebug
# APK 输出到 app/build/outputs/apk/debug/
```

## 更新日志

### 2026-04-11
- Android 客户端重构：短视频筛选/随机播放、移除图片库
- 新增喜欢功能（红心），收藏改用星星图标
- 个人中心页面（喜欢/收藏 Tab 切换）
- APK 固定输出目录 `/mnt/fan/apk/`

### 2026-03-03
- Docker 单容器部署方案
- Android 客户端支持
- 一键部署脚本

### 2026-03-01
- 抖音风格短视频模式
- 图片幻灯片播放
- 播放历史记录
- Bilibili 风格 UI

## 许可证

MIT License
