from datetime import datetime
from typing import get_type_hints

import pytest
from pydantic import ValidationError

from app.api.routers import api_router
from app.models.domain import Emergency, EmergencyCreate, Trip, TripCreate


def _assert_user_id_extra_forbidden(error: ValidationError) -> None:
    errors = error.errors()
    assert len(errors) == 1

    detail = errors[0]
    assert tuple(detail.get("loc", ())) == ("user_id",)
    assert detail.get("type") in {"extra_forbidden", "value_error.extra"}


def _route_paths() -> set[str]:
    return {route.path for route in api_router.routes if hasattr(route, "path")}


def test_trip_create_no_user_id_input_contract() -> None:
    assert "user_id" not in TripCreate.model_fields

    with pytest.raises(ValidationError) as exc_info:
        TripCreate.model_validate(
            {
                "user_id": "legacy-user-id",
                "mode": "walking",
                "start_latitude": 1.0,
                "start_longitude": 2.0,
                "destination_latitude": 3.0,
                "destination_longitude": 4.0,
                "planned_arrival_at": datetime(2026, 1, 1, 12, 0, 0),
                "planned_path": [{"latitude": 1.1, "longitude": 2.2}],
            }
        )
    _assert_user_id_extra_forbidden(exc_info.value)


def test_emergency_create_no_user_id_input_contract() -> None:
    assert "user_id" not in EmergencyCreate.model_fields

    with pytest.raises(ValidationError) as exc_info:
        EmergencyCreate.model_validate(
            {
                "user_id": "legacy-user-id",
                "type": "sos",
                "latitude": 1.0,
                "longitude": 2.0,
            }
        )
    _assert_user_id_extra_forbidden(exc_info.value)


def test_trip_user_id_annotation_is_str() -> None:
    assert get_type_hints(Trip)["user_id"] is str


def test_emergency_user_id_annotation_is_str() -> None:
    assert get_type_hints(Emergency)["user_id"] is str


def test_api_router_no_users_routes() -> None:
    route_paths = _route_paths()
    assert not any(path.startswith("/users") for path in route_paths)


def test_api_router_includes_migrated_route_prefixes() -> None:
    route_paths = _route_paths()

    assert any(path.startswith("/guardians") for path in route_paths)
    assert any(path.startswith("/chats") for path in route_paths)
    assert any(path.startswith("/trip-events") for path in route_paths)
