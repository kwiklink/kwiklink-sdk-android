package io.kwiklink.android.sdk.internal.net

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import io.kwiklink.android.sdk.BuildConfig
import io.kwiklink.android.sdk.internal.log.KwiklinkLog

/**
 * Snapshot of "who's calling" — this SDK's own version, the host app, and
 * the device — computed once at [io.kwiklink.android.sdk.Kwiklink.init]
 * (a `PackageManager` lookup isn't worth redoing on every single API call)
 * and attached to every request by [AttributionInterceptor]. A plain
 * settable `var`, not a private-set snapshot, so tests can seed it directly
 * without a real [Context] — same pattern as
 * [io.kwiklink.android.sdk.internal.log.KwiklinkLog]'s `enabled`/`minLevel`.
 */
internal object SdkAttributionInfo {
    @Volatile
    internal var headers: Map<String, String> = emptyMap()

    /**
     * Computed once, here, and cached in [headers] for every later request —
     * never recomputed per call. Wrapped end-to-end: a `SharedPreferences`/
     * `Settings.Secure` failure inside [DeviceIdProvider.resolve] (a locked-
     * down storage environment, e.g.) must not propagate out of
     * [io.kwiklink.android.sdk.Kwiklink.init], since that's called from the
     * host app's own `Application.onCreate()` — an uncaught exception there
     * crashes the host app's startup, a far worse outcome than a request
     * simply going out without attribution headers this session.
     */
    fun initialize(context: Context) {
        headers = try {
            val metrics = context.resources.displayMetrics
            val deviceId = DeviceIdProvider.resolve(context)
            buildAttributionHeaders(
                AttributionInfoInput(
                    sdkVersion = BuildConfig.SDK_VERSION,
                    osVersion = Build.VERSION.RELEASE ?: "unknown",
                    apiLevel = Build.VERSION.SDK_INT,
                    deviceModel = Build.MODEL ?: "unknown",
                    deviceManufacturer = Build.MANUFACTURER ?: "unknown",
                    appPackage = context.packageName,
                    appVersion = appVersionName(context),
                    screenWidthPx = metrics.widthPixels,
                    screenHeightPx = metrics.heightPixels,
                    screenDensityDpi = metrics.densityDpi,
                    deviceFingerprint = computeDeviceFingerprint(
                        manufacturer = Build.MANUFACTURER ?: "unknown",
                        model = Build.MODEL ?: "unknown",
                        brand = Build.BRAND ?: "unknown",
                        device = Build.DEVICE ?: "unknown",
                        product = Build.PRODUCT ?: "unknown",
                        board = Build.BOARD ?: "unknown",
                        hardware = Build.HARDWARE ?: "unknown",
                        buildFingerprint = Build.FINGERPRINT ?: "unknown",
                        osVersion = Build.VERSION.RELEASE ?: "unknown",
                        apiLevel = Build.VERSION.SDK_INT,
                        screenWidthPx = metrics.widthPixels,
                        screenHeightPx = metrics.heightPixels,
                        screenDensityDpi = metrics.densityDpi,
                        supportedAbis = Build.SUPPORTED_ABIS?.toList() ?: emptyList(),
                    ),
                    deviceId = deviceId.id,
                    deviceIdSource = deviceId.source,
                ),
            )
        } catch (e: Exception) {
            KwiklinkLog.e("Failed to compute attribution info — requests will go out without it", e)
            emptyMap()
        }
    }

    // A host app's own package can, in principle, not resolve via its own
    // PackageManager (a broken instrumented-test harness, e.g.) — fail soft
    // to a missing header rather than crashing SDK init over an app-version
    // string that's a nice-to-have, not a requirement.
    private fun appVersionName(context: Context): String? = try {
        @Suppress("DEPRECATION")
        context.packageManager.getPackageInfo(context.packageName, 0).versionName
    } catch (e: PackageManager.NameNotFoundException) {
        null
    }
}
