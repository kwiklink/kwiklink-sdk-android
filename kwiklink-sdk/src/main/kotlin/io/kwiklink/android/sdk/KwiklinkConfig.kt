package io.kwiklink.android.sdk

/**
 * @param apiKey Sent as `X-Api-Key` on every call this SDK makes (`GET
 *   /android/v1/links/resolve`, `POST /android/v1/attribution/match`) —
 *   the same key created for this app under App Details -> API Keys in the
 *   dashboard. Note what this does and doesn't buy: this key ships inside
 *   a distributable APK, so it's trivially recoverable by decompiling one
 *   (same non-secret "app identifier" role Branch's `branch_key` or
 *   AppsFlyer's `dev_key` play) — it stops a blind stranger from
 *   enumerating links across the whole platform for free, and enables
 *   per-key rate limiting/revocation, but it is not a secret and does not
 *   prove the caller is genuinely this app. An invalid, disabled, or
 *   revoked key fails with [io.kwiklink.android.sdk.model.KwiklinkError.Server]
 *   (HTTP 401).
 * @param loggingEnabled Off by default — a host app's Logcat shouldn't
 *   gain noise it never asked for. Turn on to see this SDK's own
 *   `Kwiklink`-tagged lines while integrating (referrer-read outcomes,
 *   resolve/attribution-match calls and their result). What's deliberately
 *   never logged, on or off: the API key, a click id, a domain+slug pair,
 *   any `linkData` value, or the raw referrer string — see
 *   [io.kwiklink.android.sdk.internal.log.KwiklinkLog]'s own doc comment for why.
 */
data class KwiklinkConfig(
    val apiKey: String,
    val loggingEnabled: Boolean = false,
)
