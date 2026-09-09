package io.kwiklink.android.sdk.internal.net

import okhttp3.Interceptor
import okhttp3.Response

// Mirrored on the backend by internal/redirectsvc/attribution_log.go's own
// copy of these exact names — keep both in sync if either changes.
internal const val HEADER_SDK_VERSION = "X-Kwiklink-Sdk-Version"
internal const val HEADER_SDK_PLATFORM = "X-Kwiklink-Sdk-Platform"
internal const val HEADER_OS_VERSION = "X-Kwiklink-Os-Version"
internal const val HEADER_API_LEVEL = "X-Kwiklink-Api-Level"
internal const val HEADER_DEVICE_MODEL = "X-Kwiklink-Device-Model"
internal const val HEADER_DEVICE_MANUFACTURER = "X-Kwiklink-Device-Manufacturer"
internal const val HEADER_APP_PACKAGE = "X-Kwiklink-App-Package"
internal const val HEADER_APP_VERSION = "X-Kwiklink-App-Version"

/**
 * Pure header-map builder — no `android.os.Build`/`Context` reference, so
 * it's directly exercisable by JVM unit tests with explicit values instead
 * of needing Robolectric (this project doesn't have it) or a real device,
 * same reasoning as [io.kwiklink.android.sdk.internal.link.IntentLinkParser.matchesConfiguredDomain]'s
 * extraction. [SdkAttributionInfo.initialize] is the only real caller,
 * feeding it actual `Build.*`/`PackageManager` values.
 */
internal fun buildAttributionHeaders(
    sdkVersion: String,
    osVersion: String,
    apiLevel: Int,
    deviceModel: String,
    deviceManufacturer: String,
    appPackage: String,
    appVersion: String?,
): Map<String, String> = buildMap {
    put(HEADER_SDK_VERSION, sdkVersion)
    put(HEADER_SDK_PLATFORM, "android")
    put(HEADER_OS_VERSION, osVersion)
    put(HEADER_API_LEVEL, apiLevel.toString())
    put(HEADER_DEVICE_MODEL, deviceModel)
    put(HEADER_DEVICE_MANUFACTURER, deviceManufacturer)
    put(HEADER_APP_PACKAGE, appPackage)
    if (appVersion != null) put(HEADER_APP_VERSION, appVersion)
}

/**
 * Attaches [SdkAttributionInfo.headers] (computed once, at
 * [io.kwiklink.android.sdk.Kwiklink.init]) to every request this SDK makes —
 * the backend logs these per-call, see
 * `backend/internal/redirectsvc/attribution_log.go`. Deliberately never
 * throws: a header-attachment failure must never take down an actual API
 * call, so any exception here just falls back to proceeding with the
 * request unmodified.
 */
internal class AttributionInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val withHeaders = try {
            val builder = original.newBuilder()
            SdkAttributionInfo.headers.forEach { (name, value) -> builder.header(name, value) }
            builder.build()
        } catch (e: Exception) {
            original
        }
        return chain.proceed(withHeaders)
    }
}
