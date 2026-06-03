package com.celestine.safewalk

import android.Manifest
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.amap.api.maps.AMap
import com.amap.api.maps.CameraUpdateFactory
import com.amap.api.maps.model.BitmapDescriptorFactory
import com.amap.api.maps.model.LatLng
import com.amap.api.maps.model.LatLngBounds
import com.amap.api.maps.model.MarkerOptions
import com.amap.api.maps.model.MyLocationStyle
import com.amap.api.maps.model.PolylineOptions
import com.amap.api.maps.TextureMapView
import com.amap.api.services.core.AMapException
import com.amap.api.services.core.LatLonPoint
import com.amap.api.services.help.Inputtips
import com.amap.api.services.help.InputtipsQuery
import com.amap.api.services.help.Tip
import com.amap.api.services.route.BusRouteResult
import com.amap.api.services.route.DriveRouteResult
import com.amap.api.services.route.RideRouteResult
import com.amap.api.services.route.RouteSearch
import com.amap.api.services.route.RouteSearchV2
import com.amap.api.services.route.WalkPath
import com.amap.api.services.route.WalkRouteResult
import java.time.OffsetDateTime
import java.time.ZoneOffset
import kotlin.math.roundToInt

class MapActivity : ComponentActivity(), AMap.OnMyLocationChangeListener, AMap.OnMapLongClickListener,
    RouteSearch.OnRouteSearchListener {

    private var mapView: TextureMapView? = null
    private lateinit var backendClient: SafeWalkBackendClient
    private var aMap: AMap? = null
    private var routeSearch: RouteSearch? = null
    private var routeSearchV2: RouteSearchV2? = null
    private val guardHandler = Handler(Looper.getMainLooper())

    private val routeSearchV2Listener = object : RouteSearchV2.OnRouteSearchListener {
        override fun onDriveRouteSearched(result: com.amap.api.services.route.DriveRouteResultV2?, errorCode: Int) = Unit

        override fun onBusRouteSearched(result: com.amap.api.services.route.BusRouteResultV2?, errorCode: Int) = Unit

        override fun onRideRouteSearched(result: com.amap.api.services.route.RideRouteResultV2?, errorCode: Int) = Unit

        override fun onWalkRouteSearched(result: com.amap.api.services.route.WalkRouteResultV2?, errorCode: Int) {
            handleWalkRouteResult(
                paths = result?.paths.orEmpty(),
                startPos = result?.startPos,
                endPos = result?.targetPos,
                errorCode = errorCode,
                source = "RouteSearchV2",
            )
        }
    }

    private var uiState by mutableStateOf(
        MapUiState(
            permissionGranted = false,
            statusMessage = "",
        )
    )

    private var currentLocation: Location? = null
    private var routeStartPoint: LatLonPoint? = null
    private var destinationPoint: LatLonPoint? = null
    private var routeAlternatives: List<RouteAlternative> = emptyList()
    private var selectedRouteAlternative: RouteAlternative? = null
    private var activeTripId: Int? = null
    private var hasCenteredOnLocation = false
    private var destinationSuggestionRequestId = 0
    private var pendingSuggestionRunnable: Runnable? = null

    private data class RouteAlternative(
        val id: String,
        val distanceMeters: Int,
        val durationSeconds: Int,
        val polylinePoints: List<LatLng>,
    )

    private val uploadRunnable = object : Runnable {
        override fun run() {
            uploadCurrentLocation()
            if (activeTripId != null) {
                guardHandler.postDelayed(this, LOCATION_UPLOAD_INTERVAL_MS)
            }
        }
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            enableLocationLayer()
        } else {
            uiState = uiState.copy(
                permissionGranted = false,
                statusMessage = getString(R.string.map_permission_denied),
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val mode = intent.getStringExtra(EXTRA_MODE)
        if (mode != Mode.WALKING.name.lowercase()) {
            Toast.makeText(this, R.string.map_mode_not_supported, Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        backendClient = SafeWalkBackendClient()
        val renderPolicyAllowsAmap = shouldRenderAmap()
        if (renderPolicyAllowsAmap) {
            mapView = runCatching {
                TextureMapView(this).also { view ->
                    view.onCreate(savedInstanceState)
                }
            }.getOrElse {
                Log.e("MapActivity", "TextureMapView init failed", it)
                Toast.makeText(this, R.string.map_init_failed, Toast.LENGTH_SHORT).show()
                null
            }
        }
        val isAmapRenderable = mapView != null

        uiState = MapUiState(
            permissionGranted = hasLocationPermission(),
            statusMessage = if (!isAmapRenderable) {
                getMapUnavailableMessage()
            } else if (hasLocationPermission()) {
                getString(R.string.map_status_waiting_location)
            } else {
                getString(R.string.map_permission_denied)
            },
        )

        setupMap()
        setContent {
            SafeWalkApp {
                MapScreenContent(
                    state = uiState,
                    onBack = { finish() },
                    onRequestPermission = { requestLocationPermission() },
                    onDestinationQueryChange = { onDestinationQueryChanged(it) },
                    onDestinationSuggestionSelected = { onDestinationSuggestionSelected(it) },
                    onRouteSelected = { onRouteSelected(it) },
                    onResetSelection = { resetRouteSelection() },
                    onStartGuard = { startGuarding() },
                    onStopGuard = { stopGuarding() },
                    mapContent = {
                        val currentMapView = mapView
                        if (currentMapView != null) {
                            AndroidView(
                                factory = {
                                    (currentMapView.parent as? ViewGroup)?.removeView(currentMapView)
                                    currentMapView
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            MapPlaceholder()
                        }
                    }
                )
            }
        }

        requestLocationPermission()
    }

    override fun onResume() {
        super.onResume()
        mapView?.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView?.onPause()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView?.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        stopGuarding()
        pendingSuggestionRunnable?.let(guardHandler::removeCallbacks)
        pendingSuggestionRunnable = null
        if (::backendClient.isInitialized) {
            backendClient.shutdown()
        }
        mapView?.onDestroy()
        super.onDestroy()
    }

    override fun onMyLocationChange(location: Location?) {
        val newLocation = location ?: return
        currentLocation = newLocation

        if (routeStartPoint == null) {
            routeStartPoint = LatLonPoint(newLocation.latitude, newLocation.longitude)
            uiState = uiState.copy(statusMessage = getString(R.string.map_status_search_destination))
        }

        lockMapAroundLocation(newLocation, animate = !hasCenteredOnLocation)
        if (!hasCenteredOnLocation) {
            hasCenteredOnLocation = true
        }
    }

    override fun onMapLongClick(latLng: LatLng?) {
        val clickedPoint = latLng ?: return
        if (routeStartPoint == null) {
            Toast.makeText(this, R.string.map_waiting_location_toast, Toast.LENGTH_SHORT).show()
            return
        }
        onDestinationSelected(
            destination = LatLonPoint(clickedPoint.latitude, clickedPoint.longitude),
            displayName = getString(R.string.map_destination_point),
        )
    }

    override fun onBusRouteSearched(result: BusRouteResult?, errorCode: Int) = Unit

    override fun onDriveRouteSearched(result: DriveRouteResult?, errorCode: Int) = Unit

    override fun onRideRouteSearched(result: RideRouteResult?, errorCode: Int) = Unit

    override fun onWalkRouteSearched(result: WalkRouteResult?, errorCode: Int) {
        handleWalkRouteResult(
            paths = result?.paths.orEmpty(),
            startPos = result?.startPos,
            endPos = result?.targetPos,
            errorCode = errorCode,
            source = "RouteSearch",
        )
    }

    private fun handleWalkRouteResult(
        paths: List<WalkPath>,
        startPos: LatLonPoint?,
        endPos: LatLonPoint?,
        errorCode: Int,
        source: String,
    ) {
        Log.d("MapActivity", "Walk route callback from $source, error=$errorCode, pathCount=${paths.size}")
        if (errorCode != AMapException.CODE_AMAP_SUCCESS || paths.isEmpty()) {
            routeAlternatives = emptyList()
            selectedRouteAlternative = null
            uiState = uiState.copy(
                isSearchingRoute = false,
                routeOptions = emptyList(),
                selectedRouteId = null,
                statusMessage = getString(R.string.map_route_failed),
            )
            Toast.makeText(this, R.string.map_route_failed, Toast.LENGTH_SHORT).show()
            return
        }

        if (startPos == null || endPos == null) {
            uiState = uiState.copy(
                isSearchingRoute = false,
                routeOptions = emptyList(),
                selectedRouteId = null,
                statusMessage = getString(R.string.map_route_failed),
            )
            return
        }

        val alternatives = paths
            .take(MAX_ROUTE_RECOMMENDATION_COUNT)
            .mapIndexed { index, walkPath ->
                RouteAlternative(
                    id = "route_${index + 1}",
                    distanceMeters = walkPath.distance.toInt(),
                    durationSeconds = walkPath.duration.toInt(),
                    polylinePoints = buildPolylinePoints(
                        start = startPos,
                        walkPath = walkPath,
                        end = endPos,
                    )
                )
            }
            .filter { it.polylinePoints.isNotEmpty() }

        if (alternatives.isEmpty()) {
            uiState = uiState.copy(
                isSearchingRoute = false,
                routeOptions = emptyList(),
                selectedRouteId = null,
                statusMessage = getString(R.string.map_route_failed),
            )
            return
        }

        routeAlternatives = alternatives
        selectedRouteAlternative = null
        renderRoute(selectedRoute = null)

        uiState = uiState.copy(
            isSearchingRoute = false,
            routeOptions = alternatives.mapIndexed { index, option ->
                MapRouteOption(
                    id = option.id,
                    title = getString(R.string.map_route_option_title_format, index + 1),
                    distanceText = formatDistance(option.distanceMeters),
                    durationText = formatDuration(option.durationSeconds),
                )
            },
            selectedRouteId = null,
            statusMessage = getString(R.string.map_select_route_to_guard),
        )
    }

    private fun setupMap() {
        val currentMapView = mapView ?: return
        aMap = currentMapView.map
        aMap?.apply {
            uiSettings.isZoomControlsEnabled = false
            uiSettings.isMyLocationButtonEnabled = true
            setOnMyLocationChangeListener(this@MapActivity)
            setOnMapLongClickListener(this@MapActivity)
            setMyLocationStyle(
                MyLocationStyle().myLocationType(MyLocationStyle.LOCATION_TYPE_LOCATION_ROTATE_NO_CENTER)
            )
        }

        try {
            routeSearch = RouteSearch(this).apply {
                setRouteSearchListener(this@MapActivity)
            }
            routeSearchV2 = RouteSearchV2(this).apply {
                setRouteSearchListener(routeSearchV2Listener)
            }
        } catch (_: Exception) {
            Toast.makeText(this, R.string.map_init_failed, Toast.LENGTH_SHORT).show()
        }
    }

    private fun requestLocationPermission() {
        if (hasLocationPermission()) {
            enableLocationLayer()
            return
        }
        permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    private fun enableLocationLayer() {
        if (!hasLocationPermission()) {
            return
        }

        if (aMap == null) {
            uiState = uiState.copy(
                permissionGranted = true,
                statusMessage = getMapUnavailableMessage(),
            )
            return
        }

        try {
            aMap?.isMyLocationEnabled = true
            uiState = uiState.copy(
                permissionGranted = true,
                statusMessage = if (routeSelected()) {
                    getString(R.string.map_route_ready_hint)
                } else {
                    getString(R.string.map_status_waiting_location)
                },
            )
        } catch (_: SecurityException) {
            uiState = uiState.copy(
                permissionGranted = false,
                statusMessage = getString(R.string.map_permission_denied),
            )
        }
    }

    private fun onDestinationQueryChanged(rawQuery: String) {
        if (!uiState.isGuarding && routeAlternatives.isNotEmpty()) {
            routeAlternatives = emptyList()
            selectedRouteAlternative = null
            uiState = uiState.copy(routeOptions = emptyList(), selectedRouteId = null)
            renderRoute(selectedRoute = null)
        }

        uiState = uiState.copy(destinationQuery = rawQuery)
        val query = rawQuery.trim()
        pendingSuggestionRunnable?.let(guardHandler::removeCallbacks)
        pendingSuggestionRunnable = null

        if (query.isEmpty()) {
            destinationSuggestionRequestId += 1
            uiState = uiState.copy(
                destinationSuggestions = emptyList(),
                isSearchingDestination = false,
            )
            return
        }

        if (routeStartPoint == null) {
            uiState = uiState.copy(
                destinationSuggestions = emptyList(),
                isSearchingDestination = false,
                statusMessage = getString(R.string.map_status_waiting_location),
            )
            return
        }

        val requestId = destinationSuggestionRequestId + 1
        destinationSuggestionRequestId = requestId
        uiState = uiState.copy(
            isSearchingDestination = true,
            destinationSuggestions = emptyList(),
        )

        val searchRunnable = Runnable {
            requestDestinationSuggestions(query, requestId)
        }
        pendingSuggestionRunnable = searchRunnable
        guardHandler.postDelayed(searchRunnable, DESTINATION_SEARCH_DEBOUNCE_MS)
    }

    private fun requestDestinationSuggestions(keyword: String, requestId: Int) {
        val anchorPoint = routeStartPoint ?: return
        val query = InputtipsQuery(keyword, "").apply {
            setCityLimit(false)
            setLocation(anchorPoint)
        }

        try {
            val inputtips = Inputtips(this, query)
            inputtips.setInputtipsListener { tips: List<Tip>?, errorCode: Int ->
                if (requestId != destinationSuggestionRequestId) {
                    return@setInputtipsListener
                }
                if (errorCode != AMapException.CODE_AMAP_SUCCESS) {
                    uiState = uiState.copy(
                        isSearchingDestination = false,
                        destinationSuggestions = emptyList(),
                    )
                    return@setInputtipsListener
                }

                val suggestions = tips
                    .orEmpty()
                    .mapNotNull { tip ->
                        val point = tip.point ?: return@mapNotNull null
                        val distanceMeters = calculateDistanceMeters(anchorPoint, point)
                        if (distanceMeters > MAP_LOCK_RADIUS_METERS) {
                            return@mapNotNull null
                        }
                        val title = tip.name?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                        val subtitle = listOfNotNull(
                            tip.district?.takeIf { it.isNotBlank() },
                            tip.address?.takeIf { it.isNotBlank() }
                        ).joinToString(" · ")

                        MapDestinationSuggestion(
                            id = tip.poiID?.takeIf { it.isNotBlank() }
                                ?: "${title}_${point.latitude}_${point.longitude}",
                            title = title,
                            subtitle = subtitle,
                            distanceText = formatDistance(distanceMeters.toInt()),
                            latitude = point.latitude,
                            longitude = point.longitude,
                        ) to distanceMeters
                    }
                    .sortedBy { it.second }
                    .map { it.first }
                    .take(MAX_DESTINATION_SUGGESTIONS)

                uiState = uiState.copy(
                    isSearchingDestination = false,
                    destinationSuggestions = suggestions,
                )
            }
            inputtips.requestInputtipsAsyn()
        } catch (_: Exception) {
            if (requestId == destinationSuggestionRequestId) {
                uiState = uiState.copy(
                    isSearchingDestination = false,
                    destinationSuggestions = emptyList(),
                )
            }
        }
    }

    private fun onDestinationSuggestionSelected(suggestion: MapDestinationSuggestion) {
        onDestinationSelected(
            destination = LatLonPoint(suggestion.latitude, suggestion.longitude),
            displayName = suggestion.title,
        )
    }

    private fun onDestinationSelected(destination: LatLonPoint, displayName: String) {
        if (routeStartPoint == null) {
            Toast.makeText(this, R.string.map_waiting_location_toast, Toast.LENGTH_SHORT).show()
            return
        }
        destinationPoint = destination
        routeAlternatives = emptyList()
        selectedRouteAlternative = null
        uiState = uiState.copy(
            destinationQuery = displayName,
            destinationSuggestions = emptyList(),
            isSearchingDestination = false,
            isSearchingRoute = true,
            routeOptions = emptyList(),
            selectedRouteId = null,
            statusMessage = getString(R.string.map_status_searching_route),
            syncMessage = getString(R.string.map_sync_selected_destination_format, displayName),
        )
        searchWalkRoute()
    }

    private fun onRouteSelected(routeId: String) {
        val route = routeAlternatives.firstOrNull { it.id == routeId } ?: return
        selectedRouteAlternative = route
        renderRoute(selectedRoute = route)
        uiState = uiState.copy(
            selectedRouteId = routeId,
            statusMessage = getString(R.string.map_route_ready_hint),
            syncMessage = getString(
                R.string.map_route_selected_format,
                formatDistance(route.distanceMeters),
                formatDuration(route.durationSeconds)
            ),
        )
    }

    private fun resetRouteSelection() {
        destinationPoint = null
        routeAlternatives = emptyList()
        selectedRouteAlternative = null
        renderRoute(selectedRoute = null)
        uiState = uiState.copy(
            destinationQuery = "",
            destinationSuggestions = emptyList(),
            routeOptions = emptyList(),
            selectedRouteId = null,
            isSearchingDestination = false,
            isSearchingRoute = false,
            statusMessage = if (routeStartPoint == null) {
                getString(R.string.map_status_waiting_location)
            } else {
                getString(R.string.map_status_search_destination)
            },
            syncMessage = null,
        )
    }

    private fun lockMapAroundLocation(location: Location, animate: Boolean) {
        val map = aMap ?: return
        val center = LatLng(location.latitude, location.longitude)
        val latDelta = metersToLatitudeDelta(MAP_LOCK_RADIUS_METERS)
        val lngDelta = metersToLongitudeDelta(MAP_LOCK_RADIUS_METERS, location.latitude)
        val bounds = LatLngBounds(
            LatLng(center.latitude - latDelta, center.longitude - lngDelta),
            LatLng(center.latitude + latDelta, center.longitude + lngDelta),
        )
        map.setMapStatusLimits(bounds)
        map.setMinZoomLevel(MAP_MIN_ZOOM_LEVEL)
        if (animate) {
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(center, MAP_DEFAULT_ZOOM_LEVEL))
        }
    }

    private fun calculateDistanceMeters(start: LatLonPoint, end: LatLonPoint): Float {
        val result = FloatArray(1)
        Location.distanceBetween(
            start.latitude,
            start.longitude,
            end.latitude,
            end.longitude,
            result,
        )
        return result[0]
    }

    private fun metersToLatitudeDelta(meters: Float): Double {
        return meters / 111_320.0
    }

    private fun metersToLongitudeDelta(meters: Float, latitude: Double): Double {
        val latitudeCos = kotlin.math.cos(Math.toRadians(latitude)).coerceAtLeast(0.01)
        return meters / (111_320.0 * latitudeCos)
    }

    private fun searchWalkRoute() {
        val start = routeStartPoint ?: return
        val end = destinationPoint ?: return

        try {
            val queryV2 = RouteSearchV2.WalkRouteQuery(
                RouteSearchV2.FromAndTo(start, end)
            ).apply {
                setAlternativeRoute(MAX_ROUTE_RECOMMENDATION_COUNT)
                setIndoor(false)
            }
            routeSearchV2?.calculateWalkRouteAsyn(queryV2) ?: run {
                val fromAndTo = RouteSearch.FromAndTo(start, end)
                val query = RouteSearch.WalkRouteQuery(fromAndTo, RouteSearch.WalkMultipath)
                routeSearch?.calculateWalkRouteAsyn(query)
            }
        } catch (_: Exception) {
            uiState = uiState.copy(
                isSearchingRoute = false,
                statusMessage = getString(R.string.map_route_failed),
            )
        }
    }

    private fun renderRoute(selectedRoute: RouteAlternative?) {
        val map = aMap ?: return
        map.clear()

        routeStartPoint?.let { start ->
            map.addMarker(
                MarkerOptions()
                    .position(LatLng(start.latitude, start.longitude))
                    .title(getString(R.string.map_start_point))
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
            )
        }

        destinationPoint?.let { destination ->
            map.addMarker(
                MarkerOptions()
                    .position(LatLng(destination.latitude, destination.longitude))
                    .title(getString(R.string.map_destination_point))
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
            )
        }

        routeAlternatives.forEach { option ->
            map.addPolyline(
                PolylineOptions()
                    .addAll(option.polylinePoints)
                    .color(if (option.id == selectedRoute?.id) Color.parseColor("#1D4ED8") else Color.parseColor("#60A5FA"))
                    .width(if (option.id == selectedRoute?.id) 20f else 12f)
            )
        }
    }

    private fun startGuarding() {
        if (activeTripId != null) {
            return
        }

        val selectedRoute = selectedRouteAlternative ?: run {
            Toast.makeText(this, R.string.map_select_route_to_guard, Toast.LENGTH_SHORT).show()
            return
        }
        val start = routeStartPoint ?: return
        val end = destinationPoint ?: return
        val location = currentLocation ?: run {
            Toast.makeText(this, R.string.map_waiting_location_toast, Toast.LENGTH_SHORT).show()
            return
        }

        uiState = uiState.copy(isCreatingTrip = true)

        backendClient.createTrip(
            TripCreatePayload(
                mode = Mode.WALKING.name.lowercase(),
                startLatitude = start.latitude,
                startLongitude = start.longitude,
                destinationLatitude = end.latitude,
                destinationLongitude = end.longitude,
                plannedArrivalAt = OffsetDateTime.now(ZoneOffset.UTC)
                    .plusSeconds(selectedRoute.durationSeconds.toLong())
                    .toString(),
                plannedPath = selectedRoute.polylinePoints.map { point ->
                    PathPointPayload(
                        latitude = point.latitude,
                        longitude = point.longitude,
                    )
                },
                deviationThresholdMeters = DEFAULT_DEVIATION_THRESHOLD_METERS,
                timeoutGraceMinutes = DEFAULT_TIMEOUT_GRACE_MINUTES,
            )
        ) { result ->
            when (result) {
                is BackendResult.Success -> {
                    activeTripId = result.value.id
                    uiState = uiState.copy(
                        isCreatingTrip = false,
                        isGuarding = true,
                        statusMessage = getString(R.string.map_guarding_active),
                        syncMessage = getString(R.string.map_trip_created_format, result.value.id),
                    )
                    backendClient.getTripDetail(result.value.id) { detailResult ->
                        if (detailResult is BackendResult.Success) {
                            uiState = uiState.copy(
                                syncMessage = getString(
                                    R.string.map_trip_synced_format,
                                    detailResult.value.tripId,
                                    detailResult.value.status
                                )
                            )
                        }
                    }
                    uploadLocation(
                        tripId = result.value.id,
                        location = location,
                        updateSyncMessage = true,
                    )
                    guardHandler.removeCallbacks(uploadRunnable)
                    guardHandler.postDelayed(uploadRunnable, LOCATION_UPLOAD_INTERVAL_MS)
                }

                is BackendResult.Error -> {
                    uiState = uiState.copy(
                        isCreatingTrip = false,
                        isGuarding = false,
                        statusMessage = getString(R.string.map_guard_failed),
                    )
                    Toast.makeText(this, result.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun stopGuarding() {
        guardHandler.removeCallbacks(uploadRunnable)
        activeTripId = null
        if (uiState.isGuarding || uiState.isCreatingTrip) {
            uiState = uiState.copy(
                isGuarding = false,
                isCreatingTrip = false,
                statusMessage = getString(R.string.map_guarding_stopped),
                syncMessage = null,
            )
        }
    }

    private fun uploadCurrentLocation() {
        val tripId = activeTripId ?: return
        val location = currentLocation ?: return
        uploadLocation(tripId, location, updateSyncMessage = false)
    }

    private fun uploadLocation(
        tripId: Int,
        location: Location,
        updateSyncMessage: Boolean,
    ) {
        backendClient.uploadLocation(
            LocationUploadPayload(
                tripId = tripId,
                latitude = location.latitude,
                longitude = location.longitude,
                accuracy = location.accuracy.toDouble(),
                recordedAt = OffsetDateTime.now(ZoneOffset.UTC).toString(),
            )
        ) { result ->
            if (result is BackendResult.Success && updateSyncMessage) {
                uiState = uiState.copy(
                    syncMessage = getString(
                        R.string.map_location_synced_format,
                        formatLatLng(location.latitude, location.longitude)
                    )
                )
            }
        }
    }

    private fun routeSelected(): Boolean = selectedRouteAlternative != null

    private fun getMapUnavailableMessage(): String {
        return if (BuildConfig.HAS_AMAP_API_KEY) {
            getString(R.string.map_render_unavailable)
        } else {
            getString(R.string.map_missing_key)
        }
    }

    private fun shouldRenderAmap(): Boolean {
        return MapRenderingPolicy.shouldRenderAmap(
            hasAmapApiKey = BuildConfig.HAS_AMAP_API_KEY,
            isRunningOnEmulator = isRunningOnEmulator(),
            isDebugBuild = BuildConfig.DEBUG,
            forceAmapRenderingOnEmulator = BuildConfig.SAFEWALK_FORCE_AMAP_RENDERING_ON_EMULATOR,
        )
    }

    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION,
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
    }

    private fun buildPolylinePoints(
        start: LatLonPoint,
        walkPath: WalkPath,
        end: LatLonPoint,
    ): List<LatLng> {
        val points = mutableListOf(LatLng(start.latitude, start.longitude))
        walkPath.steps.orEmpty().forEach { step ->
            step.polyline.orEmpty().forEach { point ->
                points.add(LatLng(point.latitude, point.longitude))
            }
        }
        points.add(LatLng(end.latitude, end.longitude))
        return points.distinctBy { "${it.latitude},${it.longitude}" }
    }

    private fun formatDistance(distanceMeters: Int): String {
        return if (distanceMeters >= 1000) {
            String.format("%.1f km", distanceMeters / 1000.0)
        } else {
            getString(R.string.map_distance_meters_format, distanceMeters)
        }
    }

    private fun formatDuration(durationSeconds: Int): String {
        val totalMinutes = (durationSeconds / 60.0).roundToInt().coerceAtLeast(1)
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        return if (hours > 0) {
            getString(R.string.map_duration_hours_format, hours, minutes)
        } else {
            getString(R.string.map_duration_minutes_format, minutes)
        }
    }

    private fun formatLatLng(latitude: Double, longitude: Double): String {
        return String.format("%.5f, %.5f", latitude, longitude)
    }

    companion object {
        private const val EXTRA_MODE = "mode"
        private const val LOCATION_UPLOAD_INTERVAL_MS = 15_000L
        private const val DEFAULT_DEVIATION_THRESHOLD_METERS = 100.0
        private const val DEFAULT_TIMEOUT_GRACE_MINUTES = 10
        private const val MAX_ROUTE_RECOMMENDATION_COUNT = 3
        private const val MAX_DESTINATION_SUGGESTIONS = 8
        private const val DESTINATION_SEARCH_DEBOUNCE_MS = 260L
        private const val MAP_LOCK_RADIUS_METERS = 5_000f
        private const val MAP_DEFAULT_ZOOM_LEVEL = 13f
        private const val MAP_MIN_ZOOM_LEVEL = 11.5f

        private fun isRunningOnEmulator(): Boolean {
            return Build.FINGERPRINT.startsWith("generic") ||
                Build.FINGERPRINT.startsWith("unknown") ||
                Build.MODEL.contains("google_sdk", ignoreCase = true) ||
                Build.MODEL.contains("emulator", ignoreCase = true) ||
                Build.MODEL.contains("Android SDK built for", ignoreCase = true) ||
                Build.MANUFACTURER.contains("Genymotion", ignoreCase = true) ||
                Build.PRODUCT.contains("sdk", ignoreCase = true) ||
                Build.HARDWARE.contains("goldfish", ignoreCase = true) ||
                Build.HARDWARE.contains("ranchu", ignoreCase = true)
        }

        fun createIntent(context: Context, mode: Mode): Intent {
            return Intent(context, MapActivity::class.java).putExtra(
                EXTRA_MODE,
                mode.name.lowercase()
            )
        }
    }
}
