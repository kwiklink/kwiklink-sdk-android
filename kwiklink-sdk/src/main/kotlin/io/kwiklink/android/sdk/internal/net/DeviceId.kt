package io.kwiklink.android.sdk.internal.net

internal data class ResolvedDeviceId(val id: String, val source: String, val shouldPersist: Boolean) {
    companion object {
        const val SOURCE_ANDROID_ID = "android_id"
        const val SOURCE_GENERATED = "generated"
    }
}

/**
 * Android ID first — no permission, no dependency, no Play policy
 * obligation — falling back to a self-generated id only when it's
 * null/blank (some OEM devices return that). The fallback is itself
 * checked against [storedFallback] first so the same generated id survives
 * across calls/process restarts instead of changing every time; only a
 * genuinely first-ever fallback sets [ResolvedDeviceId.shouldPersist] so
 * [DeviceIdProvider] knows to write it once.
 *
 * Pure — takes the already-read Android ID and stored fallback as plain
 * values, and [generateFallback] as an injectable side effect, so this is
 * directly testable on the JVM without `Context`/`Settings.Secure`/
 * `SharedPreferences`, same reasoning as [buildAttributionHeaders]. The
 * real caller, [DeviceIdProvider], supplies actual values and
 * `{ UUID.randomUUID().toString() }`.
 */
internal fun resolveDeviceId(
    androidId: String?,
    storedFallback: String?,
    generateFallback: () -> String,
): ResolvedDeviceId {
    if (!androidId.isNullOrBlank()) {
        return ResolvedDeviceId(androidId, ResolvedDeviceId.SOURCE_ANDROID_ID, shouldPersist = false)
    }
    if (!storedFallback.isNullOrBlank()) {
        return ResolvedDeviceId(storedFallback, ResolvedDeviceId.SOURCE_GENERATED, shouldPersist = false)
    }
    return ResolvedDeviceId(generateFallback(), ResolvedDeviceId.SOURCE_GENERATED, shouldPersist = true)
}
