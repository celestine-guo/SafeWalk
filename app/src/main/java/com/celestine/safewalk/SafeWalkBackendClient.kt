package com.celestine.safewalk

import android.os.Handler
import android.os.Looper
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.time.OffsetDateTime
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

data class PathPointPayload(
    val latitude: Double,
    val longitude: Double,
)

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

data class TripResponsePayload(
    val id: Int,
    val status: String,
)

data class LocationUploadPayload(
    val tripId: Int,
    val latitude: Double,
    val longitude: Double,
    val accuracy: Double?,
    val recordedAt: String,
)

data class TripDetailPayload(
    val tripId: Int,
    val status: String,
)

sealed class BackendResult<out T> {
    data class Success<T>(val value: T) : BackendResult<T>()
    data class Error(val message: String) : BackendResult<Nothing>()
}

class SafeWalkBackendClient(
    private val executor: ExecutorService = Executors.newSingleThreadExecutor(),
    private val authTokenProvider: AuthTokenProvider = ClerkAuthTokenProvider(),
) {
    private val mainHandler = Handler(Looper.getMainLooper())
    private val baseUrl = BuildConfig.SAFEWALK_BACKEND_BASE_URL.trim().trimEnd('/')

    fun isConfigured(): Boolean = baseUrl.isNotBlank()

    fun createTrip(
        payload: TripCreatePayload,
        callback: (BackendResult<TripResponsePayload>) -> Unit,
    ) {
        execute(callback) {
            val body = JSONObject().apply {
                put("mode", payload.mode)
                put("start_latitude", payload.startLatitude)
                put("start_longitude", payload.startLongitude)
                put("destination_latitude", payload.destinationLatitude)
                put("destination_longitude", payload.destinationLongitude)
                put("planned_arrival_at", payload.plannedArrivalAt)
                put("planned_path", JSONArray().apply {
                    payload.plannedPath.forEach { point ->
                        put(
                            JSONObject().apply {
                                put("latitude", point.latitude)
                                put("longitude", point.longitude)
                            }
                        )
                    }
                })
                put("deviation_threshold_meters", payload.deviationThresholdMeters)
                put("timeout_grace_minutes", payload.timeoutGraceMinutes)
            }

            val response = request("/trips/", "POST", body.toString())
            val json = JSONObject(response)
            TripResponsePayload(
                id = json.getInt("id"),
                status = json.getString("status"),
            )
        }
    }

    fun uploadLocation(
        payload: LocationUploadPayload,
        callback: (BackendResult<Unit>) -> Unit,
    ) {
        execute(callback) {
            val body = JSONObject().apply {
                put("trip_id", payload.tripId)
                put("latitude", payload.latitude)
                put("longitude", payload.longitude)
                put("recorded_at", payload.recordedAt)
                payload.accuracy?.let { put("accuracy", it) }
            }

            request("/locations/", "POST", body.toString())
            Unit
        }
    }

    fun getTripDetail(
        tripId: Int,
        callback: (BackendResult<TripDetailPayload>) -> Unit,
    ) {
        execute(callback) {
            val response = request("/trips/$tripId", "GET")
            val json = JSONObject(response)
            val trip = json.getJSONObject("trip")
            TripDetailPayload(
                tripId = trip.getInt("id"),
                status = trip.getString("status"),
            )
        }
    }

    fun shutdown() {
        executor.shutdownNow()
    }

    private fun <T> execute(
        callback: (BackendResult<T>) -> Unit,
        task: () -> T,
    ) {
        if (!isConfigured()) {
            dispatch(callback, BackendResult.Error("请先配置 SAFEWALK_BACKEND_BASE_URL"))
            return
        }

        executor.execute {
            val result = try {
                BackendResult.Success(task())
            } catch (exception: Exception) {
                BackendResult.Error(exception.message ?: "请求失败")
            }
            dispatch(callback, result)
        }
    }

    private fun <T> dispatch(
        callback: (BackendResult<T>) -> Unit,
        result: BackendResult<T>,
    ) {
        mainHandler.post {
            callback(result)
        }
    }

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

        try {
            if (body != null) {
                OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use { writer ->
                    writer.write(body)
                }
            }

            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val responseText = stream?.use { input ->
                BufferedReader(InputStreamReader(input)).readText()
            }.orEmpty()

            if (code !in 200..299) {
                val detail = responseText.ifBlank { "HTTP $code" }
                throw IllegalStateException(detail)
            }

            return responseText
        } finally {
            connection.disconnect()
        }
    }

    companion object {
        fun nowIsoString(): String = OffsetDateTime.now().toString()
    }
}
