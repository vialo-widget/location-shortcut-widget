package com.vialo.app.data.pairing

import android.util.Log
import com.vialo.app.BuildConfig
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/**
 * Talks to the Vialo paired-sync backend (vialo-widget/vialo-sync).
 *
 * Adds the bearer token + X-Device-Id headers automatically by reading from
 * [DeviceIdentity]. Unauthenticated endpoints (`/device/register`) bypass
 * the auth headers naturally — they just won't appear on the request.
 *
 * Callers get [ApiResult.Success] or [ApiResult.Failure]; we never throw on
 * a non-2xx — that's a normal control-flow signal here (e.g. "code expired").
 */
class VialoApi(
    private val identity: DeviceIdentity,
    private val baseUrl: String = BuildConfig.BACKEND_URL,
) {
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    private val http: HttpClient = HttpClient(Android) {
        install(ContentNegotiation) { json(json) }
        defaultRequest {
            headers.append(HttpHeaders.Accept, "application/json")
        }
        expectSuccess = false
    }

    // ─── Device ────────────────────────────────────────────────────────────

    /**
     * First-time registration (or token rotation). Stores the returned
     * session token in [DeviceIdentity] before returning.
     */
    suspend fun registerDevice(fcmToken: String? = null): ApiResult<Unit> {
        return request<RegisterResponse> {
            http.post("$baseUrl/device/register") {
                contentType(ContentType.Application.Json)
                setBody(RegisterRequest(identity.deviceId, fcmToken))
            }
        }.map {
            identity.saveSessionToken(it.sessionToken)
            Unit
        }
    }

    suspend fun updateFcmToken(fcmToken: String): ApiResult<Unit> =
        request<Unit> {
            http.patch("$baseUrl/device/fcm") {
                authHeaders()
                contentType(ContentType.Application.Json)
                setBody(FcmTokenUpdate(fcmToken))
            }
        }

    // ─── Pairing ───────────────────────────────────────────────────────────

    /** Caree: generate a fresh 6-digit code. Invalidates any previous code. */
    suspend fun generateCode(): ApiResult<ActiveCode> =
        request<CodeResponse> {
            http.post("$baseUrl/pair/code") { authHeaders() }
        }.map { ActiveCode(it.code, it.expiresAt) }

    /** Carer: redeem a code with both display names. */
    suspend fun redeemCode(
        code: String,
        carerDisplayName: String,
        careeDisplayName: String,
    ): ApiResult<OutboundInvite> =
        request<RedeemResponse> {
            http.post("$baseUrl/pair/redeem") {
                authHeaders()
                contentType(ContentType.Application.Json)
                setBody(RedeemRequest(code, carerDisplayName, careeDisplayName))
            }
        }.map {
            OutboundInvite(
                pendingId = it.pendingId,
                careeDeviceId = "", // server doesn't return this in v1; carer-side UI doesn't need it
                carerDisplayName = carerDisplayName,
                careeDisplayName = careeDisplayName,
                expiresAt = it.expiresAt,
            )
        }

    /** Caree: accept a pending invite by id. */
    suspend fun accept(pendingId: String): ApiResult<String> =
        request<AcceptResponse> {
            http.post("$baseUrl/pair/accept") {
                authHeaders()
                contentType(ContentType.Application.Json)
                setBody(AcceptRejectRequest(pendingId))
            }
        }.map { it.pairId }

    /** Caree: reject a pending invite by id. */
    suspend fun reject(pendingId: String): ApiResult<Unit> =
        request<Unit> {
            http.post("$baseUrl/pair/reject") {
                authHeaders()
                contentType(ContentType.Application.Json)
                setBody(AcceptRejectRequest(pendingId))
            }
        }

    /** Either side: end a pair. Server pushes notification to the other side. */
    suspend fun unpair(pairId: String): ApiResult<Unit> =
        request<Unit> {
            http.delete("$baseUrl/pair/$pairId") { authHeaders() }
        }

    /** Carer only: rename either of the two display strings on the pair. */
    suspend fun rename(
        pairId: String,
        carerDisplayName: String? = null,
        careeDisplayName: String? = null,
    ): ApiResult<Unit> =
        request<Unit> {
            http.patch("$baseUrl/pair/$pairId/names") {
                authHeaders()
                contentType(ContentType.Application.Json)
                setBody(RenameRequest(carerDisplayName, careeDisplayName))
            }
        }

    /** All pairs for this device — split into caree-role and carer-role. */
    suspend fun listPairs(): ApiResult<Pair2Lists> =
        request<PairListResponse> {
            http.get("$baseUrl/pair/list") { authHeaders() }
        }.map { resp ->
            Pair2Lists(
                asCaree = resp.asCaree.map { it.toDomain(Pair.Role.CAREE) },
                asCarer = resp.asCarer.map { it.toDomain(Pair.Role.CARER) },
            )
        }

    /** Caree-side: any pending invites addressed to me. */
    suspend fun inbox(): ApiResult<List<InboundInvite>> =
        request<InboxResponse> {
            http.get("$baseUrl/pair/inbox") { authHeaders() }
        }.map { resp -> resp.inbox.map { it.toDomain() } }

    /** Carer-side: any redemptions of mine awaiting accept. */
    suspend fun pending(): ApiResult<List<OutboundInvite>> =
        request<PendingResponse> {
            http.get("$baseUrl/pair/pending") { authHeaders() }
        }.map { resp -> resp.pending.map { it.toDomain() } }

    // ─── Internals ─────────────────────────────────────────────────────────

    /** A typed wrapper for endpoints that return non-empty JSON. */
    private suspend inline fun <reified T> request(
        crossinline call: suspend () -> HttpResponse,
    ): ApiResult<T> {
        return try {
            val response = call()
            if (response.status.isSuccess()) {
                ApiResult.Success(response.body<T>())
            } else {
                val msg = runCatching { response.body<ErrorResponse>().error }
                    .getOrElse { response.bodyAsText().take(200) }
                ApiResult.Failure(response.status.value, msg)
            }
        } catch (e: Exception) {
            Log.w(TAG, "API call failed", e)
            ApiResult.Failure(0, e.message ?: "network error")
        }
    }

    /** Overload for endpoints whose 200 body we don't care about. */
    @JvmName("requestVoid")
    private suspend inline fun request(
        crossinline call: suspend () -> HttpResponse,
    ): ApiResult<Unit> {
        return try {
            val response = call()
            if (response.status.isSuccess()) {
                ApiResult.Success(Unit)
            } else {
                val msg = runCatching { response.body<ErrorResponse>().error }
                    .getOrElse { response.bodyAsText().take(200) }
                ApiResult.Failure(response.status.value, msg)
            }
        } catch (e: Exception) {
            Log.w(TAG, "API call failed", e)
            ApiResult.Failure(0, e.message ?: "network error")
        }
    }

    private fun io.ktor.client.request.HttpRequestBuilder.authHeaders() {
        val token = identity.sessionToken
            ?: throw IllegalStateException(
                "VialoApi authenticated call before device registered. " +
                    "Call registerDevice() first or check DeviceIdentity.isRegistered."
            )
        headers {
            append(HttpHeaders.Authorization, "Bearer $token")
            append("X-Device-Id", identity.deviceId)
        }
    }

    private inline fun <T, R> ApiResult<T>.map(block: (T) -> R): ApiResult<R> = when (this) {
        is ApiResult.Success -> ApiResult.Success(block(value))
        is ApiResult.Failure -> this
    }

    private fun PairRow.toDomain(role: Pair.Role) = Pair(
        id = id,
        role = role,
        otherDeviceId = otherDeviceId,
        otherDisplayName = otherDisplayName,
        createdAt = createdAt,
    )

    private fun InboxRow.toDomain() = InboundInvite(
        pendingId = id,
        carerDeviceId = carerDeviceId,
        carerDisplayName = carerDisplayName,
        expiresAt = expiresAt,
    )

    private fun PendingRow.toDomain() = OutboundInvite(
        pendingId = id,
        careeDeviceId = careeDeviceId,
        carerDisplayName = carerDisplayName,
        careeDisplayName = careeDisplayName,
        expiresAt = expiresAt,
    )

    private companion object {
        const val TAG = "VialoApi"
    }
}

/** Result of `/pair/list` — split by role for convenience. */
data class Pair2Lists(
    val asCaree: List<Pair>,
    val asCarer: List<Pair>,
)
