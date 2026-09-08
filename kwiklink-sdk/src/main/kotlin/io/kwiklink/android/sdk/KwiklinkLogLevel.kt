package io.kwiklink.android.sdk

/**
 * How verbose this SDK's own Logcat output is, once
 * [Kwiklink.enableLogging] has turned it on at all. Ordered least to most
 * severe, matching the three levels
 * [io.kwiklink.android.sdk.internal.log.KwiklinkLog] actually uses
 * (`Log.d`/`Log.w`/`Log.e`) one-to-one — there's no `VERBOSE` or `INFO`
 * here because nothing in this SDK logs at those levels. Setting [WARN]
 * suppresses [DEBUG] lines but still shows [WARN]/[ERROR] ones; [ERROR]
 * shows only [ERROR] lines.
 */
enum class KwiklinkLogLevel {
    DEBUG,
    WARN,
    ERROR,
}
