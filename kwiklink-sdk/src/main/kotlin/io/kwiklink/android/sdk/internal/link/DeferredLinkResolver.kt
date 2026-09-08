package io.kwiklink.android.sdk.internal.link

import io.kwiklink.android.sdk.internal.net.API_HOST
import io.kwiklink.android.sdk.internal.net.AttributionMatchRequestDto
import io.kwiklink.android.sdk.internal.net.AttributionMatchResponseDto
import io.kwiklink.android.sdk.internal.net.NetClient
import io.kwiklink.android.sdk.model.AttributionResult
import io.kwiklink.android.sdk.model.LinkData
import okhttp3.HttpUrl.Companion.toHttpUrl

/**
 * Calls `POST /android/v1/attribution/match` to claim a deferred deep link
 * on cold start, always against the fixed [API_HOST]. Unlike the warm
 * path, this never needed a domain at all — `HandleAttributionMatch`
 * matches purely on `clickId`/IP+platform fingerprint, with no per-domain
 * scoping — so there's nothing here for [io.kwiklink.android.sdk.KwiklinkConfig.domain]
 * to configure.
 */
internal class DeferredLinkResolver(private val net: NetClient = NetClient()) {

    suspend fun resolve(clickId: String?, apiKey: String): AttributionResult {
        val url = "https://$API_HOST/android/v1/attribution/match".toHttpUrl()
        val dto = net.post(
            url,
            apiKey,
            AttributionMatchRequestDto(clickId = clickId.orEmpty()),
            AttributionMatchRequestDto.serializer(),
            AttributionMatchResponseDto.serializer(),
        )
        if (!dto.matched) return AttributionResult.unmatched()
        return AttributionResult(
            matched = true,
            linkId = dto.linkId,
            linkData = dto.linkData?.let { LinkData(it) },
        )
    }
}
