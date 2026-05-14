# FanHub - 本地媒体娱乐中心

## 项目概述

FanHub 是一个现代化的本地媒体管理中心，参考主流视频网站设计，支持视频库和图片库管理。数据来源支持本地文件系统和 NAS，提供类似 Bilibili 的浏览体验和抖音风格的短视频模式。

## 核心特性

- 🎬 **视频库管理** - 浏览、搜索、排序、分页、收藏筛选
- 🖼️ **图片库管理** - 幻灯片播放、缩放、全屏浏览
- 📱 **抖音库** - 抖音风格短视频沉浸式播放（独立于视频库）
- 🏷️ **标签系统** - 自定义标签、颜色、基于标签的推荐
- 📜 **播放历史** - 断点续播、进度记忆、观看统计
- 💗 **喜欢功能** - 快速表达喜爱（红心）
- ⭐ **收藏功能** - 保存到收藏夹（星星）
- 🤖 **Android 客户端** - FanHub 和 FanTok 双应用
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

### 抖音库 API

| 方法 | 端点 | 描述 |
|------|------|------|
| GET | `/api/douyin` | 抖音视频列表 (分页、筛选喜欢/收藏/标签) |
| GET | `/api/douyin/<id>` | 视频详情 |
| GET | `/api/douyin/<id>/stream` | 视频流播放 |
| GET | `/api/douyin/<id>/thumbnail` | 视频缩略图 |
| POST | `/api/douyin/<id>/like` | 喜欢/取消喜欢 |
| POST | `/api/douyin/<id>/favorite` | 收藏/取消收藏 |
| DELETE | `/api/douyin/<id>` | 删除视频（文件+数据库记录）|
| GET | `/api/douyin/stats` | 统计信息（总数、喜欢数、收藏数）|

**注意**: 抖音库与视频库完全隔离，通过 Source 的 media_type='douyin' 区分。

## 页面路由

| 路由 | 描述 |
|------|------|
| `/` | 首页仪表盘 |
| `/videos` | 视频库 |
| `/videos/:id` | 视频播放页 |
| `/douyin` | 抖音库（沉浸式短视频）|
| `/images` | 图片库 |
| `/sources` | 来源配置 |
| `/favorites` | 个人中心（喜欢/收藏 Tab → 视频库/图片库/抖音库）|

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
- media_type ('all', 'video', 'image', 'douyin')
- is_active, last_scan_at

### Tag 模型
- id, name, color

### WatchHistory 模型
- id, video_id, playback_position
- is_completed, watched_at

## Android 客户端

FanHub 项目包含三个 Android 应用，共享相同的技术栈但针对不同使用场景：

### 应用对比

| 应用 | 包名 | 定位 | 主要功能 |
|------|------|------|----------|
| **FanHub** | `com.fanhub.app` | 完整版媒体中心 | 视频库、图片库、短视频、个人中心 |
| **FanPeak** | `com.fanpeak.app` | 精简短视频版 | 抖音风格短视频、个人中心 |
| **FanTok** | `com.fantok.app` | 极简抖音版 | 纯短视频播放、直连后端 |

### 技术栈
- **语言**: Kotlin 2.0
- **UI**: Jetpack Compose + Material3
- **架构**: MVVM + Hilt DI
- **网络**: Retrofit + OkHttp
- **播放器**: Media3 ExoPlayer
- **图片加载**: Coil

### 项目结构
```
android/
├── app/                          # FanHub 完整版
│   └── src/main/java/com/fanhub/app/
│       ├── data/
│       │   ├── api/             # API 接口 (Retrofit)
│       │   ├── model/           # 数据模型
│       │   ├── repository/      # 数据仓库
│       │   └── local/           # 本地存储 (DataStore)
│       ├── player/              # ExoPlayer 管理
│       ├── ui/
│       │   ├── screens/         # 页面
│       │   │   ├── feed/        # 短视频 (VideoFeedItem.kt)
│       │   │   ├── library/     # 视频库
│       │   │   ├── home/        # 首页
│       │   │   └── profile/     # 个人中心
│       │   ├── components/      # 公共组件
│       │   ├── theme/           # 主题
│       │   └── navigation/      # 导航
│       └── MainActivity.kt
├── fanpeak/                      # FanPeak 精简版
│   └── src/main/java/com/fanpeak/app/
│       └── ... (类似结构)
└── fantok/                       # FanTok 极简版
    └── src/main/java/com/fantok/app/
        └── ... (类似结构)
```

### 构建命令
```bash
cd /home/gmk/fanhub/android

# FanHub 完整版
./gradlew :app:assembleRelease
# APK: app/build/outputs/apk/release/fanhub-release.apk

# FanPeak 精简版
./gradlew :fanpeak:assembleRelease
# APK: fanpeak/build/outputs/apk/release/fanpeak-release.apk

# FanTok 极简版
./gradlew :fantok:assembleRelease
# APK: fantok/build/outputs/apk/release/fantok-release.apk

# 复制到输出目录
cp */build/outputs/apk/release/*-release.apk /mnt/fan/apk/
```

### 短视频页面 (VideoFeedItem)
**文件位置**: `ui/screens/feed/VideoFeedItem.kt`

**功能**:
- 沉浸式全屏视频播放
- 上下滑动切换视频 (VerticalPager)
- 双击喜欢 (红心动画)
- 侧边操作按钮 (喜欢/收藏/删除/全屏)
- 底部进度条 + 倍速控制
- 点击切换控制栏显示/隐藏

**注意事项**:
- 手势检测应放在视频区域，不要放在根 Box 上，否则会拦截按钮点击事件
- 删除操作需更新播放器状态，避免播放已删除视频

## 更新日志

### 2026-05-14
- **Bug 修复**: Android 短视频页面删除按钮无响应
  - **问题**: VideoFeedItem 根 Box 上的 `pointerInput` 手势检测拦截了所有子组件的点击事件，导致删除、喜欢、收藏等按钮无法响应
  - **修复**: 将手势检测从根 Box 移至视频/缩略图区域，按钮可正常接收点击事件
  - **影响模块**: FanHub (`android/app`)、FanPeak (`android/fanpeak`)、FanTok (`android/fantok`)
  - **相关文件**:
    - `VideoFeedItem.kt` - 手势检测位置调整
    - `FeedViewModel.kt` - 删除操作增加响应处理和播放器状态更新

### 2026-04-29
- 新增抖音库功能（独立于视频库）
  - Source.media_type 扩展支持 'douyin'
  - 抖音库专用 API `/api/douyin/*`
  - Web 抖音库页面（沉浸式播放、双击喜欢、滚轮切换）
  - FanTok Android 极简抖音应用
- 抖音库交互特性
  - 双击屏幕触发喜欢（红心动画）
  - 鼠标滚轮/触摸滑动切换视频
  - 控制栏播放时自动隐藏（3秒）
  - 侧边按钮始终可见（喜欢/收藏/删除）
  - 隐藏式可拖拽进度条
- 个人中心重构
  - 两级 Tab 结构：喜欢/收藏 → 视频库/图片库/抖音库

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
