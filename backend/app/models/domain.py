from datetime import datetime
from enum import Enum
from typing import Any, Optional

from pydantic import Field

from .base import BaseSchema, IDTimestampSchema


class TripMode(str, Enum):
    WALKING = "walking"
    TAXI = "taxi"


class TripStatus(str, Enum):
    ACTIVE = "active"
    ARRIVED = "arrived"
    SOS = "sos"
    CANCELLED = "cancelled"
    TIMEOUT = "timeout"


class NotificationType(str, Enum):
    TRIP_STARTED = "trip_started"
    TRIP_TIMEOUT = "trip_timeout"
    TRIP_DEVIATED = "trip_deviated"
    SOS = "sos"
    ARRIVED = "arrived"


class NotificationStatus(str, Enum):
    PENDING = "pending"
    GENERATED = "generated"


class EmergencyType(str, Enum):
    SOS = "sos"
    MANUAL_ALERT = "manual_alert"


class EmergencyStatus(str, Enum):
    ACTIVE = "active"
    RESOLVED = "resolved"


class ChatRole(str, Enum):
    USER = "user"
    ASSISTANT = "assistant"


class PathPoint(BaseSchema):
    latitude: float
    longitude: float


class TripCreate(BaseSchema):
    mode: TripMode
    start_latitude: float
    start_longitude: float
    destination_latitude: float
    destination_longitude: float
    planned_arrival_at: datetime
    planned_path: list[PathPoint]
    deviation_threshold_meters: Optional[float] = None
    timeout_grace_minutes: Optional[int] = None


class Trip(IDTimestampSchema):
    user_id: str
    mode: TripMode
    status: TripStatus
    start_latitude: float
    start_longitude: float
    destination_latitude: float
    destination_longitude: float
    planned_arrival_at: datetime
    deviation_threshold_meters: float
    timeout_grace_minutes: int
    planned_path: list[PathPoint] = Field(alias="planned_path_json")
    started_at: datetime
    arrived_at: Optional[datetime] = None


class LocationCreate(BaseSchema):
    trip_id: int
    latitude: float
    longitude: float
    accuracy: Optional[float] = None
    recorded_at: datetime


class Location(IDTimestampSchema):
    trip_id: int
    latitude: float
    longitude: float
    accuracy: Optional[float] = None
    recorded_at: datetime


class ChatMessage(IDTimestampSchema):
    trip_id: int
    user_id: str
    role: ChatRole
    content: str
    provider: str


class ChatRequest(BaseSchema):
    trip_id: int
    message: str


class ChatResponse(BaseSchema):
    trip_id: int
    user_message: ChatMessage
    assistant_message: ChatMessage


class NotificationEvent(IDTimestampSchema):
    trip_id: int
    user_id: str
    type: NotificationType
    title: str
    body: str
    status: NotificationStatus
    payload: Optional[dict[str, Any]] = Field(default=None, alias="payload_json")


class TripDetail(BaseSchema):
    trip: Trip
    latest_location: Optional[Location] = None
    notifications: list[NotificationEvent] = Field(default_factory=list)


class EmergencyCreate(BaseSchema):
    type: EmergencyType
    status: EmergencyStatus = EmergencyStatus.ACTIVE
    latitude: float
    longitude: float


class EmergencyUpdate(BaseSchema):
    status: Optional[EmergencyStatus] = None
    resolved_at: Optional[datetime] = None


class Emergency(IDTimestampSchema):
    user_id: str
    type: EmergencyType
    status: EmergencyStatus
    latitude: float
    longitude: float
    timestamp: datetime
    resolved_at: Optional[datetime] = None


class GuardianCreate(BaseSchema):
    guardian_name: str
    guardian_phone: str
    relationship: str
    is_primary: bool = False
    note: Optional[str] = None


class Guardian(IDTimestampSchema):
    user_id: str
    guardian_name: str
    guardian_phone: str
    relationship: str
    is_primary: bool
    note: Optional[str] = None


class TripEventType(str, Enum):
    TRIP_STARTED = "trip_started"
    LOCATION_UPDATE = "location_update"
    TRIP_DEVIATED = "trip_deviated"
    SOS = "sos"
    ARRIVED = "arrived"
    TRIP_TIMEOUT = "trip_timeout"
    CHAT = "chat"
    SYSTEM = "system"


class TripEventCreate(BaseSchema):
    trip_id: int
    event_type: TripEventType
    title: str
    content: str
    payload: Optional[dict[str, Any]] = None


class TripEvent(IDTimestampSchema):
    trip_id: int
    user_id: str
    event_type: TripEventType
    title: str
    content: str
    payload: Optional[dict[str, Any]] = Field(default=None, alias="payload_json")
