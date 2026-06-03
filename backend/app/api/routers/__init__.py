from fastapi import APIRouter
from .chats import router as chats_router
from .emergencies import router as emergencies_router
from .guardians import router as guardians_router
from .locations import router as locations_router
from .trips import router as trips_router
from .trip_events import router as trip_events_router

# 创建主路由
api_router = APIRouter()

# 注册子路由
api_router.include_router(trips_router)
api_router.include_router(locations_router)
api_router.include_router(emergencies_router)
api_router.include_router(guardians_router)
api_router.include_router(chats_router)
api_router.include_router(trip_events_router)
