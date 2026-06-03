from pydantic import BaseModel, ConfigDict
from datetime import datetime
from typing import Optional


class BaseSchema(BaseModel):
    """基础模型类"""
    model_config = ConfigDict(
        from_attributes=True,
        populate_by_name=True,
        extra="forbid",
    )


class TimestampSchema(BaseSchema):
    """带时间戳的基础模型"""
    created_at: datetime
    updated_at: datetime


class IDTimestampSchema(TimestampSchema):
    """带ID和时间戳的基础模型"""
    id: int
