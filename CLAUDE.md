# FunHub 娱乐中心应用

## 项目概述

创建一个现代化的娱乐中心应用，参考主流视频网站设计，支持视频库和图片库管理，数据来源支持本地文件系统和 NAS。

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
│   │   ├── models.py          # 数据库模型 (Video, Image, Source, Tag)
│   │   ├── routes/
│   │   │   ├── __init__.py
│   │   │   ├── videos.py      # 视频 API (含标签、推荐)
│   │   │   ├── images.py      # 图片 API
│   │   │   ├── sources.py     # 来源配置 API
│   │   │   ├── favorites.py   # 收藏 API
│   │   │   └── tags.py        # 标签管理 API
│   │   ├── services/
│   │   │   ├── __init__.py
│   │   │   ├── scanner.py     # 文件扫描服务
│   │   │   ├── thumbnail.py   # 缩略图生成
│   │   │   └── nas_client.py  # NAS 连接
│   │   └── utils/
│   │       ├── __init__.py
│   │       └── helpers.py
│   ├── config.py
│   ├── requirements.txt
│   └── run.py
├── frontend/                   # React 前端
│   ├── public/
│   ├── src/
│   │   ├── components/
│   │   │   ├── Layout/        # 应用布局组件
│   │   │   ├── MediaCard/     # 媒体卡片组件
│   │   │   ├── MediaGrid/     # 媒体网格组件
│   │   │   ├── VideoPlayer/   # 视频播放器组件 (自定义控制)
│   │   │   └── ErrorBoundary/ # 错误边界组件
│   │   ├── pages/
│   │   │   ├── Home/          # 首页仪表盘
│   │   │   ├── VideoLibrary/  # 视频库页面
│   │   │   ├── VideoPlay/     # 视频播放详情页 (含标签、推荐)
│   │   │   ├── ImageLibrary/  # 图片库页面
│   │   │   ├── SourceConfig/  # 来源配置页面
│   │   │   └── Favorites/     # 收藏页面
│   │   ├── services/
│   │   │   └── api.js         # API 客户端
│   │   ├── App.jsx
│   │   └── main.jsx
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
- ✅ 视频库管理 (列表、搜索、排序、分页)
- ✅ 图片库管理 (列表、搜索、分页)
- ✅ 收藏功能 (视频/图片统一收藏管理)
- ✅ 来源配置 (本地目录、NAS 支持)
- ✅ 自动扫描媒体文件
- ✅ 缩略图生成 (ffmpeg/Pillow)

### 视频播放功能
- ✅ 独立视频播放页面 (`/videos/:id`)
- ✅ 自定义视频播放器：
  - 播放/暂停控制
  - 进度条拖动 (支持实时预览)
  - 音量调节和静音
  - 快进/快退 10秒
  - 倍速播放 (0.5x - 2x)
  - 全屏模式
  - 自动隐藏控制栏
- ✅ 视频信息编辑 (标题、描述)
- ✅ 视频收藏功能

### 标签系统
- ✅ 标签创建、编辑、删除
- ✅ 视频标签管理 (添加/移除标签)
- ✅ 基于标签的视频推荐
- ✅ 相关视频展示 (同标签推荐)

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
- description: Text (视频描述) ⭐新增
- created_at: DateTime
- updated_at: DateTime
- tags: Relationship (Many-to-Many) ⭐新增

### Image 模型
- id: Integer (PK)
- title: String
- path: String
- source_id: Integer (FK)
- file_size: Integer
- width/height: Integer
- thumbnail_path: String
- is_favorite: Boolean
- created_at: DateTime

### Source 模型
- id: Integer (PK)
- name: String (显示名称)
- type: Enum ('local', 'nas')
- path: String (本地路径或 NAS 路径)
- nas_config: JSON (NAS 连接配置)
- scan_interval: Integer (自动扫描间隔, 分钟)
- is_active: Boolean
- last_scan_at: DateTime
- created_at: DateTime

### Tag 模型 ⭐新增
- id: Integer (PK)
- name: String (标签名)
- color: String (颜色代码, 默认 #1890ff)
- created_at: DateTime

### 关联表 video_tags ⭐新增
- video_id: Integer (FK)
- tag_id: Integer (FK)

## API 接口

### 视频 API
- `GET /api/videos` - 视频列表 (分页、搜索、排序、按标签筛选)
- `GET /api/videos/<id>` - 视频详情 (含标签、来源信息)
- `PUT /api/videos/<id>` - 更新视频信息 (标题、描述、收藏状态)
- `GET /api/videos/<id>/stream` - 视频流播放 (支持 Range 请求)
- `DELETE /api/videos/<id>` - 删除视频记录
- `POST /api/videos/<id>/favorite` - 收藏/取消收藏
- `POST /api/videos/<id>/thumbnail` - 重新生成缩略图
- `GET /api/videos/<id>/tags` - 获取视频标签 ⭐
- `POST /api/videos/<id>/tags` - 添加标签到视频 ⭐
- `DELETE /api/videos/<id>/tags/<tag_id>` - 从视频移除标签 ⭐
- `GET /api/videos/<id>/related` - 获取相关视频 (基于标签) ⭐

### 图片 API
- `GET /api/images` - 图片列表 (分页、搜索)
- `GET /api/images/<id>` - 图片详情
- `GET /api/images/<id>/file` - 获取原图
- `DELETE /api/images/<id>` - 删除图片记录
- `POST /api/images/<id>/favorite` - 收藏/取消收藏

### 来源配置 API
- `GET /api/sources` - 来源列表
- `POST /api/sources` - 创建来源
- `PUT /api/sources/<id>` - 更新来源
- `DELETE /api/sources/<id>` - 删除来源
- `POST /api/sources/<id>/scan` - 手动触发扫描
- `GET /api/sources/<id>/status` - 检查连接状态
- `GET /api/sources/<id>/stats` - 获取来源统计

### 标签 API ⭐新增
- `GET /api/tags` - 获取所有标签
- `POST /api/tags` - 创建标签
- `PUT /api/tags/<id>` - 更新标签
- `DELETE /api/tags/<id>` - 删除标签

### 收藏 API
- `GET /api/favorites` - 获取所有收藏 (视频+图片)
- `GET /api/favorites/stats` - 获取收藏统计

## 启动命令

### 后端
```bash
cd backend
python -m venv venv
source venv/bin/activate
pip install -r requirements.txt
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

- 前端: http://localhost:5173
- 后端: http://localhost:5000
- API Health: http://localhost:5000/api/health

## 页面路由

- `/` - 首页仪表盘
- `/videos` - 视频库
- `/videos/:id` - 视频播放页 ⭐
- `/images` - 图片库
- `/sources` - 来源配置
- `/favorites` - 我的收藏

## 待优化项

- [ ] 视频缩略图自动批量生成
- [ ] 播放历史记录
- [ ] 播放进度记忆
- [ ] 更多视频格式支持优化
- [ ] 移动端适配优化
