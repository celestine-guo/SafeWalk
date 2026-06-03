from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.ext.asyncio import AsyncSession

from backend.app.api.auth import CurrentUser, require_current_user
from backend.app.data.database import get_db
from backend.app.models.domain import ChatMessage, ChatRequest
from backend.app.services.impl import ChatService

router = APIRouter(prefix="/chats", tags=["chats"])


def _to_chat(chat_model) -> ChatMessage:
    return ChatMessage(
        id=chat_model.id,
        trip_id=chat_model.trip_id,
        user_id=chat_model.user_id,
        role=chat_model.role,
        content=chat_model.content,
        provider=chat_model.provider,
        created_at=chat_model.created_at,
        updated_at=chat_model.updated_at,
    )


@router.post("/", response_model=ChatMessage)
async def create_chat_message(
    chat: ChatRequest,
    current_user: CurrentUser = Depends(require_current_user),
    db: AsyncSession = Depends(get_db),
):
    service = ChatService(db)
    chat_model = await service.create_user_message(current_user.clerk_user_id, chat)
    if chat_model is None:
        raise HTTPException(status_code=404, detail="Trip not found")
    return _to_chat(chat_model)


@router.get("/trip/{trip_id}", response_model=list[ChatMessage])
async def list_chat_messages(
    trip_id: int,
    current_user: CurrentUser = Depends(require_current_user),
    db: AsyncSession = Depends(get_db),
):
    service = ChatService(db)
    chat_models = await service.list_trip_messages(current_user.clerk_user_id, trip_id)
    if chat_models is None:
        raise HTTPException(status_code=404, detail="Trip not found")
    return [_to_chat(item) for item in chat_models]
