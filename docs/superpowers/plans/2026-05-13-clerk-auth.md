# Clerk Authentication Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace SafeWalk's local user/JWT path with Clerk-managed Android sign-in and Clerk-verified backend requests.

**Architecture:** Android initializes Clerk, shows Clerk `AuthView` until signed in, and sends Clerk session tokens to the backend. FastAPI verifies every protected request through Clerk's Python SDK, derives the Clerk `user_id` server-side, and stores that string on trips, locations, emergencies, chats, and notifications. Local `/api/users/*`, local passwords, and backend JWT settings leave the active API surface.

**Tech Stack:** Android Kotlin, Jetpack Compose, Clerk Android UI/API `1.0.16`, FastAPI, SQLAlchemy async, Pydantic v2, `clerk-backend-api==5.0.6`, Python 3.10+.

---

## File Structure

- Modify `gradle/libs.versions.toml`: add Clerk Android UI `1.0.16` and lifecycle Compose `2.9.2`.
- Modify `app/build.gradle.kts`: add `CLERK_PUBLISHABLE_KEY`, Java 17, and Clerk dependencies.
- Modify `app/src/main/java/com/celestine/safewalk/SafeWalkApplication.kt`: initialize Clerk.
- Create `app/src/main/java/com/celestine/safewalk/AuthGate.kt`: render loading, Clerk auth, and signed-in content.
- Modify `app/src/main/java/com/celestine/safewalk/MainActivity.kt`: route through `AuthGate`.
- Create `app/src/main/java/com/celestine/safewalk/AuthTokenProvider.kt`: small wrapper for Clerk token retrieval.
- Modify `app/src/main/java/com/celestine/safewalk/SafeWalkBackendClient.kt`: add bearer token injection and remove client-supplied `userId`.
- Modify `app/src/main/java/com/celestine/safewalk/MapActivity.kt`: remove `DEMO_USER_ID` usage.
- Modify `app/src/main/res/values/strings.xml`: add login/loading/auth-expired strings.
- Modify `app/src/androidTest/java/com/celestine/safewalk/MainActivityTest.kt`: continue testing signed-in content without real Clerk login.
- Modify `backend/requirements.txt` and `backend/pyproject.toml`: Python 3.10+, Clerk SDK, pytest tooling.
- Modify `backend/config/settings.py`: Clerk settings, remove active local JWT settings.
- Modify `backend/.env.example`: Clerk environment variables.
- Create `backend/app/api/auth.py`: Clerk request authentication dependency.
- Modify `backend/app/models/domain.py`: remove user schemas and switch user IDs to `str`.
- Modify `backend/app/data/models.py`: remove `UserModel`; switch user ID columns to `String`.
- Modify `backend/app/services/impl.py`: remove user service/repository, accept authenticated user IDs in business methods.
- Modify `backend/app/data/repositories.py`: add ownership lookup helpers.
- Modify `backend/app/api/routers/__init__.py`: remove users router.
- Delete `backend/app/api/routers/users.py`: local user API no longer exists.
- Modify `backend/app/api/routers/trips.py`, `locations.py`, `emergencies.py`: require auth and enforce ownership.
- Configure Clerk Dashboard Android application: add SafeWalk package name, debug SHA-256, and verify `assetlinks.json`.
- Modify `backend/README.md`: document Clerk setup, Python 3.10+, and old DB caveat.
- Create `backend/tests/test_clerk_auth_contract.py`: backend contract tests with auth dependency overrides.

## Task 1: Android Clerk Dependency And Build Configuration

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `app/build.gradle.kts`

- [ ] **Step 1: Add Clerk and lifecycle versions to the version catalog**

Update `gradle/libs.versions.toml` with these entries:

```toml
[versions]
clerkAndroid = "1.0.16"
lifecycle = "2.9.2"

[libraries]
clerk-android-ui = { group = "com.clerk", name = "clerk-android-ui", version.ref = "clerkAndroid" }
androidx-lifecycle-viewmodel-compose = { group = "androidx.lifecycle", name = "lifecycle-viewmodel-compose", version.ref = "lifecycle" }
androidx-lifecycle-runtime-compose = { group = "androidx.lifecycle", name = "lifecycle-runtime-compose", version.ref = "lifecycle" }
```

Keep the existing entries and insert these without renaming current aliases.

- [ ] **Step 2: Add Clerk publishable key provider and Java 17**

In `app/build.gradle.kts`, add this provider near the existing `amapApiKeyProvider` and `backendBaseUrlProvider`:

```kotlin
val clerkPublishableKeyProvider = providers.gradleProperty("CLERK_PUBLISHABLE_KEY")
    .orElse(providers.environmentVariable("CLERK_PUBLISHABLE_KEY"))
    .orElse("")
```

In `defaultConfig`, add:

```kotlin
buildConfigField(
    "String",
    "CLERK_PUBLISHABLE_KEY",
    "\"${clerkPublishableKeyProvider.get()}\""
)
```

Change `compileOptions` to:

```kotlin
compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}
```

- [ ] **Step 3: Add Android dependencies**

In `app/build.gradle.kts`, add:

```kotlin
implementation(libs.clerk.android.ui)
implementation(libs.androidx.lifecycle.viewmodel.compose)
implementation(libs.androidx.lifecycle.runtime.compose)
```

- [ ] **Step 4: Verify Gradle configuration**

Run:

```bash
./gradlew :app:dependencies --configuration debugRuntimeClasspath
```

Expected: command resolves `com.clerk:clerk-android-ui:1.0.16` and exits successfully.

- [ ] **Step 5: Commit**

```bash
git add gradle/libs.versions.toml app/build.gradle.kts
git commit -m "build: add clerk android dependencies"
```

## Task 2: Android Auth Gate

**Files:**
- Modify: `app/src/main/java/com/celestine/safewalk/SafeWalkApplication.kt`
- Create: `app/src/main/java/com/celestine/safewalk/AuthGate.kt`
- Modify: `app/src/main/java/com/celestine/safewalk/MainActivity.kt`
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/androidTest/java/com/celestine/safewalk/MainActivityTest.kt`

- [ ] **Step 1: Add user-facing strings**

In `app/src/main/res/values/strings.xml`, add:

```xml
<!-- 登录状态 -->
<string name="auth_loading">正在检查登录状态…</string>
<string name="auth_missing_key">请先配置 CLERK_PUBLISHABLE_KEY</string>
```

- [ ] **Step 2: Initialize Clerk in the application class**

Update `SafeWalkApplication.kt`:

```kotlin
package com.celestine.safewalk

import android.app.Application
import com.amap.api.maps.MapsInitializer
import com.clerk.api.Clerk

class SafeWalkApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        MapsInitializer.updatePrivacyShow(this, true, true)
        MapsInitializer.updatePrivacyAgree(this, true)
        MapsInitializer.initialize(this)

        if (BuildConfig.CLERK_PUBLISHABLE_KEY.isNotBlank()) {
            Clerk.initialize(
                this,
                publishableKey = BuildConfig.CLERK_PUBLISHABLE_KEY
            )
        }
    }
}
```

- [ ] **Step 3: Create AuthGate**

Create `app/src/main/java/com/celestine/safewalk/AuthGate.kt`:

```kotlin
package com.celestine.safewalk

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.clerk.api.Clerk
import com.clerk.ui.auth.AuthView

@Composable
fun AuthGate(
    signedInContent: @Composable () -> Unit,
) {
    if (BuildConfig.CLERK_PUBLISHABLE_KEY.isBlank()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(text = stringResource(id = R.string.auth_missing_key))
        }
        return
    }

    val isInitialized by Clerk.isInitialized.collectAsStateWithLifecycle(initialValue = false)
    val user by Clerk.userFlow.collectAsStateWithLifecycle(initialValue = null)

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        when {
            !isInitialized -> CircularProgressIndicator()
            user == null -> AuthView()
            else -> signedInContent()
        }
    }
}
```

- [ ] **Step 4: Route MainActivity through AuthGate**

Update `MainActivity.onCreate` so `SafeWalkScreen` is inside `AuthGate`:

```kotlin
setContent {
    SafeWalkApp {
        AuthGate {
            SafeWalkScreen(
                onWalkingModeSelected = {
                    startActivity(MapActivity.createIntent(this, Mode.WALKING))
                }
            )
        }
    }
}
```

- [ ] **Step 5: Keep existing Compose tests focused on signed-in content**

Do not launch `MainActivity` directly in `MainActivityTest`. Keep the current `launchContent()` helper rendering `SafeWalkScreen` directly:

```kotlin
private fun launchContent(onWalkingModeSelected: () -> Unit = {}) {
    composeRule.setContent {
        SafeWalkApp {
            SafeWalkScreen(onWalkingModeSelected = onWalkingModeSelected)
        }
    }
}
```

- [ ] **Step 6: Run Android instrumentation tests for existing screens**

Run:

```bash
./gradlew connectedDebugAndroidTest
```

Expected: existing `MainActivityTest` and `MapScreenTest` pass on an available emulator/device. If no emulator is available, record the environment blocker and run `./gradlew assembleDebug` in Task 3.

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/com/celestine/safewalk/SafeWalkApplication.kt app/src/main/java/com/celestine/safewalk/AuthGate.kt app/src/main/java/com/celestine/safewalk/MainActivity.kt app/src/main/res/values/strings.xml app/src/androidTest/java/com/celestine/safewalk/MainActivityTest.kt
git commit -m "feat: gate android app with clerk auth"
```

## Task 3: Android Backend Client Uses Clerk Token

**Files:**
- Create: `app/src/main/java/com/celestine/safewalk/AuthTokenProvider.kt`
- Modify: `app/src/main/java/com/celestine/safewalk/SafeWalkBackendClient.kt`
- Modify: `app/src/main/java/com/celestine/safewalk/MapActivity.kt`
- Modify: `app/src/main/res/values/strings.xml`

- [ ] **Step 1: Add expired-login string**

In `strings.xml`, add:

```xml
<string name="auth_session_expired">登录状态已失效，请重新登录</string>
```

- [ ] **Step 2: Add a Clerk token provider**

Create `AuthTokenProvider.kt`:

```kotlin
package com.celestine.safewalk

import com.clerk.api.Clerk
import kotlinx.coroutines.runBlocking

interface AuthTokenProvider {
    fun getToken(): String?
}

class ClerkAuthTokenProvider : AuthTokenProvider {
    override fun getToken(): String? = runBlocking {
        Clerk.auth.getToken()
    }
}
```

- [ ] **Step 3: Remove userId from trip payload and inject token**

Change `TripCreatePayload` in `SafeWalkBackendClient.kt`:

```kotlin
data class TripCreatePayload(
    val mode: String,
    val startLatitude: Double,
    val startLongitude: Double,
    val destinationLatitude: Double,
    val destinationLongitude: Double,
    val plannedArrivalAt: String,
    val plannedPath: List<PathPointPayload>,
    val deviationThresholdMeters: Double,
    val timeoutGraceMinutes: Int,
)
```

Change the client constructor:

```kotlin
class SafeWalkBackendClient(
    private val executor: ExecutorService = Executors.newSingleThreadExecutor(),
    private val authTokenProvider: AuthTokenProvider = ClerkAuthTokenProvider(),
) {
```

Remove this line from the trip body:

```kotlin
put("user_id", payload.userId)
```

Update `request` to require a token:

```kotlin
private fun request(path: String, method: String, body: String? = null): String {
    val token = authTokenProvider.getToken()
        ?: throw IllegalStateException("登录状态已失效，请重新登录")

    val connection = (URL("$baseUrl$path").openConnection() as HttpURLConnection).apply {
        requestMethod = method
        connectTimeout = 10_000
        readTimeout = 10_000
        setRequestProperty("Content-Type", "application/json; charset=utf-8")
        setRequestProperty("Accept", "application/json")
        setRequestProperty("Authorization", "Bearer $token")
        doInput = true
        doOutput = body != null
    }
```

Keep the existing response handling after the `connection` block.

- [ ] **Step 4: Remove demo user ID from MapActivity**

In `MapActivity.startGuarding()`, remove the `userId = DEMO_USER_ID` argument:

```kotlin
TripCreatePayload(
    mode = Mode.WALKING.name.lowercase(),
    startLatitude = start.latitude,
    startLongitude = start.longitude,
    destinationLatitude = end.latitude,
    destinationLongitude = end.longitude,
    plannedArrivalAt = OffsetDateTime.now(ZoneOffset.UTC)
        .plusSeconds(routeDurationSeconds.toLong())
        .toString(),
    plannedPath = routePolylinePoints.map { point ->
        PathPointPayload(
            latitude = point.latitude,
            longitude = point.longitude,
        )
    },
    deviationThresholdMeters = DEFAULT_DEVIATION_THRESHOLD_METERS,
    timeoutGraceMinutes = DEFAULT_TIMEOUT_GRACE_MINUTES,
)
```

Delete:

```kotlin
private const val DEMO_USER_ID = 1
```

- [ ] **Step 5: Compile Android**

Run:

```bash
./gradlew assembleDebug
```

Expected: app compiles and Clerk classes resolve.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/celestine/safewalk/AuthTokenProvider.kt app/src/main/java/com/celestine/safewalk/SafeWalkBackendClient.kt app/src/main/java/com/celestine/safewalk/MapActivity.kt app/src/main/res/values/strings.xml
git commit -m "feat: send clerk tokens to backend"
```

## Task 4: Backend Dependencies And Settings

**Files:**
- Modify: `backend/requirements.txt`
- Modify: `backend/pyproject.toml`
- Modify: `backend/config/settings.py`
- Modify: `backend/.env.example`

- [ ] **Step 1: Update backend requirements**

Update `backend/requirements.txt` to include:

```text
clerk-backend-api==5.0.6
pytest==8.3.5
pytest-asyncio==0.25.3
httpx==0.28.1
```

Keep the existing dependencies.

- [ ] **Step 2: Update pyproject runtime and dependencies**

Change:

```toml
requires-python = ">=3.10"
```

Add these strings to `[project].dependencies`:

```toml
"clerk-backend-api==5.0.6",
"pytest==8.3.5",
"pytest-asyncio==0.25.3",
"httpx==0.28.1",
```

- [ ] **Step 3: Update settings**

In `backend/config/settings.py`, remove the local JWT settings:

```python
SECRET_KEY: str = "dev-secret-key"
ALGORITHM: str = "HS256"
ACCESS_TOKEN_EXPIRE_MINUTES: int = 30
```

Add Clerk settings:

```python
# Clerk认证配置
CLERK_SECRET_KEY: str = ""
CLERK_PUBLISHABLE_KEY: str = ""
CLERK_JWT_KEY: str = ""
CLERK_AUTHORIZED_PARTIES: list[str] = []
```

- [ ] **Step 4: Update env example**

In `backend/.env.example`, replace the security section with:

```dotenv
# Clerk认证配置
CLERK_SECRET_KEY=sk_test_replace_me
CLERK_PUBLISHABLE_KEY=pk_test_replace_me
CLERK_JWT_KEY=
CLERK_AUTHORIZED_PARTIES=[]
```

- [ ] **Step 5: Verify dependency metadata**

Run:

```bash
python3 -m compileall backend/config
```

Expected: settings compile successfully under Python 3.10+.

- [ ] **Step 6: Commit**

```bash
git add backend/requirements.txt backend/pyproject.toml backend/config/settings.py backend/.env.example
git commit -m "build: add clerk backend settings"
```

## Task 5: Backend Authentication Dependency

**Files:**
- Create: `backend/app/api/auth.py`

- [ ] **Step 1: Create auth dependency**

Create `backend/app/api/auth.py`:

```python
from typing import Any

from clerk_backend_api import Clerk
from clerk_backend_api.security.types import AuthenticateRequestOptions
from fastapi import HTTPException, Request, status
from pydantic import BaseModel

from backend.config.settings import settings


class CurrentUser(BaseModel):
    clerk_user_id: str


def _build_auth_options() -> AuthenticateRequestOptions:
    options: dict[str, Any] = {
        "accepts_token": ["session_token"],
    }
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
    request_state = clerk.authenticate_request(
        request,
        _build_auth_options(),
    )

    if not request_state.is_signed_in:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Unauthorized",
        )

    payload = request_state.payload or {}
    user_id = payload.get("sub")
    if not isinstance(user_id, str) or not user_id:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Unauthorized",
        )

    return CurrentUser(clerk_user_id=user_id)
```

- [ ] **Step 2: Compile auth module**

Run:

```bash
python3 -m compileall backend/app/api/auth.py
```

Expected: file compiles.

- [ ] **Step 3: Commit**

```bash
git add backend/app/api/auth.py
git commit -m "feat: add clerk auth dependency"
```

## Task 6: Backend Domain And ORM Contract

**Files:**
- Modify: `backend/app/models/domain.py`
- Modify: `backend/app/data/models.py`
- Modify: `backend/app/services/impl.py`
- Modify: `backend/app/api/routers/__init__.py`
- Delete: `backend/app/api/routers/users.py`

- [ ] **Step 1: Remove local user schemas and switch user IDs to strings**

In `backend/app/models/domain.py`, delete:

```python
class UserCreate(BaseSchema):
    username: str
    email: str
    phone_number: Optional[str] = None
    password: str


class UserUpdate(BaseSchema):
    username: Optional[str] = None
    email: Optional[str] = None
    phone_number: Optional[str] = None
    is_active: Optional[bool] = None
    is_verified: Optional[bool] = None


class User(IDTimestampSchema):
    username: str
    email: str
    phone_number: Optional[str] = None
    is_active: bool
    is_verified: bool
```

Change these fields:

```python
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
```

Change every other `user_id: int` in `ChatMessage`, `ChatRequest`, `NotificationEvent`, `EmergencyCreate`, and `Emergency` to either remove it from create requests or make it `str` in response models:

```python
class ChatRequest(BaseSchema):
    trip_id: int
    message: str


class NotificationEvent(IDTimestampSchema):
    user_id: str


class EmergencyCreate(BaseSchema):
    type: EmergencyType
    status: EmergencyStatus = EmergencyStatus.ACTIVE
    latitude: float
    longitude: float


class Emergency(IDTimestampSchema):
    user_id: str
```

- [ ] **Step 2: Remove UserModel and change ORM user IDs**

In `backend/app/data/models.py`, remove `Boolean` from the import if unused and delete `UserModel`.

Change:

```python
user_id = Column(Integer, nullable=False, index=True)
```

to:

```python
user_id = Column(String, nullable=False, index=True)
```

for `TripModel`, `EmergencyModel`, `ChatMessageModel`, and `NotificationEventModel`.

- [ ] **Step 3: Remove user service and repository**

In `backend/app/services/impl.py`, remove imports and classes related to `UserModel`, `UserCreate`, `UserUpdate`, `UserRepository`, and `UserService`.

Change `TripService.create_trip` signature:

```python
async def create_trip(self, user_id: str, trip_data: TripCreate) -> TripModel:
```

Inside the method, set:

```python
user_id=user_id,
```

Change `EmergencyService.create_emergency` signature:

```python
async def create_emergency(self, user_id: str, emergency_data: EmergencyCreate) -> EmergencyModel:
```

Inside the method, set:

```python
user_id=user_id,
```

- [ ] **Step 4: Remove users router from API router**

In `backend/app/api/routers/__init__.py`, remove:

```python
from .users import router as users_router
api_router.include_router(users_router)
```

- [ ] **Step 5: Delete local users router**

Delete:

```bash
backend/app/api/routers/users.py
```

- [ ] **Step 6: Compile backend models and services**

Run:

```bash
python3 -m compileall backend/app/models backend/app/data backend/app/services backend/app/api/routers
```

Expected: no import errors for removed user classes.

- [ ] **Step 7: Commit**

```bash
git add backend/app/models/domain.py backend/app/data/models.py backend/app/services/impl.py backend/app/api/routers/__init__.py
git rm backend/app/api/routers/users.py
git commit -m "refactor: remove local user contract"
```

## Task 7: Backend Protected Routes And Ownership

**Files:**
- Modify: `backend/app/data/repositories.py`
- Modify: `backend/app/services/impl.py`
- Modify: `backend/app/api/routers/trips.py`
- Modify: `backend/app/api/routers/locations.py`
- Modify: `backend/app/api/routers/emergencies.py`

- [ ] **Step 1: Add repository helpers**

In `TripRepository`, add:

```python
async def get_owned_by_id(self, trip_id: int, user_id: str) -> Optional[TripModel]:
    result = await self.session.execute(
        select(TripModel)
        .where(TripModel.id == trip_id)
        .where(TripModel.user_id == user_id)
    )
    return result.scalar_one_or_none()
```

In `EmergencyRepository` in `backend/app/services/impl.py`, add:

```python
async def get_owned_by_id(self, emergency_id: int, user_id: str) -> Optional[EmergencyModel]:
    result = await self.session.execute(
        select(EmergencyModel)
        .where(EmergencyModel.id == emergency_id)
        .where(EmergencyModel.user_id == user_id)
    )
    return result.scalar_one_or_none()
```

- [ ] **Step 2: Add service ownership methods**

In `TripService`, add:

```python
async def get_owned_trip_detail(
    self,
    trip_id: int,
    user_id: str,
) -> tuple[Optional[TripModel], Optional[LocationModel], list[NotificationEventModel]]:
    trip_repo = TripRepository(self.db)
    location_repo = LocationRepository(self.db)
    notification_repo = NotificationEventRepository(self.db)

    trip = await trip_repo.get_owned_by_id(trip_id, user_id)
    if not trip:
        return None, None, []

    latest_location = await location_repo.get_latest_for_trip(trip_id)
    notifications = await notification_repo.list_events(trip_id=trip_id, user_id=user_id)
    return trip, latest_location, notifications
```

In `LocationService`, add:

```python
async def get_owned_trip_locations(
    self,
    trip_id: int,
    user_id: str,
    limit: int = 100,
) -> Optional[List[LocationModel]]:
    trip_repo = TripRepository(self.db)
    trip = await trip_repo.get_owned_by_id(trip_id, user_id)
    if not trip:
        return None

    result = await self.db.execute(
        select(LocationModel)
        .where(LocationModel.trip_id == trip_id)
        .order_by(desc(LocationModel.recorded_at), desc(LocationModel.id))
        .limit(limit)
    )
    return list(result.scalars().all())
```

In `EmergencyService.update_emergency`, change the signature:

```python
async def update_emergency(
    self,
    emergency_id: int,
    user_id: str,
    emergency_data: EmergencyUpdate,
) -> Optional[EmergencyModel]:
```

Use:

```python
repo = EmergencyRepository(self.db)
emergency = await repo.get_owned_by_id(emergency_id, user_id)
```

- [ ] **Step 3: Protect trips routes**

In `trips.py`, import:

```python
from backend.app.api.auth import CurrentUser, require_current_user
```

Update `create_trip`:

```python
@router.post("/", response_model=Trip)
async def create_trip(
    trip: TripCreate,
    current_user: CurrentUser = Depends(require_current_user),
    db: AsyncSession = Depends(get_db),
):
    """创建新行程"""
    service = TripService(db)
    trip_model = await service.create_trip(current_user.clerk_user_id, trip)
    return _to_trip(trip_model)
```

Update `get_trip_detail`:

```python
@router.get("/{trip_id}", response_model=TripDetail)
async def get_trip_detail(
    trip_id: int,
    current_user: CurrentUser = Depends(require_current_user),
    db: AsyncSession = Depends(get_db),
):
    """获取行程详情"""
    service = TripService(db)
    trip_model, latest_location, notifications = await service.get_owned_trip_detail(
        trip_id,
        current_user.clerk_user_id,
    )
    if not trip_model:
        raise HTTPException(status_code=404, detail="Trip not found")
    return TripDetail(
        trip=_to_trip(trip_model),
        latest_location=_to_location(latest_location) if latest_location else None,
        notifications=[_to_notification(item) for item in notifications],
    )
```

- [ ] **Step 4: Protect locations routes**

In `locations.py`, import `CurrentUser` and `require_current_user`.

Update `create_location` to require auth and verify ownership through the parent trip:

```python
@router.post("/", response_model=Location)
async def create_location(
    location: LocationCreate,
    current_user: CurrentUser = Depends(require_current_user),
    db: AsyncSession = Depends(get_db),
):
    """创建定位记录"""
    trip_service = TripService(db)
    trip = await trip_service.get_trip(location.trip_id)
    if not trip or trip.user_id != current_user.clerk_user_id:
        raise HTTPException(status_code=404, detail="Trip not found")

    service = LocationService(db)
    location_model = await service.create_location(location)
    return _to_location(location_model)
```

Update `get_trip_locations`:

```python
@router.get("/trip/{trip_id}", response_model=List[Location])
async def get_trip_locations(
    trip_id: int,
    limit: int = 100,
    current_user: CurrentUser = Depends(require_current_user),
    db: AsyncSession = Depends(get_db),
):
    """获取行程的定位记录"""
    service = LocationService(db)
    location_models = await service.get_owned_trip_locations(
        trip_id,
        current_user.clerk_user_id,
        limit,
    )
    if location_models is None:
        raise HTTPException(status_code=404, detail="Trip not found")
    return [_to_location(location_model) for location_model in location_models]
```

- [ ] **Step 5: Protect emergencies routes**

In `emergencies.py`, import `CurrentUser` and `require_current_user`.

Update `create_emergency`:

```python
@router.post("/", response_model=Emergency)
async def create_emergency(
    emergency: EmergencyCreate,
    current_user: CurrentUser = Depends(require_current_user),
    db: AsyncSession = Depends(get_db),
):
    """创建紧急事件"""
    service = EmergencyService(db)
    emergency_model = await service.create_emergency(current_user.clerk_user_id, emergency)
    return Emergency(
        id=emergency_model.id,
        user_id=emergency_model.user_id,
        type=emergency_model.type,
        status=emergency_model.status,
        latitude=emergency_model.latitude,
        longitude=emergency_model.longitude,
        timestamp=emergency_model.timestamp,
        resolved_at=emergency_model.resolved_at,
        created_at=emergency_model.created_at,
        updated_at=emergency_model.updated_at,
    )
```

Update `update_emergency` to pass `current_user.clerk_user_id` to the service and return `404` when no owned record is found.

Update `get_active_emergencies` to require auth and return only records for the authenticated user. Add this service method:

```python
async def get_active_emergencies_for_user(self, user_id: str) -> List[EmergencyModel]:
    result = await self.db.execute(
        select(EmergencyModel)
        .where(EmergencyModel.user_id == user_id)
        .where(EmergencyModel.status != "resolved")
        .order_by(desc(EmergencyModel.timestamp), desc(EmergencyModel.id))
    )
    return list(result.scalars().all())
```

- [ ] **Step 6: Compile protected backend routes**

Run:

```bash
python3 -m compileall backend/app/api backend/app/services backend/app/data
```

Expected: no missing imports or signature mismatches.

- [ ] **Step 7: Commit**

```bash
git add backend/app/data/repositories.py backend/app/services/impl.py backend/app/api/routers/trips.py backend/app/api/routers/locations.py backend/app/api/routers/emergencies.py
git commit -m "feat: protect backend routes with clerk users"
```

## Task 8: Backend Contract Tests

**Files:**
- Create: `backend/tests/test_clerk_auth_contract.py`

- [ ] **Step 1: Create backend test file**

Create `backend/tests/test_clerk_auth_contract.py`:

```python
from datetime import datetime, timezone

import pytest
import pytest_asyncio
from httpx import ASGITransport, AsyncClient
from sqlalchemy.ext.asyncio import async_sessionmaker, create_async_engine

from backend.app.api.auth import CurrentUser, require_current_user
from backend.app.data.database import Base, get_db
from backend.main import app


@pytest_asyncio.fixture
async def test_db():
    engine = create_async_engine("sqlite+aiosqlite:///:memory:", future=True)
    async with engine.begin() as conn:
        await conn.run_sync(Base.metadata.create_all)

    session_factory = async_sessionmaker(engine, expire_on_commit=False)
    try:
        yield session_factory
    finally:
        await engine.dispose()


@pytest_asyncio.fixture
async def client(test_db):
    async def override_db():
        async with test_db() as session:
            yield session

    app.dependency_overrides[get_db] = override_db
    transport = ASGITransport(app=app)
    async with AsyncClient(transport=transport, base_url="http://test") as test_client:
        yield test_client
    app.dependency_overrides.clear()


def auth_as(user_id: str):
    async def override_auth():
        return CurrentUser(clerk_user_id=user_id)

    return override_auth


def trip_payload():
    return {
        "mode": "walking",
        "start_latitude": 31.2304,
        "start_longitude": 121.4737,
        "destination_latitude": 31.2404,
        "destination_longitude": 121.4837,
        "planned_arrival_at": datetime.now(timezone.utc).isoformat(),
        "planned_path": [
            {"latitude": 31.2304, "longitude": 121.4737},
            {"latitude": 31.2404, "longitude": 121.4837},
        ],
        "deviation_threshold_meters": 100.0,
        "timeout_grace_minutes": 10,
    }


@pytest.mark.asyncio
async def test_create_trip_uses_authenticated_user(client):
    app.dependency_overrides[require_current_user] = auth_as("user_clerk_1")

    response = await client.post("/api/trips/", json={**trip_payload(), "user_id": 999})

    assert response.status_code == 200
    assert response.json()["user_id"] == "user_clerk_1"


@pytest.mark.asyncio
async def test_trip_detail_for_other_user_returns_404(client):
    app.dependency_overrides[require_current_user] = auth_as("user_owner")
    created = await client.post("/api/trips/", json=trip_payload())
    assert created.status_code == 200
    trip_id = created.json()["id"]

    app.dependency_overrides[require_current_user] = auth_as("user_other")
    response = await client.get(f"/api/trips/{trip_id}")

    assert response.status_code == 404


@pytest.mark.asyncio
async def test_trip_locations_for_other_user_returns_404(client):
    app.dependency_overrides[require_current_user] = auth_as("user_owner")
    created = await client.post("/api/trips/", json=trip_payload())
    assert created.status_code == 200
    trip_id = created.json()["id"]

    app.dependency_overrides[require_current_user] = auth_as("user_other")
    response = await client.get(f"/api/locations/trip/{trip_id}")

    assert response.status_code == 404


def test_users_router_removed():
    paths = {route.path for route in app.routes}
    assert "/api/users/" not in paths
```

- [ ] **Step 2: Run backend tests**

Run:

```bash
python3 -m pytest backend/tests/test_clerk_auth_contract.py -q
```

Expected: all tests pass. If `clerk-backend-api` is not installed, first run:

```bash
python3 -m pip install -r backend/requirements.txt
```

- [ ] **Step 3: Compile backend**

Run:

```bash
python3 -m compileall backend
```

Expected: no syntax errors.

- [ ] **Step 4: Commit**

```bash
git add backend/tests/test_clerk_auth_contract.py
git commit -m "test: cover clerk auth backend contract"
```

## Task 9: Clerk Dashboard Android Application Configuration

**Files:**
- No repository file changes in this task

- [ ] **Step 1: Open Clerk Android application configuration**

Open the Clerk Dashboard for the SafeWalk instance and navigate to the Android application setup for:

```text
https://brave-lobster-37.clerk.accounts.dev
```

- [ ] **Step 2: Add the SafeWalk Android app**

Fill the Clerk form with:

```text
Namespace: Android app
Package name: com.celestine.safewalk
SHA-256 certificate fingerprints: EB:0A:8C:CE:4B:3B:FF:A9:F9:96:82:82:B9:05:40:CB:ED:F9:30:27:54:2B:9B:97:68:98:13:78:0C:02:C8:B1
```

This SHA-256 is the local Android debug keystore fingerprint from:

```bash
"/Applications/Android Studio.app/Contents/jbr/Contents/Home/bin/keytool" -list -v -keystore "$HOME/.android/debug.keystore" -alias androiddebugkey -storepass android -keypass android
```

- [ ] **Step 3: Verify Clerk asset links**

Open:

```text
https://brave-lobster-37.clerk.accounts.dev/.well-known/assetlinks.json
```

Expected: the response is no longer `[]` and contains an entry for:

```json
{
  "target": {
    "namespace": "android_app",
    "package_name": "com.celestine.safewalk",
    "sha256_cert_fingerprints": [
      "EB:0A:8C:CE:4B:3B:FF:A9:F9:96:82:82:B9:05:40:CB:ED:F9:30:27:54:2B:9B:97:68:98:13:78:0C:02:C8:B1"
    ]
  }
}
```

If it still returns `[]`, wait a few minutes and refresh. If it remains empty, reopen the Clerk Android application configuration and confirm the app was saved.

- [ ] **Step 4: Record release keystore follow-up**

Add this note to the final implementation summary:

```text
The configured SHA-256 is for the debug keystore only. Before shipping a release build, add the release keystore SHA-256 fingerprint to Clerk as an additional Android application fingerprint.
```

- [ ] **Step 5: Commit**

No commit is needed unless documentation is updated in Task 10.

## Task 10: Documentation And Final Verification

**Files:**
- Modify: `backend/README.md`
- Modify: `Docs/项目介绍.md` if it mentions local backend registration or JWT

- [ ] **Step 1: Update backend README tech stack and setup**

In `backend/README.md`, change Python requirement to:

```markdown
- Python 3.10+
```

Replace the security section with:

````markdown
## 认证

SafeWalk 后端使用 Clerk 验证移动端传来的 session token。客户端必须在业务请求中发送：

```http
Authorization: Bearer <clerk_session_token>
```

后端不再保存本地密码，也不再提供 `/api/users/*` 注册登录接口。业务数据中的 `user_id` 是 Clerk 用户 ID 字符串。
````

Add a local database caveat:

```markdown
> 注意：第一期 Clerk 接入不包含旧数据库迁移。如果本地已有 `safewalk.db` 仍使用旧的整数 `user_id` schema，开发验证前需要手动删除或重建本地数据库。
```

- [ ] **Step 2: Update Android run instructions**

Add:

````markdown
Android 调试需要配置：

```bash
CLERK_PUBLISHABLE_KEY=pk_test_xxx
SAFEWALK_BACKEND_BASE_URL=http://10.0.2.2:8000/api
```

Clerk Dashboard 的 Android application 需要包含：

```text
Package name: com.celestine.safewalk
Debug SHA-256: EB:0A:8C:CE:4B:3B:FF:A9:F9:96:82:82:B9:05:40:CB:ED:F9:30:27:54:2B:9B:97:68:98:13:78:0C:02:C8:B1
Asset links: https://brave-lobster-37.clerk.accounts.dev/.well-known/assetlinks.json
```
````

- [ ] **Step 3: Run final checks**

Run:

```bash
./gradlew assembleDebug
python3 -m pytest backend/tests/test_clerk_auth_contract.py -q
python3 -m compileall backend
```

Expected: Android build passes, backend tests pass, backend compiles.

- [ ] **Step 4: Check API/data contract impact for final notes**

Run:

```bash
rg -n "user_id|UserCreate|UserUpdate|UserModel|SECRET_KEY|ACCESS_TOKEN|ALGORITHM|DEMO_USER_ID" app/src/main/java backend Docs backend/README.md
```

Expected:

- No `DEMO_USER_ID`.
- No active `UserCreate`, `UserUpdate`, `UserModel`, or `/api/users` router references.
- `user_id` remains only as Clerk string response/storage fields or documented contract text.
- No active local JWT settings remain.

- [ ] **Step 5: Commit docs**

```bash
git add backend/README.md Docs/项目介绍.md
git commit -m "docs: document clerk authentication setup"
```

Skip `Docs/项目介绍.md` in the `git add` command if the search shows it does not mention local registration or JWT.

## Manual Environment Notes

- Clerk Dashboard must have Native API enabled for the Android application.
- Clerk Dashboard must include package `com.celestine.safewalk` and debug SHA-256 `EB:0A:8C:CE:4B:3B:FF:A9:F9:96:82:82:B9:05:40:CB:ED:F9:30:27:54:2B:9B:97:68:98:13:78:0C:02:C8:B1`.
- Clerk asset links must be non-empty at `https://brave-lobster-37.clerk.accounts.dev/.well-known/assetlinks.json`.
- `CLERK_PUBLISHABLE_KEY` must be available to the Android Gradle build.
- Backend must run with `CLERK_SECRET_KEY` and `CLERK_PUBLISHABLE_KEY`.
- `CLERK_JWT_KEY` is optional, but recommended for networkless backend verification.
- Release builds need the release keystore SHA-256 added to Clerk before distribution.
- Existing local `safewalk.db` is not migrated by this plan. Delete or recreate it manually if schema mismatch blocks local verification.
