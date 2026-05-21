from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.ext.asyncio import AsyncSession
from backend.app.data.database import get_db
from backend.app.models.domain import Emergency, EmergencyCreate, EmergencyUpdate
from backend.app.services.impl import EmergencyService
from typing import List

router = APIRouter(prefix="/emergencies", tags=["emergencies"])


@router.post("/", response_model=Emergency)
async def create_emergency(emergency: EmergencyCreate, db: AsyncSession = Depends(get_db)):
    """创建紧急事件"""
    service = EmergencyService(db)
    emergency_model = await service.create_emergency(emergency)
    return Emergency(
        id=emergency_model.id,
        user_id=emergency_model.user_id,
        type=emergency_model.type,
        status=emergency_model.status,
        latitude=emergency_model.latitude,
        longitude=emergency_model.longitude,
        timestamp=emergency_model.timestamp,
        resolved_at=emergency_model.resolved_at,
        created_at=emergency_model.created_at,
        updated_at=emergency_model.updated_at
    )


@router.put("/{emergency_id}", response_model=Emergency)
async def update_emergency(emergency_id: int, emergency: EmergencyUpdate, db: AsyncSession = Depends(get_db)):
    """更新紧急事件"""
    service = EmergencyService(db)
    emergency_model = await service.update_emergency(emergency_id, emergency)
    if not emergency_model:
        raise HTTPException(status_code=404, detail="Emergency not found")
    return Emergency(
        id=emergency_model.id,
        user_id=emergency_model.user_id,
        type=emergency_model.type,
        status=emergency_model.status,
        latitude=emergency_model.latitude,
        longitude=emergency_model.longitude,
        timestamp=emergency_model.timestamp,
        resolved_at=emergency_model.resolved_at,
        created_at=emergency_model.created_at,
        updated_at=emergency_model.updated_at
    )


@router.get("/active", response_model=List[Emergency])
async def get_active_emergencies(db: AsyncSession = Depends(get_db)):
    """获取活跃的紧急事件"""
    service = EmergencyService(db)
    emergency_models = await service.get_active_emergencies()
    return [
        Emergency(
            id=emergency.id,
            user_id=emergency.user_id,
            type=emergency.type,
            status=emergency.status,
            latitude=emergency.latitude,
            longitude=emergency.longitude,
            timestamp=emergency.timestamp,
            resolved_at=emergency.resolved_at,
            created_at=emergency.created_at,
            updated_at=emergency.updated_at
        )
        for emergency in emergency_models
    ]
