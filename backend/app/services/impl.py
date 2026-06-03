from datetime import datetime
from typing import Optional

from sqlalchemy import desc, select

from backend.app.data.models import (
    ChatMessageModel,
    EmergencyModel,
    GuardianModel,
    LocationModel,
    NotificationEventModel,
    TripEventModel,
    TripModel,
)
from backend.app.data.repositories import (
    ChatMessageRepository,
    EmergencyRepository,
    GuardianRepository,
    LocationRepository,
    NotificationEventRepository,
    TripEventRepository,
    TripRepository,
)
from backend.app.models.domain import (
    ChatRequest,
    EmergencyCreate,
    EmergencyUpdate,
    GuardianCreate,
    LocationCreate,
    TripCreate,
    TripEventCreate,
)

from .base import BaseService


class TripService(BaseService):
    """行程服务实现"""

    async def create_trip(self, user_id: str, trip_data: TripCreate) -> TripModel:
        trip = TripModel(
            user_id=user_id,
            mode=trip_data.mode.value,
            status="active",
            start_latitude=trip_data.start_latitude,
            start_longitude=trip_data.start_longitude,
            destination_latitude=trip_data.destination_latitude,
            destination_longitude=trip_data.destination_longitude,
            planned_arrival_at=trip_data.planned_arrival_at,
            deviation_threshold_meters=trip_data.deviation_threshold_meters or 100.0,
            timeout_grace_minutes=trip_data.timeout_grace_minutes or 10,
            planned_path_json=[point.model_dump() for point in trip_data.planned_path],
        )
        repo = TripRepository(self.db)
        return await repo.create(trip)

    async def get_owned_trip(self, trip_id: int, user_id: str) -> Optional[TripModel]:
        repo = TripRepository(self.db)
        return await repo.get_owned_by_id(trip_id, user_id)

    async def get_owned_trip_detail(
        self,
        trip_id: int,
        user_id: str,
    ) -> tuple[Optional[TripModel], Optional[LocationModel], list[NotificationEventModel]]:
        trip_repo = TripRepository(self.db)
        location_repo = LocationRepository(self.db)
        notification_repo = NotificationEventRepository(self.db)

        trip = await trip_repo.get_owned_by_id(trip_id, user_id)
        if not trip:
            return None, None, []

        latest_location = await location_repo.get_latest_for_trip(trip_id)
        notifications = await notification_repo.list_events(trip_id=trip_id, user_id=user_id)
        return trip, latest_location, notifications


class LocationService(BaseService):
    """定位服务实现"""

    async def health_check(self) -> dict:
        return {"status": "healthy", "service": "LocationService"}

    async def create_location(self, user_id: str, location_data: LocationCreate) -> Optional[LocationModel]:
        trip_repo = TripRepository(self.db)
        trip = await trip_repo.get_owned_by_id(location_data.trip_id, user_id)
        if not trip:
            return None

        location = LocationModel(
            trip_id=location_data.trip_id,
            latitude=location_data.latitude,
            longitude=location_data.longitude,
            accuracy=location_data.accuracy,
            recorded_at=location_data.recorded_at,
        )
        repo = LocationRepository(self.db)
        return await repo.create(location)

    async def get_trip_locations(
        self,
        user_id: str,
        trip_id: int,
        limit: int = 100,
    ) -> Optional[list[LocationModel]]:
        trip_repo = TripRepository(self.db)
        trip = await trip_repo.get_owned_by_id(trip_id, user_id)
        if not trip:
            return None

        result = await self.db.execute(
            select(LocationModel)
            .where(LocationModel.trip_id == trip_id)
            .order_by(desc(LocationModel.recorded_at), desc(LocationModel.id))
            .limit(limit)
        )
        return list(result.scalars().all())


class EmergencyService(BaseService):
    """紧急事件服务实现"""

    async def health_check(self) -> dict:
        return {"status": "healthy", "service": "EmergencyService"}

    async def create_emergency(self, user_id: str, emergency_data: EmergencyCreate):
        emergency = EmergencyModel(
            user_id=user_id,
            type=emergency_data.type.value,
            status=emergency_data.status.value,
            latitude=emergency_data.latitude,
            longitude=emergency_data.longitude,
        )
        repo = EmergencyRepository(self.db)
        return await repo.create(emergency)

    async def update_emergency(
        self,
        emergency_id: int,
        user_id: str,
        emergency_data: EmergencyUpdate,
    ):
        repo = EmergencyRepository(self.db)
        emergency = await repo.get_owned_by_id(emergency_id, user_id)
        if not emergency:
            return None

        update_data = emergency_data.model_dump(exclude_unset=True)
        for field, value in update_data.items():
            if hasattr(value, "value"):
                value = value.value
            setattr(emergency, field, value)

        if emergency.status == "resolved" and not emergency.resolved_at:
            emergency.resolved_at = datetime.utcnow()

        return await repo.update(emergency)

    async def get_active_emergencies_for_user(self, user_id: str):
        repo = EmergencyRepository(self.db)
        return await repo.list_active_by_user(user_id)


class GuardianService(BaseService):
    """守护人服务实现"""

    async def create_guardian(self, user_id: str, guardian_data: GuardianCreate) -> GuardianModel:
        guardian = GuardianModel(
            user_id=user_id,
            guardian_name=guardian_data.guardian_name,
            guardian_phone=guardian_data.guardian_phone,
            relationship=guardian_data.relationship,
            is_primary=guardian_data.is_primary,
            note=guardian_data.note,
        )
        repo = GuardianRepository(self.db)
        return await repo.create(guardian)

    async def list_guardians(self, user_id: str) -> list[GuardianModel]:
        repo = GuardianRepository(self.db)
        return await repo.list_by_user(user_id)


class ChatService(BaseService):
    """聊天消息服务实现"""

    async def create_user_message(self, user_id: str, chat_data: ChatRequest) -> Optional[ChatMessageModel]:
        trip_repo = TripRepository(self.db)
        trip = await trip_repo.get_owned_by_id(chat_data.trip_id, user_id)
        if not trip:
            return None

        repo = ChatMessageRepository(self.db)
        message = ChatMessageModel(
            trip_id=chat_data.trip_id,
            user_id=user_id,
            role="user",
            content=chat_data.message,
            provider="clerk",
        )
        return await repo.create(message)

    async def list_trip_messages(self, user_id: str, trip_id: int) -> Optional[list[ChatMessageModel]]:
        trip_repo = TripRepository(self.db)
        trip = await trip_repo.get_owned_by_id(trip_id, user_id)
        if not trip:
            return None

        repo = ChatMessageRepository(self.db)
        return await repo.list_by_trip(trip_id)


class TripEventService(BaseService):
    """行程事件服务实现"""

    async def create_event(self, user_id: str, event_data: TripEventCreate) -> Optional[TripEventModel]:
        trip_repo = TripRepository(self.db)
        trip = await trip_repo.get_owned_by_id(event_data.trip_id, user_id)
        if not trip:
            return None

        repo = TripEventRepository(self.db)
        event = TripEventModel(
            trip_id=event_data.trip_id,
            user_id=user_id,
            event_type=event_data.event_type.value,
            title=event_data.title,
            content=event_data.content,
            payload_json=event_data.payload,
        )
        return await repo.create(event)

    async def list_events(self, user_id: str, trip_id: Optional[int] = None) -> list[TripEventModel]:
        repo = TripEventRepository(self.db)
        return await repo.list_events(trip_id=trip_id, user_id=user_id)
