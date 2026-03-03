# FunHub 开发环境指南

🛠️ **开发模式启动** - 前后端分离，支持热重载

---

## 📁 项目结构

```
funhub/
├── backend/              # Flask 后端
│   ├── app/             # 应用代码
│   ├── run.py           # 入口文件
│   ├── config.py        # 配置
│   ├── requirements.txt # Python 依赖
│   └── venv/            # Python 虚拟环境
├── frontend/            # React 前端
│   ├── src/            # 源代码
│   ├── package.json    # Node 依赖
│   └── vite.config.js  # Vite 配置
├── scripts/            # 启动脚本
│   ├── start-dev.sh        # 总启动脚本（前后端一起）
│   ├── start-backend.sh    # 仅后端
│   └── start-frontend.sh   # 仅前端
├── storage/            # 数据存储
│   ├── database/       # SQLite 数据库
│   └── thumbnails/     # 缩略图缓存
└── logs/               # 日志文件
```

---

## 🚀 快速启动

### 方式一：一键启动（推荐）

同时启动前端和后端服务：

```bash
cd /home/gmk/funhub
./scripts/start-dev.sh
```

**访问地址：**
- 前端：http://localhost:5173
- 后端：http://localhost:5000

### 方式二：分别启动

**终端 1 - 启动后端：**
```bash
./scripts/start-backend.sh
```

**终端 2 - 启动前端：**
```bash
./scripts/start-frontend.sh
```

---

## 🔧 开发特性

### 后端（Flask）

| 特性 | 说明 |
|------|------|
| 端口 | 5000 |
| 调试模式 | 启用 |
| 自动重载 | 启用（代码修改后自动重启） |
| 数据库 | SQLite（本地存储） |
| CORS | 允许前端跨域访问 |

### 前端（Vite + React）

| 特性 | 说明 |
|------|------|
| 端口 | 5173 |
| 热重载 | 启用（修改即刷新） |
| 后端代理 | 自动代理 `/api` 到 `localhost:5000` |
| 源码映射 | 启用（便于调试） |

---

## 📝 常用命令

### 后端相关

```bash
# 进入后端目录
cd backend

# 激活虚拟环境
source venv/bin/activate

# 安装依赖
pip install -r requirements.txt

# 运行迁移
python3 migrate.py

# 单独启动后端
python3 run.py
```

### 前端相关

```bash
# 进入前端目录
cd frontend

# 安装依赖
npm install

# 开发模式
npm run dev

# 构建生产版本
npm run build

# 预览生产构建
npm run preview

# 代码检查
npm run lint
```

---

## 🐛 故障排查

### 后端无法启动

```bash
# 检查 Python 版本
python3 --version  # 需要 3.8+

# 检查虚拟环境
ls -la backend/venv/

# 重新安装依赖
cd backend
rm -rf venv
python3 -m venv venv
source venv/bin/activate
pip install -r requirements.txt
```

### 前端无法启动

```bash
# 检查 Node.js 版本
node --version  # 需要 18+

# 清除缓存重新安装
cd frontend
rm -rf node_modules package-lock.json
npm install
```

### 端口被占用

```bash
# 检查端口占用
lsof -i :5000  # 后端端口
lsof -i :5173  # 前端端口

# 杀死占用进程
kill -9 <PID>
```

### 数据库问题

```bash
# 删除数据库重新初始化
rm storage/database/funhub.db

# 重启后端，会自动创建新数据库
./scripts/start-backend.sh
```

---

## 📊 开发工作流

1. **启动开发环境**
   ```bash
   ./scripts/start-dev.sh
   ```

2. **进行开发**
   - 前端修改：保存后自动热重载
   - 后端修改：保存后自动重启

3. **查看日志**
   ```bash
   # 实时查看后端日志
   tail -f logs/backend.log

   # 实时查看前端日志
   tail -f logs/frontend.log
   ```

4. **测试 API**
   ```bash
   # 健康检查
   curl http://localhost:5000/api/health

   # 获取视频列表
   curl http://localhost:5000/api/videos
   ```

5. **停止服务**
   - 按 `Ctrl+C` 停止所有服务

---

## 🔐 环境变量

开发模式下自动设置以下环境变量：

| 变量 | 值 | 说明 |
|------|-----|------|
| `FLASK_ENV` | `development` | Flask 开发模式 |
| `DATABASE_PATH` | `./storage/database/funhub.db` | 本地数据库 |
| `CORS_ORIGINS` | `http://localhost:5173,...` | 允许跨域 |
| `SECRET_KEY` | `dev-secret-key...` | 开发密钥 |

---

## 📦 与 Docker 部署的区别

| 特性 | 开发模式 | Docker 部署 |
|------|----------|-------------|
| 启动方式 | 脚本直接运行 | 容器化运行 |
| 热重载 | ✅ 支持 | ❌ 需重新构建 |
| 调试 | ✅ 方便 | ⚠️ 需进入容器 |
| 端口 | 5000 + 5173 | 8080（统一） |
| 数据库 | 本地文件 | 容器卷挂载 |
| 适用场景 | 开发、调试 | 生产、NAS 部署 |

---

**开发愉快！** 🎉
