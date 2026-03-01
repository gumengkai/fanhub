# FunHub 娱乐中心应用

## 项目概述

创建一个现代化的娱乐中心应用，参考主流视频网站设计，支持视频库和图片库管理，数据来源支持本地文件系统和 NAS。

**2026-03-01 最新更新**: 新增抖音风格短视频模式、图片幻灯片播放、播放历史记录、Bilibili 风格 UI 优化

## 技术栈

- **后端**: Python 3.13 + Flask + SQLAlchemy + SQLite
- **前端**: React 18 + Ant Design 5 + React Router + Axios
- **媒体处理**: ffmpeg (视频缩略图), Pillow (图片处理)

## 项目结构

```
funhub/
├── backend/                    # Flask 后端
│   ├── app/
│   │   ├── __init__.py        # Flask 应用初始化
│   │   ├── models.py          # 数据库模型 (Video, Image, Source, Tag, WatchHistory, ThumbnailCache)
│   │   ├── routes/
│   │   │   ├── __init__.py
│   │   │   ├── videos.py      # 视频 API (含标签、推荐、历史记录)
│   │   │   ├── images.py      # 图片 API (含全部列表)
│   │   │   ├── sources.py     # 来源配置 API
│   │   │   ├── favorites.py   # 收藏 API
│   │   │   ├── tags.py        # 标签管理 API
│   │   │   └── history.py     # 播放历史 API ⭐新增
│   │   ├── services/
│   │   │   ├── __init__.py
│   │   │   ├── scanner.py     # 文件扫描服务
│   │   │   ├── thumbnail.py   # 缩略图生成 (优化版) ⭐
│   │   │   └── nas_client.py  # NAS 连接
│   │   └── utils/
│   │       ├── __init__.py
│   │       └── helpers.py
│   ├── config.py
│   ├── requirements.txt
│   ├── migrate.py             # 数据库迁移脚本 ⭐新增
│   └── run.py
├── frontend/                   # React 前端
│   ├── public/
│   ├── src/
│   │   ├── components/
│   │   │   ├── Layout/        # 应用布局组件 (Bilibili 风格优化) ⭐
│   │   │   ├── MediaCard/     # 媒体卡片组件
│   │   │   ├── MediaGrid/     # 媒体网格组件
│   │   │   ├── VideoPlayer/   # 视频播放器组件 (画中画、进度记忆) ⭐
│   │   │   ├── Slideshow/     # 图片幻灯片组件 ⭐新增
│   │   │   └── ErrorBoundary/ # 错误边界组件
│   │   ├── pages/
│   │   │   ├── Home/          # 首页仪表盘
│   │   │   ├── VideoLibrary/  # 视频库页面
│   │   │   ├── VideoPlay/     # 视频播放详情页 (B 站风格优化) ⭐
│   │   │   ├── ShortVideo/    # 抖音风格短视频 ⭐新增
│   │   │   ├── ImageLibrary/  # 图片库页面 (集成幻灯片) ⭐
│   │   │   ├── SourceConfig/  # 来源配置页面
│   │   │   └── Favorites/     # 收藏页面
│   │   ├── services/
│   │   │   └── api.js         # API 客户端
│   │   ├── App.jsx
│   │   └── main.jsx           # Bilibili 主题配置 ⭐
│   ├── package.json
│   └── vite.config.js
├── storage/                    # 数据存储
│   ├── thumbnails/            # 缩略图缓存
│   └── database/              # SQLite 数据库
├── docker-compose.yml
└── *.sh                       # 启动脚本
```

## 已实现功能

### 核心功能
- ✅ 视频库管理 (列表、搜索、排序、分页、收藏筛选)
- ✅ 图片库管理 (列表、搜索、分页、收藏筛选)
- ✅ 收藏功能 (视频/图片统一收藏管理)
- ✅ 来源配置 (本地目录、NAS 支持)
- ✅ 自动扫描媒体文件
- ✅ 缩略图生成 (ffmpeg/Pillow，优化缓存)

### 视频播放功能
- ✅ 独立视频播放页面 (`/videos/:id`)
- ✅ 自定义视频播放器：
  - 播放/暂停控制
  - 进度条拖动 (支持实时预览)
  - 音量调节和静音
  - 快进/快退 10 秒
  - 倍速播放 (0.5x - 2x)
  - 全屏模式
  - 自动隐藏控制栏
  - **画中画模式** ⭐新增
  - **播放进度记忆** ⭐新增
  - **自动保存观看历史** ⭐新增
- ✅ 视频信息编辑 (标题、描述)
- ✅ 视频收藏功能
- ✅ 播放次数统计 ⭐新增

### 短视频模式 (抖音风格) ⭐新增
- ✅ 沉浸式全屏播放 (`/short-video`)
- ✅ 上下滑动切换视频
- ✅ 播放模式：顺序/随机
- ✅ 筛选功能：全部/收藏/按标签
- ✅ 快捷操作：
  - 收藏/取消收藏 (快捷键 C)
  - 编辑标签 (快捷键 L)
  - 删除视频
- ✅ 键盘控制：
  - ↑/↓ - 上一个/下一个
  - ←/→ - 快退/快进 10 秒
  - 空格 - 播放/暂停
  - F - 全屏
  - M - 静音
  - R - 切换随机模式
- ✅ 触摸手势支持 (移动端)
- ✅ 进度条拖动

### 图片幻灯片播放 ⭐新增
- ✅ 全屏幻灯片模式
- ✅ 播放模式：顺序/随机
- ✅ 可调节播放间隔 (1-30 秒)
- ✅ 图片缩放 (50%-300%)
- ✅ 全屏模式
- ✅ 键盘控制：
  - ←/→ - 上一张/下一张
  - 空格 - 播放/暂停
  - F - 全屏
  - R - 切换随机模式
  - +/- - 缩放
- ✅ 自动预加载

### 标签系统
- ✅ 标签创建、编辑、删除
- ✅ 视频标签管理 (添加/移除标签)
- ✅ 基于标签的视频推荐
- ✅ 相关视频展示 (同标签推荐)
- ✅ 标签颜色自定义 (B 站粉色主题)

### 播放历史记录 ⭐新增
- ✅ 自动记录观看进度
- ✅ 断点续播功能
- ✅ 观看完成状态标记
- ✅ 历史播放统计
- ✅ 进度自动保存 (每 10 秒)

### UI/UX 优化 ⭐新增
- ✅ Bilibili 粉色主题 (#fb7299)
- ✅ 渐变进度条
- ✅ 流畅动画效果
- ✅ 响应式设计 (移动端优化)
- ✅ 毛玻璃效果控件
- ✅ 现代化卡片设计

## 数据库模型

### Video 模型
- id: Integer (PK)
- title: String
- path: String (文件路径)
- source_id: Integer (FK)
- file_size: Integer
- duration: Integer (秒)
- width/height: Integer (分辨率)
- thumbnail_path: String
- is_favorite: Boolean
- description: Text (视频描述)
- **view_count: Integer (播放次数)** ⭐新增
- created_at: DateTime
- updated_at: DateTime
- tags: Relationship (Many-to-Many)
- **watch_history: Relationship (One-to-Many)** ⭐新增

### Image 模型
- id: Integer (PK)
- title: String
- path: String
- source_id: Integer (FK)
- file_size: Integer
- width/height: Integer
- thumbnail_path: String
- is_favorite: Boolean
- **view_count: Integer (查看次数)** ⭐新增
- created_at: DateTime

### Source 模型
- id: Integer (PK)
- name: String (显示名称)
- type: Enum ('local', 'nas')
- path: String (本地路径或 NAS 路径)
- nas_config: JSON (NAS 连接配置)
- scan_interval: Integer (自动扫描间隔，分钟)
- is_active: Boolean
- last_scan_at: DateTime
- created_at: DateTime

### Tag 模型
- id: Integer (PK)
- name: String (标签名)
- **color: String (颜色代码，默认 #fb7299)** ⭐B 站粉色
- created_at: DateTime

### WatchHistory 模型 ⭐新增
- id: Integer (PK)
- video_id: Integer (FK)
- playback_position: Integer (播放进度，秒)
- is_completed: Boolean (是否看完)
- watched_at: DateTime

### ThumbnailCache 模型 ⭐新增
- id: Integer (PK)
- media_type: String ('video' or 'image')
- media_id: Integer
- file_path: String
- file_hash: String (缓存失效校验)
- file_size: Integer
- created_at: DateTime
- expires_at: DateTime

### 关联表 video_tags
- video_id: Integer (FK)
- tag_id: Integer (FK)

## API 接口

### 视频 API
- `GET /api/videos` - 视频列表 (分页、搜索、排序、按标签筛选、收藏筛选)
- `GET /api/videos/<id>` - 视频详情 (含标签、来源信息)
- `PUT /api/videos/<id>` - 更新视频信息 (标题、描述、收藏状态)
- `GET /api/videos/<id>/stream` - 视频流播放 (支持 Range 请求，优化块大小)
- `DELETE /api/videos/<id>` - 删除视频记录
- `POST /api/videos/<id>/favorite` - 收藏/取消收藏
- `POST /api/videos/<id>/thumbnail` - 重新生成缩略图
- `GET /api/videos/<id>/tags` - 获取视频标签
- `POST /api/videos/<id>/tags` - 添加标签到视频
- `DELETE /api/videos/<id>/tags/<tag_id>` - 从视频移除标签
- `GET /api/videos/<id>/related` - 获取相关视频 (基于标签)
- `GET /api/videos/<id>/history` - 获取观看历史
- `POST /api/videos/<id>/history` - 更新观看进度
- `POST /api/videos/thumbnails/batch` - 批量生成缩略图 ⭐新增

### 图片 API
- `GET /api/images` - 图片列表 (分页、搜索)
- `GET /api/images/all` - 获取所有图片 (用于幻灯片) ⭐新增
- `GET /api/images/<id>` - 图片详情
- `GET /api/images/<id>/file` - 获取原图
- `DELETE /api/images/<id>` - 删除图片记录
- `POST /api/images/<id>/favorite` - 收藏/取消收藏

### 播放历史 API ⭐新增
- `GET /api/history` - 获取观看历史列表
- `GET /api/history/video/<video_id>` - 获取指定视频观看历史
- `POST /api/history/video/<video_id>` - 更新/创建观看历史
- `DELETE /api/history/video/<video_id>` - 删除观看历史
- `POST /api/history/clear` - 清空所有历史
- `GET /api/history/stats` - 获取历史统计

### 来源配置 API
- `GET /api/sources` - 来源列表
- `POST /api/sources` - 创建来源
- `PUT /api/sources/<id>` - 更新来源
- `DELETE /api/sources/<id>` - 删除来源
- `POST /api/sources/<id>/scan` - 手动触发扫描
- `GET /api/sources/<id>/status` - 检查连接状态
- `GET /api/sources/<id>/stats` - 获取来源统计

### 标签 API
- `GET /api/tags` - 获取所有标签
- `POST /api/tags` - 创建标签
- `PUT /api/tags/<id>` - 更新标签
- `DELETE /api/tags/<id>` - 删除标签

### 收藏 API
- `GET /api/favorites` - 获取所有收藏 (视频 + 图片)
- `GET /api/favorites/stats` - 获取收藏统计

## 启动命令

### 后端
```bash
cd backend
python -m venv venv
source venv/bin/activate
pip install -r requirements.txt
python migrate.py  # 运行数据库迁移 ⭐新增
python run.py
```

### 前端
```bash
cd frontend
npm install
npm run dev
```

### 一键启动脚本
```bash
./start-all.sh      # 启动前后端
./stop-all.sh       # 停止所有服务
./status.sh         # 查看服务状态
```

## 访问地址

- 前端：http://localhost:5173
- 后端：http://localhost:5000
- API Health: http://localhost:5000/api/health

## 页面路由

- `/` - 首页仪表盘
- `/videos` - 视频库
- `/videos/:id` - 视频播放页
- `/short-video` - 抖音风格短视频 ⭐新增
- `/images` - 图片库 (支持幻灯片播放) ⭐
- `/sources` - 来源配置
- `/favorites` - 我的收藏

## 性能优化

### 后端优化 ⭐新增
- 视频流块大小优化 (8KB → 16KB)
- 缩略图缓存机制 (7 天有效期)
- 批量缩略图生成支持
- 快速 ffmpeg seek (先 seek 后解码)
- 数据库索引优化

### 前端优化 ⭐新增
- 图片预加载
- 视频预加载 (短视频模式)
- 组件懒加载
- 防抖/节流处理
- 键盘事件优化

## 待优化项

- [ ] 弹幕功能
- [ ] 用户系统 (多用户支持)
- [ ] NAS 扫描完整实现
- [ ] 视频转码支持
- [ ] 移动端 App 封装
- [ ] 视频上传功能
- [ ] 播放列表管理
- [ ] 字幕支持

## 更新日志

### 2026-03-01
- ✨ 新增抖音风格短视频模式 (`/short-video`)
- ✨ 新增图片幻灯片播放功能
- ✨ 新增播放历史记录系统
- ✨ 新增播放进度记忆功能
- ✨ 新增画中画播放支持
- ✨ 新增批量缩略图生成 API
- ✨ Bilibili 风格 UI 全面优化
- ✨ 新增观看次数统计
- ✨ 键盘快捷键全面支持
- ✨ 移动端触摸手势支持
- 🐛 修复视频流播放性能问题
- 🐛 优化缩略图生成速度
