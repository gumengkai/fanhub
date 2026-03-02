# FunHub - 娱乐中心应用

<div align="center">

🎬 一个现代化的本地媒体管理中心，支持视频库和图片库管理

[![Python](https://img.shields.io/badge/Python-3.13-blue.svg)](https://python.org)
[![Flask](https://img.shields.io/badge/Flask-3.x-green.svg)](https://flask.palletsprojects.com)
[![React](https://img.shields.io/badge/React-18-blue.svg)](https://react.dev)
[![Ant Design](https://img.shields.io/badge/AntD-5-blue.svg)](https://ant.design)

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
- 🖥️ **多源支持** - 本地目录、NAS 网络存储
- 🎨 **B 站主题** - 粉色主题 (#fb7299)、毛玻璃效果、流畅动画

---

## 🚀 快速开始

### 前置要求

- Python 3.13+
- Node.js 18+
- ffmpeg (视频缩略图生成)
- Git

### 一键安装

```bash
# 克隆项目
git clone <your-repo-url>
cd funhub

# 运行安装脚本
chmod +x install.sh
./install.sh
```

### 启动服务

```bash
# 一键启动前后端
./start-all.sh

# 或分别启动
./start-backend.sh  # 后端 (http://localhost:5000)
./start-frontend.sh # 前端 (http://localhost:5173)
```

### Docker 部署

```bash
# 使用 Docker Compose 启动
docker-compose up -d

# 查看状态
./status.sh
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
├── docker-compose.yml
└── *.sh                       # 启动脚本
```

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

## 🛠️ 开发指南

### 后端开发

```bash
cd backend
python -m venv venv
source venv/bin/activate
pip install -r requirements.txt
python migrate.py  # 运行数据库迁移
python run.py
```

### 前端开发

```bash
cd frontend
npm install
npm run dev
```

### 数据库迁移

```bash
cd backend
python migrate.py
```

---

## 📝 更新日志

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

</div>
