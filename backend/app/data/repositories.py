from typing import Generic, Optional, TypeVar

from sqlalchemy import delete, desc, select
from sqlalchemy.ext.asyncio import AsyncSession

from .database import Base
from .models import ChatMessageModel, LocationModel, NotificationEventModel, TripModel

ModelType = TypeVar("ModelType", bound=Base)


class BaseRepository(Generic[ModelType]):
    """基础仓储抽象类"""

    model: type[ModelType]

    def __init__(self, session: AsyncSession):
        self.session = session

    async def get_by_id(self, entity_id: int) -> Optional[ModelType]:
        result = await self.session.execute(
            select(self.model).where(self.model.id == entity_id)
        )
        return result.scalar_one_or_none()

    async def create(self, instance: ModelType) -> ModelType:
        self.session.add(instance)
        await self.session.commit()
        await self.session.refresh(instance)
        return instance

    async def update(self, instance: ModelType) -> ModelType:
        await self.session.commit()
        await self.session.refresh(instance)
        return instance

    async def delete(self, entity_id: int) -> bool:
        result = await self.session.execute(
            delete(self.model).where(self.model.id == entity_id)
        )
        await self.session.commit()
        return bool(result.rowcount)


class TripRepository(BaseRepository[TripModel]):
    model = TripModel


class LocationRepository(BaseRepository[LocationModel]):
    model = LocationModel

    async def get_latest_for_trip(self, trip_id: int) -> Optional[LocationModel]:
        result = await self.session.execute(
            select(LocationModel)
            .where(LocationModel.trip_id == trip_id)
            .order_by(desc(LocationModel.recorded_at), desc(LocationModel.id))
            .limit(1)
        )
        return result.scalar_one_or_none()


class ChatMessageRepository(BaseRepository[ChatMessageModel]):
    model = ChatMessageModel


class NotificationEventRepository(BaseRepository[NotificationEventModel]):
    model = NotificationEventModel

    async def get_by_trip_and_type(
        self,
        trip_id: int,
        event_type: str,
    ) -> Optional[NotificationEventModel]:
        result = await self.session.execute(
            select(NotificationEventModel)
            .where(NotificationEventModel.trip_id == trip_id)
            .where(NotificationEventModel.type == event_type)
            .order_by(desc(NotificationEventModel.id))
            .limit(1)
        )
        return result.scalar_one_or_none()

    async def list_events(
        self,
        trip_id: Optional[int] = None,
        user_id: Optional[int] = None,
    ) -> list[NotificationEventModel]:
        stmt = select(NotificationEventModel).order_by(
            desc(NotificationEventModel.created_at),
            desc(NotificationEventModel.id),
        )
        if trip_id is not None:
            stmt = stmt.where(NotificationEventModel.trip_id == trip_id)
        if user_id is not None:
            stmt = stmt.where(NotificationEventModel.user_id == user_id)

        result = await self.session.execute(stmt)
        return list(result.scalars().all())
