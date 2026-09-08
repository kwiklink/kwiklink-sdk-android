package io.kwiklink.android.sdk.internal.autoinit

import io.kwiklink.android.sdk.KwiklinkConfig

/**
 * Decides what (if anything) [io.kwiklink.android.sdk.Kwiklink] should be
 * auto-initialized with, given the two manifest `<meta-data>` values
 * [KwiklinkInitProvider] reads from a real [android.os.Bundle]. Takes plain
 * `Boolean`/`String?` rather than a `Bundle` so this is testable on the JVM
 * without Robolectric — same reasoning as
 * [io.kwiklink.android.sdk.internal.link.IntentLinkParser.parseUri].
 *
 * Opt-in only: [autoInit] absent from a host app's manifest defaults to
 * `false` at the call site ([KwiklinkInitProvider]'s
 * `metaData.getBoolean(META_AUTO_INIT, false)`), so an app that has never
 * heard of this feature is completely unaffected by this provider existing.
 */
internal object KwiklinkAutoInit {
    const val META_AUTO_INIT = "io.kwiklink.android.sdk.AutoInit"
    const val META_API_KEY = "io.kwiklink.android.sdk.ApiKey"
    const val META_LOGGING_ENABLED = "io.kwiklink.android.sdk.LoggingEnabled"

    /**
     * `null` means "don't auto-init" — either [autoInit] wasn't requested,
     * or it was requested but misconfigured (blank/missing key). Callers
     * that care about telling those two cases apart (to decide whether to
     * log a misconfiguration warning) check [autoInit] themselves; this
     * function only signals the outcome, not the reason.
     */
    fun configFor(autoInit: Boolean, apiKey: String?, loggingEnabled: Boolean): KwiklinkConfig? {
        if (!autoInit || apiKey.isNullOrBlank()) return null
        return KwiklinkConfig(apiKey = apiKey, loggingEnabled = loggingEnabled)
    }
}
