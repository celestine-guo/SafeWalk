from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.ext.asyncio import AsyncSession
from backend.app.data.database import get_db
from backend.app.models.domain import User, UserCreate, UserUpdate
from backend.app.services.impl import UserService

router = APIRouter(prefix="/users", tags=["users"])


@router.post("/", response_model=User)
async def create_user(user: UserCreate, db: AsyncSession = Depends(get_db)):
    """创建新用户"""
    service = UserService(db)
    user_model = await service.create_user(user)
    # 转换为响应模型
    return User(
        id=user_model.id,
        username=user_model.username,
        email=user_model.email,
        phone_number=user_model.phone_number,
        is_active=user_model.is_active,
        is_verified=user_model.is_verified,
        created_at=user_model.created_at,
        updated_at=user_model.updated_at
    )


@router.get("/{user_id}", response_model=User)
async def get_user(user_id: int, db: AsyncSession = Depends(get_db)):
    """获取用户信息"""
    service = UserService(db)
    user_model = await service.get_user_by_id(user_id)
    if not user_model:
        raise HTTPException(status_code=404, detail="User not found")
    return User(
        id=user_model.id,
        username=user_model.username,
        email=user_model.email,
        phone_number=user_model.phone_number,
        is_active=user_model.is_active,
        is_verified=user_model.is_verified,
        created_at=user_model.created_at,
        updated_at=user_model.updated_at
    )


@router.put("/{user_id}", response_model=User)
async def update_user(user_id: int, user: UserUpdate, db: AsyncSession = Depends(get_db)):
    """更新用户信息"""
    service = UserService(db)
    user_model = await service.update_user(user_id, user)
    if not user_model:
        raise HTTPException(status_code=404, detail="User not found")
    return User(
        id=user_model.id,
        username=user_model.username,
        email=user_model.email,
        phone_number=user_model.phone_number,
        is_active=user_model.is_active,
        is_verified=user_model.is_verified,
        created_at=user_model.created_at,
        updated_at=user_model.updated_at
    )
