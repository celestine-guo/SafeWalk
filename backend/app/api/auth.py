from typing import Any

from clerk_backend_api import Clerk
from clerk_backend_api.security.types import (
    AuthErrorReason,
    AuthenticateRequestOptions,
    TokenVerificationErrorReason,
)
from fastapi import HTTPException, Request, status
from pydantic import BaseModel

from backend.config.settings import settings


class CurrentUser(BaseModel):
    clerk_user_id: str


SERVER_AUTH_REASONS = {
    AuthErrorReason.SECRET_KEY_MISSING,
    TokenVerificationErrorReason.SECRET_KEY_MISSING,
    TokenVerificationErrorReason.SERVER_ERROR,
    TokenVerificationErrorReason.JWK_FAILED_TO_LOAD,
    TokenVerificationErrorReason.JWK_REMOTE_INVALID,
    TokenVerificationErrorReason.JWK_FAILED_TO_RESOLVE,
}


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
        request_state = await clerk.authenticate_request_async(
            request,
            _build_auth_options(),
        )
    except Exception as exc:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="Authentication service error",
        ) from exc

    if request_state.reason in SERVER_AUTH_REASONS:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="Authentication service error",
        )

    payload = request_state.payload or {}
    user_id = payload.get("sub")
    if not request_state.is_signed_in or not isinstance(user_id, str) or not user_id:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Unauthorized",
        )

    return CurrentUser(clerk_user_id=user_id)
