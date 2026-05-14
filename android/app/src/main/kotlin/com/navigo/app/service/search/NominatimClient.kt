package com.navigo.app.service.search

import android.util.Log
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Free OpenStreetMap Nominatim client — forward search (autocomplete) and
 * reverse geocoding (lat/lng → human-readable label).
 *
 * Usage policy:
 *   - max 1 request/second per IP (caller debounces; we don't bake that in)
 *   - descriptive User-Agent is mandatory (set via defaultRequest below)
 *   - app must show OSM attribution near search results
 *
 * Forward results are in [PlaceResult]; reverse results return a short
 * "Road, City"-style label (or a "lat, lng" fallback on failure).
 */
class NominatimClient(
    userAgent: String = DEFAULT_USER_AGENT,
) {
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    private val http: HttpClient = HttpClient(Android) {
        install(ContentNegotiation) { json(json) }
        defaultRequest {
            headers.append(HttpHeaders.UserAgent, userAgent)
            headers.append(HttpHeaders.Accept, "application/json")
        }
        expectSuccess = false
    }

    suspend fun search(query: String, limit: Int = 5): List<PlaceResult> {
        if (query.length < 3) return emptyList()
        return try {
            val response: HttpResponse = http.get("$BASE_URL/search") {
                parameter("q", query)
                parameter("format", "json")
                parameter("limit", limit)
                parameter("addressdetails", 0)
            }
            if (!response.status.isSuccess()) {
                Log.w(TAG, "Search HTTP ${response.status}")
                return emptyList()
            }
            response.body<List<SearchDto>>().mapNotNull { it.toPlaceResult() }
        } catch (e: Exception) {
            Log.w(TAG, "Search failed for '$query'", e)
            emptyList()
        }
    }

    /** Returns a short address label, or "lat, lng" as a last-resort fallback. */
    suspend fun reverse(latitude: Double, longitude: Double): String {
        return try {
            val response: HttpResponse = http.get("$BASE_URL/reverse") {
                parameter("lat", latitude)
                parameter("lon", longitude)
                parameter("format", "json")
                parameter("addressdetails", 1)
            }
            if (!response.status.isSuccess()) {
                Log.w(TAG, "Reverse HTTP ${response.status}")
                return fallbackLabel(latitude, longitude)
            }
            val body = response.body<ReverseDto>()
            body.address?.shortLabel()
                ?: body.displayName?.split(",")?.take(2)?.joinToString(",")?.trim()?.takeIf { it.isNotBlank() }
                ?: fallbackLabel(latitude, longitude)
        } catch (e: Exception) {
            Log.w(TAG, "Reverse failed for $latitude,$longitude", e)
            fallbackLabel(latitude, longitude)
        }
    }

    private fun fallbackLabel(lat: Double, lng: Double): String = "%.5f, %.5f".format(lat, lng)

    @Serializable
    private data class SearchDto(
        @SerialName("osm_id") val osmId: Long? = null,
        @SerialName("place_id") val placeId: Long? = null,
        @SerialName("display_name") val displayName: String,
        val lat: String,
        val lon: String,
    ) {
        fun toPlaceResult(): PlaceResult? {
            val latD = lat.toDoubleOrNull() ?: return null
            val lonD = lon.toDoubleOrNull() ?: return null
            // Filter out the bad 0,0 results Nominatim occasionally returns.
            if (latD == 0.0 && lonD == 0.0) return null
            return PlaceResult(
                placeId = (osmId ?: placeId)?.toString().orEmpty(),
                displayName = displayName,
                latitude = latD,
                longitude = lonD,
            )
        }
    }

    @Serializable
    private data class ReverseDto(
        @SerialName("display_name") val displayName: String? = null,
        val address: AddressDto? = null,
    )

    @Serializable
    private data class AddressDto(
        val road: String? = null,
        val suburb: String? = null,
        val city: String? = null,
        val town: String? = null,
        val village: String? = null,
        val county: String? = null,
    ) {
        /** Take the first two non-blank parts of (road, suburb, city, town, village, county). */
        fun shortLabel(): String? {
            val parts = listOfNotNull(road, suburb, city, town, village, county)
                .filter { it.isNotBlank() }
                .take(2)
            return parts.takeIf { it.isNotEmpty() }?.joinToString(", ")
        }
    }

    private companion object {
        const val BASE_URL = "https://nominatim.openstreetmap.org"
        const val DEFAULT_USER_AGENT = "NaviGo/2.0 (android; contact@navigo.app)"
        const val TAG = "NominatimClient"
    }
}

/** Forward-search hit shown in autocomplete. */
data class PlaceResult(
    val placeId: String,
    val displayName: String,
    val latitude: Double,
    val longitude: Double,
)
