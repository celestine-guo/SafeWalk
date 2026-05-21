from typing import Any

from clerk_backend_api import Clerk
from clerk_backend_api.security.types import AuthenticateRequestOptions
from fastapi import HTTPException, Request, status
from pydantic import BaseModel

from backend.config.settings import settings


class CurrentUser(BaseModel):
    clerk_user_id: str


def _build_auth_options() -> AuthenticateRequestOptions:
    options: dict[str, Any] = {"accepts_token": ["session_token"]}
    if settings.CLERK_AUTHORIZED_PARTIES:
        options["authorized_parties"] = settings.CLERK_AUTHORIZED_PARTIES
    if settings.CLERK_JWT_KEY:
        options["jwt_key"] = settings.CLERK_JWT_KEY
    return AuthenticateRequestOptions(**options)


async def require_current_user(request: Request) -> CurrentUser:
    if not settings.CLERK_SECRET_KEY:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="Clerk is not configured",
        )

    clerk = Clerk(bearer_auth=settings.CLERK_SECRET_KEY)

    try:
        request_state = clerk.authenticate_request(
            request,
            _build_auth_options(),
        )
    except Exception as exc:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Unauthorized",
        ) from exc

    payload = request_state.payload or {}
    user_id = payload.get("sub")
    if not request_state.is_signed_in or not user_id:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Unauthorized",
        )

    return CurrentUser(clerk_user_id=user_id)
