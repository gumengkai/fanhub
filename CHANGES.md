# FanHub 扫描功能修复与增强

## 修改摘要

本次更新修复了数据源扫描的增量扫描逻辑，确保：
1. ✅ 文件系统中已删除的文件从 FanHub 中删除
2. ✅ 文件系统中新增的文件从 FanHub 中新增
3. ✅ 收藏和喜欢数据在增量扫描时保留
4. ✅ 修复了视频库排序问题
5. ✅ 新增扫描日志查看功能

---

## 详细修改内容

### 1. 数据库模型更新 (`backend/app/models.py`)

#### 新增字段
- **Video 模型**: 添加 `file_modified_at` 字段，用于跟踪文件实际修改时间
- **Image 模型**: 添加 `file_modified_at` 字段，用于跟踪文件实际修改时间

#### 新增模型
- **ScanLog 模型**: 用于记录每次扫描操作的详细信息
  - 扫描状态（进行中/已完成/失败）
  - 新增/更新/删除的视频和图片数量
  - 详细操作记录（每个文件的操作类型、路径、标题等）
  - 错误信息列表

### 2. 扫描服务重构 (`backend/app/services/scanner.py`)

#### 关键修复
- **增量扫描逻辑**: 使用 `file_modified_at` 字段替代 `updated_at` 进行文件修改检测
- **收藏/喜欢状态保留**: 文件更新时保留原有的收藏和喜欢状态
- **重复插入修复**: 修复了多次扫描时尝试插入重复记录的问题

#### 新增功能
- **扫描日志记录**: 每次扫描操作都会记录到 ScanLog 表
- **详细操作追踪**: 记录每个文件的操作（新增/更新/删除）及收藏/喜欢状态变化
- **实时进度更新**: 扫描过程中实时更新扫描日志

### 3. API 路由更新 (`backend/app/routes/sources.py`)

#### 新增端点
- `GET /api/sources/scan-logs` - 获取扫描日志列表（支持分页）
- `GET /api/sources/scan-logs/<id>` - 获取单个扫描日志详情
- `DELETE /api/sources/scan-logs/<id>` - 删除扫描日志
- `POST /api/sources/scan-logs/clear` - 清空所有扫描日志

#### 更新端点
- `POST /api/sources/<id>/scan` - 现在会创建扫描日志记录并返回 scan_log_id

### 4. 前端页面新增 (`frontend/src/pages/ScanLogs/index.jsx`)

#### 功能特性
- 扫描日志列表展示（支持分页）
- 扫描状态可视化（进行中/已完成/失败）
- 统计信息展示（新增/更新/删除数量）
- 详细操作记录查看（抽屉式详情面板）
- 时间线展示每个文件的操作记录
- 支持清空日志

### 5. 前端路由与导航更新

#### App.jsx
- 新增 `/scan-logs` 路由

#### Layout/index.jsx
- 设置菜单新增"扫描日志"选项

#### api.js
- 新增 `sourcesApi.getScanLogs()` 方法
- 新增 `sourcesApi.getScanLog()` 方法
- 新增 `sourcesApi.deleteScanLog()` 方法
- 新增 `sourcesApi.clearScanLogs()` 方法

### 6. 数据库迁移脚本更新 (`backend/migrate.py`)

- 自动添加 `file_modified_at` 字段到 videos 和 images 表
- 自动创建 scan_logs 表

---

## 测试验证

运行测试脚本验证增量扫描功能：

```bash
cd /home/gmk/fanhub/backend
python3 test_scan.py
```

测试结果：
- ✅ 第一次扫描: 新增 2 个视频
- ✅ 第二次扫描: 无变化 (新增 0, 更新 0)
- ✅ 第三次扫描: 更新 1 个视频
- ✅ 第四次扫描: 删除 1 个视频
- ✅ 收藏/喜欢状态在增量扫描中保留

---

## 部署步骤

1. **数据库迁移**
   ```bash
   cd /home/gmk/fanhub/backend
   python3 migrate.py
   ```

2. **前端构建**
   ```bash
   cd /home/gmk/fanhub/frontend
   npm run build
   ```

3. **重启后端服务**
   ```bash
   # 停止现有服务
   pkill -f "flask run"
   
   # 启动服务
   cd /home/gmk/fanhub/backend
   python3 -m flask run --host=0.0.0.0 --port=11303
   ```

---

## 使用说明

### 查看扫描日志
1. 点击右上角用户菜单
2. 选择"扫描日志"
3. 查看所有扫描操作的详细记录

### 扫描数据源
1. 进入"来源设置"页面
2. 点击数据源旁边的"扫描"按钮
3. 扫描完成后可在"扫描日志"中查看详细结果

---

## 技术细节

### 增量扫描算法

```python
# 1. 获取数据库中该数据源的所有现有记录
existing_videos = {v.path: v for v in Video.query.filter_by(source_id=source.id).all()}

# 2. 遍历文件系统，处理每个文件
for file_path in scanned_files:
    if file_path in existing_videos:
        # 文件已存在，检查是否需要更新
        video = existing_videos[file_path]
        if video.file_modified_at is None or modified_time > video.file_modified_at:
            # 更新文件信息，保留收藏/喜欢状态
            video.file_size = file_size
            video.file_modified_at = modified_time
    else:
        # 新文件，创建记录
        video = Video(...)

# 3. 删除数据库中不存在于文件系统的记录
for path, video in existing_videos.items():
    if path not in scanned_paths:
        # 删除视频记录及相关数据（缩略图、观看历史等）
        db.session.delete(video)
```

### 收藏/喜欢状态保留机制

- **文件新增**: 新视频的 `is_favorite` 和 `is_liked` 默认为 `False`
- **文件更新**: 保留原有的 `is_favorite` 和 `is_liked` 值
- **文件删除**: 记录删除前的收藏/喜欢状态到扫描日志

---

## 注意事项

1. **现有数据**: 已存在的视频/图片记录的 `file_modified_at` 字段为 `NULL`，会在下次扫描时自动填充
2. **扫描性能**: 扫描大量文件时可能需要较长时间，请耐心等待
3. **日志清理**: 扫描日志会占用数据库存储空间，建议定期清理
