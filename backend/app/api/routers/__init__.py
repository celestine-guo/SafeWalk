from fastapi import APIRouter
from .emergencies import router as emergencies_router
from .locations import router as locations_router
from .trips import router as trips_router
from .users import router as users_router

# 创建主路由
api_router = APIRouter()

# 注册子路由
api_router.include_router(users_router)
api_router.include_router(trips_router)
api_router.include_router(locations_router)
api_router.include_router(emergencies_router)
