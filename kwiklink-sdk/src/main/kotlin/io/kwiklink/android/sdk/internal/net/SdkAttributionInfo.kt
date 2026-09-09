package io.kwiklink.android.sdk.internal.net

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import io.kwiklink.android.sdk.BuildConfig

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

    fun initialize(context: Context) {
        headers = buildAttributionHeaders(
            sdkVersion = BuildConfig.SDK_VERSION,
            osVersion = Build.VERSION.RELEASE ?: "unknown",
            apiLevel = Build.VERSION.SDK_INT,
            deviceModel = Build.MODEL ?: "unknown",
            deviceManufacturer = Build.MANUFACTURER ?: "unknown",
            appPackage = context.packageName,
            appVersion = appVersionName(context),
        )
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
