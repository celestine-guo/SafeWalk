from datetime import datetime
from typing import List, Optional

from sqlalchemy import desc, select

from backend.app.data.models import (
    EmergencyModel,
    LocationModel,
    NotificationEventModel,
    TripModel,
    UserModel,
)
from backend.app.data.repositories import (
    BaseRepository,
    LocationRepository,
    NotificationEventRepository,
    TripRepository,
)
from backend.app.models.domain import (
    EmergencyCreate,
    EmergencyUpdate,
    LocationCreate,
    TripCreate,
    UserCreate,
    UserUpdate,
)
from .base import BaseService


class UserRepository(BaseRepository[UserModel]):
    """用户仓储实现"""

    model = UserModel


class EmergencyRepository(BaseRepository[EmergencyModel]):
    """紧急事件仓储实现"""

    model = EmergencyModel


class UserService(BaseService):
    """用户服务实现"""

    async def health_check(self) -> dict:
        return {"status": "healthy", "service": "UserService"}

    async def create_user(self, user_data: UserCreate) -> UserModel:
        user = UserModel(
            username=user_data.username,
            email=user_data.email,
            phone_number=user_data.phone_number,
            password_hash=user_data.password,
        )
        repo = UserRepository(self.db)
        return await repo.create(user)

    async def get_user_by_id(self, user_id: int) -> Optional[UserModel]:
        repo = UserRepository(self.db)
        return await repo.get_by_id(user_id)

    async def update_user(self, user_id: int, user_data: UserUpdate) -> Optional[UserModel]:
        repo = UserRepository(self.db)
        user = await repo.get_by_id(user_id)
        if not user:
            return None

        for field, value in user_data.model_dump(exclude_unset=True).items():
            setattr(user, field, value)

        return await repo.update(user)


class TripService(BaseService):
    """行程服务实现"""

    async def create_trip(self, trip_data: TripCreate) -> TripModel:
        trip = TripModel(
            user_id=trip_data.user_id,
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

    async def get_trip(self, trip_id: int) -> Optional[TripModel]:
        repo = TripRepository(self.db)
        return await repo.get_by_id(trip_id)

    async def get_trip_detail(self, trip_id: int) -> tuple[Optional[TripModel], Optional[LocationModel], list[NotificationEventModel]]:
        trip_repo = TripRepository(self.db)
        location_repo = LocationRepository(self.db)
        notification_repo = NotificationEventRepository(self.db)

        trip = await trip_repo.get_by_id(trip_id)
        if not trip:
            return None, None, []

        latest_location = await location_repo.get_latest_for_trip(trip_id)
        notifications = await notification_repo.list_events(trip_id=trip_id)
        return trip, latest_location, notifications


class LocationService(BaseService):
    """定位服务实现"""

    async def health_check(self) -> dict:
        return {"status": "healthy", "service": "LocationService"}

    async def create_location(self, location_data: LocationCreate) -> LocationModel:
        location = LocationModel(
            trip_id=location_data.trip_id,
            latitude=location_data.latitude,
            longitude=location_data.longitude,
            accuracy=location_data.accuracy,
            recorded_at=location_data.recorded_at,
        )
        repo = LocationRepository(self.db)
        return await repo.create(location)

    async def get_trip_locations(self, trip_id: int, limit: int = 100) -> List[LocationModel]:
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

    async def create_emergency(self, emergency_data: EmergencyCreate) -> EmergencyModel:
        emergency = EmergencyModel(
            user_id=emergency_data.user_id,
            type=emergency_data.type.value,
            status=emergency_data.status.value,
            latitude=emergency_data.latitude,
            longitude=emergency_data.longitude,
        )
        repo = EmergencyRepository(self.db)
        return await repo.create(emergency)

    async def update_emergency(self, emergency_id: int, emergency_data: EmergencyUpdate) -> Optional[EmergencyModel]:
        repo = EmergencyRepository(self.db)
        emergency = await repo.get_by_id(emergency_id)
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

    async def get_active_emergencies(self) -> List[EmergencyModel]:
        result = await self.db.execute(
            select(EmergencyModel)
            .where(EmergencyModel.status != "resolved")
            .order_by(desc(EmergencyModel.timestamp), desc(EmergencyModel.id))
        )
        return list(result.scalars().all())
