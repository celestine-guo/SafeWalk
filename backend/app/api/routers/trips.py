from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.ext.asyncio import AsyncSession

from backend.app.api.auth import CurrentUser, require_current_user
from backend.app.data.database import get_db
from backend.app.models.domain import Location, NotificationEvent, Trip, TripCreate, TripDetail
from backend.app.services.impl import TripService

router = APIRouter(prefix="/trips", tags=["trips"])


def _to_trip(trip_model) -> Trip:
    return Trip(
        id=trip_model.id,
        user_id=trip_model.user_id,
        mode=trip_model.mode,
        status=trip_model.status,
        start_latitude=trip_model.start_latitude,
        start_longitude=trip_model.start_longitude,
        destination_latitude=trip_model.destination_latitude,
        destination_longitude=trip_model.destination_longitude,
        planned_arrival_at=trip_model.planned_arrival_at,
        deviation_threshold_meters=trip_model.deviation_threshold_meters,
        timeout_grace_minutes=trip_model.timeout_grace_minutes,
        planned_path_json=trip_model.planned_path_json,
        started_at=trip_model.started_at,
        arrived_at=trip_model.arrived_at,
        created_at=trip_model.created_at,
        updated_at=trip_model.updated_at,
    )


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


def _to_notification(notification_model) -> NotificationEvent:
    return NotificationEvent(
        id=notification_model.id,
        trip_id=notification_model.trip_id,
        user_id=notification_model.user_id,
        type=notification_model.type,
        title=notification_model.title,
        body=notification_model.body,
        status=notification_model.status,
        payload_json=notification_model.payload_json,
        created_at=notification_model.created_at,
        updated_at=notification_model.updated_at,
    )


@router.post("/", response_model=Trip)
async def create_trip(
    trip: TripCreate,
    current_user: CurrentUser = Depends(require_current_user),
    db: AsyncSession = Depends(get_db),
):
    """创建新行程"""
    service = TripService(db)
    trip_model = await service.create_trip(current_user.clerk_user_id, trip)
    return _to_trip(trip_model)


@router.get("/{trip_id}", response_model=TripDetail)
async def get_trip_detail(
    trip_id: int,
    current_user: CurrentUser = Depends(require_current_user),
    db: AsyncSession = Depends(get_db),
):
    """获取行程详情"""
    service = TripService(db)
    trip_model, latest_location, notifications = await service.get_owned_trip_detail(
        trip_id,
        current_user.clerk_user_id,
    )
    if not trip_model:
        raise HTTPException(status_code=404, detail="Trip not found")

    return TripDetail(
        trip=_to_trip(trip_model),
        latest_location=_to_location(latest_location) if latest_location else None,
        notifications=[_to_notification(item) for item in notifications],
    )
