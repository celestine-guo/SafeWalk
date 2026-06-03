from typing import Optional

from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.ext.asyncio import AsyncSession

from backend.app.api.auth import CurrentUser, require_current_user
from backend.app.data.database import get_db
from backend.app.models.domain import TripEvent, TripEventCreate
from backend.app.services.impl import TripEventService

router = APIRouter(prefix="/trip-events", tags=["trip-events"])


def _to_trip_event(event_model) -> TripEvent:
    return TripEvent(
        id=event_model.id,
        trip_id=event_model.trip_id,
        user_id=event_model.user_id,
        event_type=event_model.event_type,
        title=event_model.title,
        content=event_model.content,
        payload_json=event_model.payload_json,
        created_at=event_model.created_at,
        updated_at=event_model.updated_at,
    )


@router.post("/", response_model=TripEvent)
async def create_trip_event(
    event: TripEventCreate,
    current_user: CurrentUser = Depends(require_current_user),
    db: AsyncSession = Depends(get_db),
):
    service = TripEventService(db)
    event_model = await service.create_event(current_user.clerk_user_id, event)
    if event_model is None:
        raise HTTPException(status_code=404, detail="Trip not found")
    return _to_trip_event(event_model)


@router.get("/", response_model=list[TripEvent])
async def list_trip_events(
    trip_id: Optional[int] = None,
    current_user: CurrentUser = Depends(require_current_user),
    db: AsyncSession = Depends(get_db),
):
    service = TripEventService(db)
    event_models = await service.list_events(current_user.clerk_user_id, trip_id)
    return [_to_trip_event(item) for item in event_models]
