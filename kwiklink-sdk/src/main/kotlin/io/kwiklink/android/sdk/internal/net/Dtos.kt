package io.kwiklink.android.sdk.internal.net

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/** Mirrors `GET /android/v1/links/resolve`'s 200 response body — see openapi/openapi.yaml. */
@Serializable
internal data class ResolveLinkResponseDto(
    val linkId: String,
    val linkData: JsonObject = JsonObject(emptyMap()),
)

/**
 * Mirrors `POST /android/v1/attribution/match`'s request body. `clickId` empty
 * means "no click id available" — `HandleAttributionMatch` falls back to
 * IP+platform fingerprint matching in that case, so this is a deliberate,
 * meaningful value, not a placeholder.
 */
@Serializable
internal data class AttributionMatchRequestDto(
    val clickId: String = "",
    val platform: String = "android",
)

/** Mirrors both the 200 and 404 response bodies of `POST /android/v1/attribution/match`. */
@Serializable
internal data class AttributionMatchResponseDto(
    val matched: Boolean,
    val linkId: String? = null,
    val linkData: JsonObject? = null,
)
