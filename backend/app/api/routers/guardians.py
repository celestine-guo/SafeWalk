from fastapi import APIRouter, Depends
from sqlalchemy.ext.asyncio import AsyncSession

from backend.app.api.auth import CurrentUser, require_current_user
from backend.app.data.database import get_db
from backend.app.models.domain import Guardian, GuardianCreate
from backend.app.services.impl import GuardianService

router = APIRouter(prefix="/guardians", tags=["guardians"])


def _to_guardian(guardian_model) -> Guardian:
    return Guardian(
        id=guardian_model.id,
        user_id=guardian_model.user_id,
        guardian_name=guardian_model.guardian_name,
        guardian_phone=guardian_model.guardian_phone,
        relationship=guardian_model.relationship,
        is_primary=guardian_model.is_primary,
        note=guardian_model.note,
        created_at=guardian_model.created_at,
        updated_at=guardian_model.updated_at,
    )


@router.post("/", response_model=Guardian)
async def create_guardian(
    guardian: GuardianCreate,
    current_user: CurrentUser = Depends(require_current_user),
    db: AsyncSession = Depends(get_db),
):
    service = GuardianService(db)
    guardian_model = await service.create_guardian(current_user.clerk_user_id, guardian)
    return _to_guardian(guardian_model)


@router.get("/", response_model=list[Guardian])
async def list_guardians(
    current_user: CurrentUser = Depends(require_current_user),
    db: AsyncSession = Depends(get_db),
):
    service = GuardianService(db)
    guardian_models = await service.list_guardians(current_user.clerk_user_id)
    return [_to_guardian(item) for item in guardian_models]
