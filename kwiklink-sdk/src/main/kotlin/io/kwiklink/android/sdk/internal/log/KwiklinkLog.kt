package io.kwiklink.android.sdk.internal.log

import android.util.Log
import io.kwiklink.android.sdk.KwiklinkLogLevel

/**
 * Every log line this SDK ever emits goes through here. Opt-in
 * ([enabled] is set from [io.kwiklink.android.sdk.KwiklinkConfig.loggingEnabled] at
 * [io.kwiklink.android.sdk.Kwiklink.init] time, default `false`, and
 * toggleable anytime via [io.kwiklink.android.sdk.Kwiklink.enableLogging])
 * — a host app's Logcat shouldn't gain noise it never asked for, and
 * Logcat output is routinely captured by bug-report tools, crash
 * reporters, and (on a rooted device) other apps, so this is also the one
 * place that decides what's safe to say out loud. [minLevel] filters
 * further *within* that opt-in — see [KwiklinkLogLevel].
 *
 * Never pass, even with logging enabled:
 *   - the API key ([io.kwiklink.android.sdk.KwiklinkConfig.apiKey], sent as the
 *     `X-Api-Key` header on every call) — the whole point of it being a
 *     header is that it doesn't otherwise appear in anything log-shaped.
 *   - a click id, a domain+slug pair, or any `linkData` field/value — the
 *     backend's own API docs call these "not secrets" in the sense that no
 *     credential is needed to redeem/resolve them, but that's an
 *     authorization statement, not a privacy one: a slug can identify a
 *     specific marketing campaign, a click id a specific install event.
 *     Log *that* a lookup happened and *what it resolved to* (matched or
 *     not, an HTTP status, a latency), never the values themselves.
 *   - the raw install-referrer string — it can carry more than just
 *     `click_id` depending on what a customer's ad network attached to it.
 */
internal object KwiklinkLog {
    private const val TAG = "Kwiklink"

    @Volatile
    internal var enabled: Boolean = false

    @Volatile
    internal var minLevel: KwiklinkLogLevel = KwiklinkLogLevel.DEBUG

    /**
     * Split out from `d`/`w`/`e` so the actual on/off + level decision is
     * testable on the JVM without touching `android.util.Log` — same
     * reasoning as [io.kwiklink.android.sdk.internal.link.IntentLinkParser.parseUri].
     */
    internal fun shouldLog(level: KwiklinkLogLevel): Boolean = enabled && level >= minLevel

    fun d(message: String) {
        if (shouldLog(KwiklinkLogLevel.DEBUG)) Log.d(TAG, message)
    }

    fun w(message: String, throwable: Throwable? = null) {
        if (shouldLog(KwiklinkLogLevel.WARN)) Log.w(TAG, message, throwable)
    }

    fun e(message: String, throwable: Throwable? = null) {
        if (shouldLog(KwiklinkLogLevel.ERROR)) Log.e(TAG, message, throwable)
    }
}
