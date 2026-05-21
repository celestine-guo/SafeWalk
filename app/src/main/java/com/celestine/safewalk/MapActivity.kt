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
import com.amap.api.services.route.BusRouteResult
import com.amap.api.services.route.DriveRouteResult
import com.amap.api.services.route.RideRouteResult
import com.amap.api.services.route.RouteSearch
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
    private val guardHandler = Handler(Looper.getMainLooper())

    private var uiState by mutableStateOf(
        MapUiState(
            permissionGranted = false,
            statusMessage = "",
        )
    )

    private var currentLocation: Location? = null
    private var routeStartPoint: LatLonPoint? = null
    private var destinationPoint: LatLonPoint? = null
    private var routePolylinePoints: List<LatLng> = emptyList()
    private var routeDurationSeconds: Int = 0
    private var activeTripId: Int? = null
    private var hasCenteredOnLocation = false

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
        if (shouldRenderAmap()) {
            mapView = TextureMapView(this).also { view ->
                view.onCreate(savedInstanceState)
            }
        }

        uiState = MapUiState(
            permissionGranted = hasLocationPermission(),
            statusMessage = if (!shouldRenderAmap()) {
                getString(R.string.map_render_unavailable)
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
            uiState = uiState.copy(statusMessage = getString(R.string.map_status_long_press_destination))
        }

        if (!hasCenteredOnLocation) {
            hasCenteredOnLocation = true
            aMap?.animateCamera(
                CameraUpdateFactory.newLatLngZoom(
                    LatLng(newLocation.latitude, newLocation.longitude),
                    16f
                )
            )
        }
    }

    override fun onMapLongClick(latLng: LatLng?) {
        val clickedPoint = latLng ?: return
        if (routeStartPoint == null) {
            Toast.makeText(this, R.string.map_waiting_location_toast, Toast.LENGTH_SHORT).show()
            return
        }

        destinationPoint = LatLonPoint(clickedPoint.latitude, clickedPoint.longitude)
        uiState = uiState.copy(
            statusMessage = getString(R.string.map_status_searching_route),
            isSearchingRoute = true,
            routeSummary = null,
        )
        searchWalkRoute()
    }

    override fun onBusRouteSearched(result: BusRouteResult?, errorCode: Int) = Unit

    override fun onDriveRouteSearched(result: DriveRouteResult?, errorCode: Int) = Unit

    override fun onRideRouteSearched(result: RideRouteResult?, errorCode: Int) = Unit

    override fun onWalkRouteSearched(result: WalkRouteResult?, errorCode: Int) {
        if (errorCode != AMapException.CODE_AMAP_SUCCESS || result?.paths.isNullOrEmpty()) {
            routePolylinePoints = emptyList()
            uiState = uiState.copy(
                isSearchingRoute = false,
                routeSummary = null,
                statusMessage = getString(R.string.map_route_failed),
            )
            Toast.makeText(this, R.string.map_route_failed, Toast.LENGTH_SHORT).show()
            return
        }

        val walkPath = result?.paths?.firstOrNull() ?: run {
            uiState = uiState.copy(
                isSearchingRoute = false,
                routeSummary = null,
                statusMessage = getString(R.string.map_route_failed),
            )
            return
        }

        routeDurationSeconds = walkPath.duration.toInt()
        routePolylinePoints = buildPolylinePoints(
            start = result.startPos,
            walkPath = walkPath,
            end = result.targetPos,
        )
        renderRoute()

        uiState = uiState.copy(
            isSearchingRoute = false,
            routeSummary = MapRouteSummary(
                distanceText = formatDistance(walkPath.distance.toInt()),
                durationText = formatDuration(routeDurationSeconds),
            ),
            statusMessage = getString(R.string.map_route_ready_hint),
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
        } catch (exception: Exception) {
            Toast.makeText(this, exception.message ?: getString(R.string.map_init_failed), Toast.LENGTH_SHORT).show()
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
                statusMessage = getString(R.string.map_render_unavailable),
            )
            return
        }

        try {
            aMap?.isMyLocationEnabled = true
            uiState = uiState.copy(
                permissionGranted = true,
                statusMessage = if (routeSummaryAvailable()) {
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

    private fun searchWalkRoute() {
        val start = routeStartPoint ?: return
        val end = destinationPoint ?: return

        try {
            val fromAndTo = RouteSearch.FromAndTo(start, end)
            val query = RouteSearch.WalkRouteQuery(fromAndTo, RouteSearch.WalkDefault)
            routeSearch?.calculateWalkRouteAsyn(query)
        } catch (exception: Exception) {
            uiState = uiState.copy(
                isSearchingRoute = false,
                statusMessage = exception.message ?: getString(R.string.map_route_failed),
            )
        }
    }

    private fun renderRoute() {
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

        if (routePolylinePoints.isNotEmpty()) {
            map.addPolyline(
                PolylineOptions()
                    .addAll(routePolylinePoints)
                    .color(Color.parseColor("#3B82F6"))
                    .width(18f)
            )

            val boundsBuilder = LatLngBounds.Builder()
            routePolylinePoints.forEach(boundsBuilder::include)
            map.animateCamera(CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), 120))
        }
    }

    private fun startGuarding() {
        if (activeTripId != null || routePolylinePoints.isEmpty()) {
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

    private fun routeSummaryAvailable(): Boolean = uiState.routeSummary != null

    private fun shouldRenderAmap(): Boolean = !isRunningOnEmulator()

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
