package io.kwiklink.android.sdk.internal.link

import io.kwiklink.android.sdk.internal.net.API_HOST
import io.kwiklink.android.sdk.internal.net.NetClient
import io.kwiklink.android.sdk.internal.net.NetResult
import io.kwiklink.android.sdk.internal.net.ResolveLinkResponseDto
import io.kwiklink.android.sdk.model.AttributionResult
import io.kwiklink.android.sdk.model.LinkData
import okhttp3.HttpUrl.Companion.toHttpUrl

/**
 * Calls the warm-open resolve endpoint (`GET /android/v1/links/resolve`,
 * added in backend Phase 0 — see docs/android-sdk-plan.md), always against
 * the fixed [API_HOST] — `domain` travels only as a query parameter,
 * identifying which app/link to look up regardless of which customer
 * short-link domain the tapped intent actually belonged to.
 */
internal class LinkResolver(private val net: NetClient = NetClient()) {

    suspend fun resolve(domain: String, slug: String, apiKey: String): AttributionResult {
        val url = "https://$API_HOST/android/v1/links/resolve".toHttpUrl().newBuilder()
            .addQueryParameter("domain", domain)
            .addQueryParameter("slug", slug)
            .build()

        return when (val result = net.get(url, apiKey, ResolveLinkResponseDto.serializer())) {
            is NetResult.Success -> AttributionResult(
                matched = true,
                linkId = result.value.linkId,
                linkData = LinkData(result.value.linkData),
            )
            NetResult.NotFound -> AttributionResult.unmatched()
        }
    }
}
