# SafeWalk Backend

SafeWalk后端API服务，为女性夜间出行安全场景提供实时定位数据与紧急事件响应支持。

## 技术栈

- Python 3.10+
- FastAPI 0.104.1
- SQLAlchemy 2.0.23
- Supabase PostgreSQL (asyncpg)
- Clerk（后端会话鉴权）
- Pydantic 2.11.2
- Uvicorn

## 项目结构

```
backend/
├── app/                      # 应用核心代码
│   ├── api/                  # API层
│   │   ├── routers/          # 路由定义
│   │   │   ├── trips.py      # 行程相关路由
│   │   │   ├── locations.py  # 定位相关路由
│   │   │   ├── emergencies.py # 紧急事件相关路由（SOS）
│   │   │   ├── guardians.py  # 守护人相关路由
│   │   │   ├── chats.py      # 聊天消息路由
│   │   │   ├── trip_events.py # 行程事件路由
│   │   │   └── __init__.py
│   │   ├── middleware.py     # 中间件配置
│   │   └── __init__.py
│   ├── services/             # 服务层
│   │   ├── base.py           # 服务基类
│   │   ├── impl.py           # 服务实现
│   │   └── __init__.py
│   ├── data/                 # 数据层
│   │   ├── database.py       # 数据库连接
│   │   ├── models.py         # ORM模型
│   │   ├── repositories.py   # 仓储层
│   │   └── __init__.py
│   ├── models/               # 模型层
│   │   ├── base.py           # 基础模型
│   │   ├── domain.py         # 领域模型
│   │   └── __init__.py
│   └── __init__.py
├── config/                   # 配置管理
│   ├── settings.py           # 配置类
│   └── __init__.py
├── core/                     # 核心功能
│   ├── logging.py            # 日志配置
│   ├── exceptions.py         # 异常处理
│   └── __init__.py
├── utils/                    # 工具函数
│   └── __init__.py
├── .env.example              # 环境变量示例
├── sql/
│   ├── supabase_schema.sql   # Supabase表结构初始化脚本
│   └── supabase_security.sql # Supabase安全策略/RLS脚本
├── main.py                   # 应用入口
├── requirements.txt          # 依赖文件
├── pyproject.toml            # 项目配置
└── README.md                 # 项目说明
```

## 快速开始

### 1. 安装依赖

```bash
cd backend
pip install -r requirements.txt
```

### 2. 配置环境变量

复制环境变量示例文件并修改：

```bash
cp .env.example .env
# 编辑.env文件，设置数据库连接等配置
```

### 3. 初始化 Supabase 表结构与安全策略

当前生产 Supabase 项目：

- Project ID / ref: `zmaxcsvwmeboegezsmxe`
- Project URL: `https://zmaxcsvwmeboegezsmxe.supabase.co`
- Region: `ap-southeast-1`

在 Supabase SQL Editor 依次执行：

1. `backend/sql/supabase_schema.sql`
2. `backend/sql/supabase_security.sql`

### 4. 启动服务

```bash
uvicorn backend.main:app --reload
```

服务将在 http://localhost:8000 启动。

### 5. 访问API文档

- Swagger UI: http://localhost:8000/docs
- ReDoc: http://localhost:8000/redoc

## Android 端 API 地址配置

Android 客户端读取 `SAFEWALK_BACKEND_BASE_URL`。可任选一种方式：

1. `local.properties`：
   `SAFEWALK_BACKEND_BASE_URL=http://10.0.2.2:8000/api`
2. 环境变量：
   `export SAFEWALK_BACKEND_BASE_URL=http://<your-host>:8000/api`
3. Gradle 参数：
   `./gradlew assembleDebug -PSAFEWALK_BACKEND_BASE_URL=http://<your-host>:8000/api`

真机体验和分发版本必须使用公网 HTTPS FastAPI 地址，例如：

```properties
SAFEWALK_BACKEND_BASE_URL=https://<your-backend-domain>/api
```

当前架构保留 FastAPI + Clerk；Android 不直接访问 Supabase 业务表，也不需要配置 Supabase `service_role` key。

## Render 托管

仓库根目录提供 `render.yaml`，可在 Render 上创建 Blueprint Web Service：

- Runtime: Python
- Region: Singapore
- Root Directory: 留空，使用仓库根目录
- Build Command: `pip install -r backend/requirements.txt`
- Start Command: `uvicorn backend.main:app --host 0.0.0.0 --port $PORT`
- Health Check Path: `/health`

创建 Blueprint 时在 Render Dashboard 填写这些 secret 环境变量：

- `DATABASE_URL`：Supabase Session Pooler asyncpg 连接串，保留 `?ssl=require`
- `CLERK_SECRET_KEY`
- `CLERK_PUBLISHABLE_KEY`
- `CLERK_JWT_KEY`：没有就留空
- `QWEN_API_KEY`：没有启用聊天能力可先留空

部署成功后，把 Android 构建配置中的 `SAFEWALK_BACKEND_BASE_URL` 改为：

```properties
SAFEWALK_BACKEND_BASE_URL=https://<render-service>.onrender.com/api
```

## 核心功能

1. **行程服务**：创建行程并查询行程详情
2. **定位服务**：按行程记录和查询用户位置信息
3. **紧急事件（SOS）**：创建、更新和查询紧急事件
4. **守护人管理**：维护守护人联系人
5. **聊天消息**：保存与读取行程聊天消息
6. **行程事件**：保存与读取行程事件流

## 架构设计

- **分层架构**：API层 → 服务层 → 数据层 → 模型层
- **依赖注入**：使用FastAPI的依赖注入系统
- **异步编程**：使用Python原生async/await
- **数据验证**：使用Pydantic V2进行数据验证
- **配置管理**：使用环境变量进行配置

## 安全考虑

- 后端所有业务路由通过 Clerk 会话令牌认证。
- 数据库以 `user_id = Clerk user_id` 做资源归属校验，接口层返回最小错误信息。
- `supabase_security.sql` 提供默认收紧的权限策略与 RLS 防御基线。
- 生产环境务必使用 HTTPS，避免泄漏 `Authorization` 头。

## 扩展性

- 预留实时通信接口（WebSocket）
- 预留地理空间计算接口
- 模块化设计，易于扩展新功能
