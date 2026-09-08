package io.kwiklink.android.sdk.internal.referrer

import java.io.UnsupportedEncodingException
import java.net.URLDecoder

/**
 * Pulls `click_id` out of the raw Play Install Referrer string (the value
 * of the `referrer` query parameter `BuildTargetURL` embedded in the Play
 * Store URL at click time — Play delivers it back to the app, decoded,
 * as-is). A pure string parser — no `android.*` dependency — so it's
 * testable on the JVM without Robolectric, same reasoning as
 * `internal.link.IntentLinkParser`.
 */
internal object ReferrerParser {

    fun parseClickId(referrer: String): String? =
        referrer.splitToSequence('&')
            .mapNotNull { pair ->
                val i = pair.indexOf('=')
                if (i < 0) null else pair.substring(0, i) to pair.substring(i + 1)
            }
            .firstOrNull { (key, _) -> key == "click_id" }
            ?.second
            ?.let(::decodeOrOriginal)
            ?.takeIf { it.isNotBlank() }

    private fun decodeOrOriginal(value: String): String =
        try {
            URLDecoder.decode(value, "UTF-8")
        } catch (e: UnsupportedEncodingException) {
            value
        } catch (e: IllegalArgumentException) {
            // A malformed %-escape in an untrusted referrer string
            // shouldn't crash attribution — fall back to the raw value.
            value
        }
}
