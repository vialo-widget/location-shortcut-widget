package com.vialo.app.data.pairing

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ───────────────────────────────────────────────────────────────────────────
// Wire models — kept in lockstep with vialo-widget/vialo-sync. Field names
// must match the worker's JSON exactly.
// ───────────────────────────────────────────────────────────────────────────

@Serializable
internal data class RegisterRequest(
    @SerialName("device_id") val deviceId: String,
    @SerialName("fcm_token") val fcmToken: String? = null,
)

@Serializable
internal data class RegisterResponse(
    @SerialName("device_id") val deviceId: String,
    @SerialName("session_token") val sessionToken: String,
)

@Serializable
internal data class FcmTokenUpdate(@SerialName("fcm_token") val fcmToken: String)

@Serializable
internal data class CodeResponse(
    val code: String,
    @SerialName("expires_at") val expiresAt: Long,
)

@Serializable
internal data class RedeemRequest(
    val code: String,
    @SerialName("carer_display_name") val carerDisplayName: String,
    @SerialName("caree_display_name") val careeDisplayName: String,
)

@Serializable
internal data class RedeemResponse(
    @SerialName("pending_id") val pendingId: String,
    @SerialName("expires_at") val expiresAt: Long,
)

@Serializable
internal data class AcceptRejectRequest(@SerialName("pending_id") val pendingId: String)

@Serializable
internal data class AcceptResponse(@SerialName("pair_id") val pairId: String)

@Serializable
internal data class RenameRequest(
    @SerialName("carer_display_name") val carerDisplayName: String? = null,
    @SerialName("caree_display_name") val careeDisplayName: String? = null,
)

@Serializable
internal data class PairListResponse(
    @SerialName("as_caree") val asCaree: List<PairRow> = emptyList(),
    @SerialName("as_carer") val asCarer: List<PairRow> = emptyList(),
)

@Serializable
internal data class PairRow(
    val id: String,
    @SerialName("other_device_id") val otherDeviceId: String,
    @SerialName("other_display_name") val otherDisplayName: String,
    @SerialName("created_at") val createdAt: Long,
    val role: String, // "caree" | "carer"
)

@Serializable
internal data class InboxResponse(val inbox: List<InboxRow> = emptyList())

@Serializable
internal data class InboxRow(
    val id: String,
    @SerialName("carer_device_id") val carerDeviceId: String,
    @SerialName("carer_display_name") val carerDisplayName: String,
    @SerialName("expires_at") val expiresAt: Long,
)

@Serializable
internal data class PendingResponse(val pending: List<PendingRow> = emptyList())

@Serializable
internal data class PendingRow(
    val id: String,
    @SerialName("caree_device_id") val careeDeviceId: String,
    @SerialName("carer_display_name") val carerDisplayName: String,
    @SerialName("caree_display_name") val careeDisplayName: String,
    @SerialName("expires_at") val expiresAt: Long,
)

@Serializable
internal data class ErrorResponse(val error: String)

// ───────────────────────────────────────────────────────────────────────────
// Phase 2 — Shortcut sync wire shapes.
// RemoteShortcut mirrors the local Shortcut model field-for-field so the
// backend can store the snapshot opaquely and both peers decode identically.
// We pass timestamps as epoch-millis (matches the Room columns) rather
// than ISO strings to avoid two encoding hops.
// ───────────────────────────────────────────────────────────────────────────

@Serializable
data class RemoteShortcut(
    val id: String,
    val label: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    @SerialName("place_id") val placeId: String,
    @SerialName("icon_name") val iconName: String,
    @SerialName("sort_order") val sortOrder: Int,
    @SerialName("created_at_millis") val createdAtMillis: Long,
    @SerialName("expires_at_millis") val expiresAtMillis: Long? = null,
)

@Serializable
internal data class CareeStateRequest(val shortcuts: List<RemoteShortcut>)

@Serializable
internal data class CareeStateResponse(
    val shortcuts: List<RemoteShortcut> = emptyList(),
    @SerialName("updated_at") val updatedAt: Long = 0L,
)

@Serializable
internal data class CareeStateAck(
    val ok: Boolean = true,
    @SerialName("updated_at") val updatedAt: Long = 0L,
)

/** Domain shape returned to callers of [VialoApi.getCareeState]. */
data class CareeStateSnapshot(
    val shortcuts: List<RemoteShortcut>,
    val updatedAt: Long,
)

// ───────────────────────────────────────────────────────────────────────────
// Domain models — what callers (repository, viewmodels, UI) see.
// Independent of wire format so the API can evolve without ripple.
// ───────────────────────────────────────────────────────────────────────────

/** A live pairing where the calling device is either the caree or the carer. */
data class Pair(
    val id: String,
    val role: Role,
    val otherDeviceId: String,
    val otherDisplayName: String,
    val createdAt: Long,
) {
    enum class Role { CAREE, CARER }
}

/** A pending invite addressed to the caree — needs accept/reject. */
data class InboundInvite(
    val pendingId: String,
    val carerDeviceId: String,
    val carerDisplayName: String,
    val expiresAt: Long,
)

/** Carer-side: a redemption awaiting the caree's response. */
data class OutboundInvite(
    val pendingId: String,
    val careeDeviceId: String,
    val carerDisplayName: String,
    val careeDisplayName: String,
    val expiresAt: Long,
)

/** Caree-side: an active 6-digit code the carer needs to enter. */
data class ActiveCode(
    val code: String,
    val expiresAt: Long,
)

/** Result wrapper for API calls. Sealed so callers must handle both cases. */
sealed class ApiResult<out T> {
    data class Success<T>(val value: T) : ApiResult<T>()
    data class Failure(val code: Int, val message: String) : ApiResult<Nothing>()
}
