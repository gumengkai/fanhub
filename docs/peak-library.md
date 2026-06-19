# FanPeak 巅峰库需求文档

## 概述

巅峰库（Peak Library）是 FanHub 的一个独立视频库，类似于抖音库，用于存储和管理特定类型的视频内容。它拥有独立的 API 端点、Web 页面和 Android 应用。

---

## 后端需求

### API 端点

| 端点 | 方法 | 描述 |
|------|------|------|
| `/api/peak` | GET | 获取视频列表（支持分页、筛选、随机排序） |
| `/api/peak/<id>` | GET | 获取单个视频详情 |
| `/api/peak/<id>` | DELETE | 删除视频 |
| `/api/peak/<id>/stream` | GET | 视频流播放（支持 Range 请求） |
| `/api/peak/<id>/thumbnail` | GET | 获取视频缩略图 |
| `/api/peak/<id>/like` | POST | 切换喜欢状态 |
| `/api/peak/<id>/favorite` | POST | 切换收藏状态 |
| `/api/peak/<id>/history` | POST/PUT | 更新观看历史 |
| `/api/peak/stats` | GET | 获取统计数据 |

### 功能特性

1. **随机排序**: 支持 `sort_by=random` 参数，从数据库随机返回视频
2. **筛选功能**: 支持按喜欢、收藏、未观看筛选
3. **来源隔离**: 只显示 `media_type='peak'` 的来源中的视频
4. **完整播放**: 支持视频流播放、进度记忆、历史记录

### 数据来源

- 通过 `/api/sources` 创建 `media_type='peak'` 的来源
- 扫描后会将视频归类到巅峰库

---

## Web 前端需求

### 页面路由

| 路由 | 组件 | 描述 |
|------|------|------|
| `/peak` | PeakLibrary | 巅峰库页面（类似抖音库） |

### 功能特性

1. **随机播放**: 默认开启随机模式，每次进入页面视频顺序不同
2. **筛选功能**: 支持全部/喜欢/收藏/标签筛选
3. **播放控制**: 播放/暂停、静音、音量、进度条拖动
4. **手势操作**: 上下滑动切换视频、双击点赞
5. **视频信息**: 显示标题、描述、标签

### 来源配置

- 在"来源配置"页面添加媒体类型"巅峰库"
- 统计卡片显示巅峰库来源数量

---

## Android 应用需求

### FanPeak 独立应用

- **包名**: `com.fanpeak.app`
- **应用名**: FanPeak
- **主题色**: 红色系（深红 `#DC143C` + 浅红 `#FF6B6B`）

### 功能特性

1. **抖音风格界面**: 全屏视频、上下滑动切换
2. **随机播放**: 默认从 `/api/peak?sort_by=random` 获取视频
3. **双击点赞**: 带爱心动画效果
4. **收藏功能**: 收藏视频到个人中心
5. **筛选功能**: 全部/喜欢/收藏

### 后端地址

- 默认: `http://192.168.31.40:11303`

---

## 与抖音库的区别

| 特性 | 抖音库 (Douyin) | 巅峰库 (Peak) |
|------|----------------|---------------|
| 媒体类型 | `media_type='douyin'` | `media_type='peak'` |
| Web 路由 | `/douyin` | `/peak` |
| Android 应用 | Fantok (TikTok风格) | FanPeak (红色主题) |
| 主题色 | 粉红+青色 | 深红+浅红 |
| API 端点 | `/api/douyin` | `/api/peak` |

---

## 文件位置

### 后端
- `/home/gmk/fanhub/backend/app/routes/peak.py` - 巅峰库 API 路由
- `/home/gmk/fanhub/backend/app/routes/sources.py` - 来源配置（需支持 `media_type='peak'`）
- `/home/gmk/fanhub/backend/app/__init__.py` - 注册 peak 蓝图

### Web 前端
- `/home/gmk/fanhub/frontend/src/pages/PeakLibrary/` - 巅峰库页面
- `/home/gmk/fanhub/frontend/src/services/api.js` - peakApi 服务
- `/home/gmk/fanhub/frontend/src/App.jsx` - 添加 /peak 路由
- `/home/gmk/fanhub/frontend/src/components/Layout/index.jsx` - 导航菜单

### Android
- `/home/gmk/fanhub/android/fanpeak/` - FanPeak 应用源码
- `/home/gmk/fanhub/android/settings.gradle.kts` - 包含 fanpeak 模块

### APK 输出
- `/mnt/fan/apk/fanpeak-release.apk` - FanPeak 发布版

---

## 随机排序实现

### 后端
```python
if sort_by == 'random':
    query = query.order_by(func.random())
```

### Web 前端
```javascript
const response = await peakApi.getList({ per_page: 1000, sort_by: 'random' })
```

### Android
```kotlin
apiService.getPeakVideos(
    page = 1,
    perPage = perPage,
    sortBy = "random"
)
```

---

## 使用流程

1. **创建来源**: 在 Web 端"来源配置"添加 `media_type='peak'` 的来源
2. **扫描视频**: 扫描来源目录，视频会自动归类到巅峰库
3. **Web 访问**: 访问 `/peak` 路径浏览巅峰库
4. **Android 访问**: 安装 FanPeak APK，连接后端服务器

---

## 注意事项

1. 巅峰库和抖音库是完全独立的，视频不会互相显示
2. 随机排序在数据库层面实现，每次请求顺序都不同
3. 播放历史、喜欢、收藏等数据与主视频库共享
4. 删除视频会同时删除文件和数据库记录

---

*文档创建时间: 2026-05-09*
*最后更新: 2026-05-09*
