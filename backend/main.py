from fastapi import FastAPI
from backend.config.settings import settings
from backend.app.api.routers import api_router
from backend.app.api.middleware import configure_middleware, global_exception_handler
from backend.core.logging import logger
from backend.app.data.database import engine, Base
import asyncio


async def create_tables():
    """创建数据库表"""
    async with engine.begin() as conn:
        await conn.run_sync(Base.metadata.create_all)


# 创建FastAPI应用实例
app = FastAPI(
    title=settings.APP_NAME,
    version=settings.APP_VERSION,
    description="SafeWalk后端API服务",
    docs_url="/docs",
    redoc_url="/redoc"
)

# 配置中间件
configure_middleware(app)

# 注册全局异常处理
app.add_exception_handler(Exception, global_exception_handler)

# 注册路由
app.include_router(api_router, prefix="/api")


@app.on_event("startup")
async def startup_event():
    """应用启动事件"""
    logger.info("Starting up SafeWalk backend service...")
    # 创建数据库表
    await create_tables()
    logger.info("Database tables created successfully")


@app.on_event("shutdown")
async def shutdown_event():
    """应用关闭事件"""
    logger.info("Shutting down SafeWalk backend service...")


@app.get("/")
async def root():
    """根路径"""
    return {
        "message": "SafeWalk Backend API",
        "version": settings.APP_VERSION,
        "status": "healthy"
    }


@app.get("/health")
async def health_check():
    """健康检查"""
    return {
        "status": "healthy",
        "service": settings.APP_NAME
    }
