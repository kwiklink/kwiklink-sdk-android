package io.kwiklink.android.sdk.model

/**
 * The outcome of resolving a link, whether via [io.kwiklink.android.sdk.Kwiklink.resolveLinkFromIntent]
 * (warm open) or a future deferred-open path (Phase 2). [matched] is `false`
 * — not an exception — for the ordinary "this link doesn't exist / already
 * expired" case; [io.kwiklink.android.sdk.KwiklinkError] is reserved for exceptional
 * failures (network, server, bad config).
 */
data class AttributionResult(
    val matched: Boolean,
    val linkId: String? = null,
    val linkData: LinkData? = null,
) {
    companion object {
        internal fun unmatched() = AttributionResult(matched = false)
    }
}
