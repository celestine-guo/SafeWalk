# SafeWalk Backend

SafeWalk后端API服务，为女性夜间出行安全场景提供实时定位数据与紧急事件响应支持。

## 技术栈

- Python 3.9+
- FastAPI 0.104.1
- SQLAlchemy 2.0.23
- PostgreSQL (asyncpg)
- Pydantic 2.5.0
- Uvicorn

## 项目结构

```
backend/
├── app/                      # 应用核心代码
│   ├── api/                  # API层
│   │   ├── routers/          # 路由定义
│   │   │   ├── users.py      # 用户相关路由
│   │   │   ├── locations.py  # 定位相关路由
│   │   │   ├── emergencies.py # 紧急事件相关路由
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

### 3. 启动服务

```bash
uvicorn main:app --reload
```

服务将在 http://localhost:8000 启动。

### 4. 访问API文档

- Swagger UI: http://localhost:8000/docs
- ReDoc: http://localhost:8000/redoc

## 核心功能

1. **用户管理**：注册、查询、更新用户信息
2. **行程服务**：创建行程并查询行程详情
3. **定位服务**：按行程记录和查询用户位置信息
4. **紧急事件**：创建、更新和查询紧急事件

## 架构设计

- **分层架构**：API层 → 服务层 → 数据层 → 模型层
- **依赖注入**：使用FastAPI的依赖注入系统
- **异步编程**：使用Python原生async/await
- **数据验证**：使用Pydantic V2进行数据验证
- **配置管理**：使用环境变量进行配置

## 安全考虑

- 密码哈希处理（待实现）
- JWT身份验证（待实现）
- HTTPS支持（生产环境）
- 输入验证和参数校验

## 扩展性

- 预留实时通信接口（WebSocket）
- 预留地理空间计算接口
- 模块化设计，易于扩展新功能
