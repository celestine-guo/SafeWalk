# SafeWalk Clerk Authentication Design

## Goal

SafeWalk will replace local backend user registration and JWT responsibility with Clerk-managed authentication. Android will use Clerk's prebuilt authentication UI, and the backend will trust only Clerk-verified session tokens for business requests.

## Confirmed Decisions

- Use Clerk's prebuilt Android `AuthView` for sign-in and sign-up.
- Require sign-in before showing the SafeWalk main screen.
- Do not keep a local `users` table or local user-id mapping.
- Store Clerk `user_id` strings directly in business tables.
- Do not implement a database migration script in the first phase.
- Upgrade the backend runtime requirement to Python 3.10+ and use Clerk's official Python backend SDK.
- Include first-phase ownership checks so users can only access their own trips and trip locations.

## Current Context

Android currently has no real authentication entry point. `MapActivity` creates trips with a hard-coded `DEMO_USER_ID = 1`, and `SafeWalkBackendClient` sends JSON requests without an `Authorization` header.

The backend currently exposes `/api/users/*`, but the implementation stores `UserCreate.password` directly in `password_hash`. Backend JWT is not actually implemented; `SECRET_KEY`, `ALGORITHM`, and `ACCESS_TOKEN_EXPIRE_MINUTES` exist in settings, while `backend/README.md` still lists JWT authentication as pending.

Existing business records use integer `user_id` fields in trips, emergencies, chat messages, and notification events. The first Clerk integration will change new code to string Clerk IDs but will not migrate existing database files.

## Architecture

Clerk becomes the single source of truth for authentication. The Android app initializes Clerk at startup, observes Clerk initialization and user state, and conditionally renders either a loading state, Clerk `AuthView`, or the existing SafeWalk experience.

The backend adds a reusable FastAPI dependency, tentatively named `require_current_user`, that validates the incoming Clerk session token and returns the authenticated Clerk user ID. Business routes depend on that value instead of accepting `user_id` from the client.

Local backend user registration, password storage, and JWT configuration are removed from the active backend API surface.

## Android Design

`SafeWalkApplication` will initialize both AMap and Clerk. The Clerk publishable key will be injected through Gradle as `BuildConfig.CLERK_PUBLISHABLE_KEY`, using a Gradle property or environment variable. It will not be hard-coded in Kotlin source.

`MainActivity` will render based on Clerk state:

- Loading while Clerk is initializing.
- Clerk prebuilt `AuthView` while signed out.
- Existing `SafeWalkScreen` while signed in.

`SafeWalkBackendClient` will become authentication-aware. Before sending a protected backend request, it will obtain the current Clerk session token and attach:

```http
Authorization: Bearer <clerk_session_token>
```

If the user is unexpectedly signed out or token retrieval fails, client calls will return a `BackendResult.Error` with Chinese user-facing copy such as `登录状态已失效，请重新登录`.

`TripCreatePayload` will stop carrying `userId`. Any future emergency or chat request payloads will also omit user identity supplied by the client.

Gradle will be updated to satisfy Clerk Android SDK requirements:

- Keep `minSdk = 24`.
- Change Java source and target compatibility to Java 17.
- Add Clerk Android UI dependency and supporting lifecycle Compose dependency.

## Backend Design

The backend runtime requirement becomes Python 3.10+. Dependencies will include Clerk's official backend SDK for Python.

Backend settings will add:

- `CLERK_SECRET_KEY`
- `CLERK_PUBLISHABLE_KEY`
- `CLERK_JWT_KEY` as optional networkless verification support
- `CLERK_AUTHORIZED_PARTIES` for allowed token origins when applicable

The local JWT settings will be removed from the active configuration unless another non-auth feature still needs them:

- `SECRET_KEY`
- `ALGORITHM`
- `ACCESS_TOKEN_EXPIRE_MINUTES`

A new authentication module will expose a small authenticated-user type, for example:

```python
class CurrentUser(BaseModel):
    clerk_user_id: str
```

The authentication dependency will:

- Read `Authorization: Bearer ...`.
- Ask Clerk's SDK to authenticate the request as a session token.
- Return `CurrentUser(clerk_user_id=...)` on success.
- Raise `HTTPException(status_code=401, detail="Unauthorized")` for missing, invalid, or expired credentials.

## API Contract Changes

The following create requests will no longer accept client-supplied user identity:

- `POST /api/trips/`
- `POST /api/emergencies/`
- Future `POST /api/chat` or equivalent chat endpoints

`TripCreate` removes `user_id`. `EmergencyCreate` removes `user_id`. `ChatRequest` removes `user_id` when chat routing is active.

Response models may continue returning `user_id`, but the type becomes `str` and the value is the Clerk user ID.

The `/api/users/*` router is removed from the main router. Backend user profile data should come from Clerk or from future dedicated profile records that are keyed by Clerk `user_id`, not from password-bearing local user records.

## Data Model Changes

The following fields change from integer IDs to Clerk user ID strings:

- `TripModel.user_id`
- `EmergencyModel.user_id`
- `ChatMessageModel.user_id`
- `NotificationEventModel.user_id`
- Corresponding Pydantic domain models

The local user model and service are removed from the active path:

- `UserModel`
- `UserCreate`
- `UserUpdate`
- `User`
- `UserService`
- `UserRepository`
- `/api/users/*`

Because the first phase does not include migration, an existing local `safewalk.db` may still contain integer columns and old tables. If that causes verification failures, the manual development action is to remove or recreate the local database before rerunning the app. This is an explicit first-phase assumption, not hidden migration work.

## Authorization Rules

All business routes that read or write user-owned resources require Clerk authentication.

For first-phase ownership checks:

- Creating a trip writes the authenticated Clerk `user_id`.
- Reading trip detail verifies the trip belongs to the authenticated Clerk `user_id`.
- Listing trip locations verifies the parent trip belongs to the authenticated Clerk `user_id`.
- Creating emergencies writes the authenticated Clerk `user_id`.
- Updating emergencies should only allow records owned by the authenticated Clerk `user_id`.

For resources owned by another user, the preferred response is `404 Not Found` to avoid leaking whether another user's resource exists.

## Error Handling

Android:

- Signed-out users see Clerk `AuthView` instead of business UI.
- Token retrieval failures return a Chinese `BackendResult.Error`.
- Backend `401` responses should be displayed as an expired-login style message, not a raw JSON body.

Backend:

- Missing `Authorization` header returns `401 Unauthorized`.
- Invalid, expired, or unverifiable Clerk token returns `401 Unauthorized`.
- Authenticated access to a missing resource returns the existing `404`.
- Authenticated access to another user's resource returns `404`.
- Missing Clerk environment configuration should fail loudly during startup or first authenticated request with a readable server-side log.

## Testing Strategy

Android verification:

- Run `./gradlew assembleDebug`.
- Adjust Compose tests so they can render the signed-in main screen without requiring real Clerk login.
- Add focused client tests where practical for header injection and missing-token error handling.

Backend verification:

- Add tests that override the authentication dependency with a mock Clerk user.
- Verify unauthenticated protected routes return `401`.
- Verify `POST /api/trips/` ignores any client `user_id` field and uses the authenticated Clerk ID.
- Verify trip detail for another user's trip returns `404`.
- Verify trip locations for another user's trip return `404`.
- Verify `/api/users/*` is no longer registered.
- Run an import or compile check such as `python -m compileall backend` when full test dependencies are unavailable.

## External References

- Clerk Android Quickstart: https://clerk.com/docs/android/getting-started/quickstart
- Clerk Android View Reference: https://clerk.com/docs/android/reference/views/overview
- Clerk Manual JWT Verification: https://clerk.com/docs/guides/sessions/manual-jwt-verification
- Clerk Python backend article: https://clerk.com/articles/how-to-add-authentication-to-a-python-backend

## Out of Scope

- Migrating existing local database rows.
- Designing a custom SafeWalk-branded login screen.
- Implementing custom local password registration.
- Supporting guest mode.
- Adding organization or multi-tenant Clerk features.
- Adding new user profile or emergency-contact management beyond replacing authentication identity.
