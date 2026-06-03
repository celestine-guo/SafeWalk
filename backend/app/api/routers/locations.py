from typing import List

from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.ext.asyncio import AsyncSession

from backend.app.api.auth import CurrentUser, require_current_user
from backend.app.data.database import get_db
from backend.app.models.domain import Location, LocationCreate
from backend.app.services.impl import LocationService

router = APIRouter(prefix="/locations", tags=["locations"])


def _to_location(location_model) -> Location:
    return Location(
        id=location_model.id,
        trip_id=location_model.trip_id,
        latitude=location_model.latitude,
        longitude=location_model.longitude,
        accuracy=location_model.accuracy,
        recorded_at=location_model.recorded_at,
        created_at=location_model.created_at,
        updated_at=location_model.updated_at,
    )


@router.post("/", response_model=Location)
async def create_location(
    location: LocationCreate,
    current_user: CurrentUser = Depends(require_current_user),
    db: AsyncSession = Depends(get_db),
):
    """创建定位记录"""
    service = LocationService(db)
    location_model = await service.create_location(current_user.clerk_user_id, location)
    if not location_model:
        raise HTTPException(status_code=404, detail="Trip not found")
    return _to_location(location_model)


@router.get("/trip/{trip_id}", response_model=List[Location])
async def get_trip_locations(
    trip_id: int,
    limit: int = 100,
    current_user: CurrentUser = Depends(require_current_user),
    db: AsyncSession = Depends(get_db),
):
    """获取行程的定位记录"""
    service = LocationService(db)
    location_models = await service.get_trip_locations(current_user.clerk_user_id, trip_id, limit)
    if location_models is None:
        raise HTTPException(status_code=404, detail="Trip not found")
    return [_to_location(location_model) for location_model in location_models]
