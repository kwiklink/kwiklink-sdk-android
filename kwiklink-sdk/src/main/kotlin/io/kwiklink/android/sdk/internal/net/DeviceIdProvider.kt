package io.kwiklink.android.sdk.internal.net

import android.content.Context
import android.provider.Settings
import java.util.UUID

/**
 * Thin `Context`-touching wrapper around [resolveDeviceId] — reads the real
 * Android ID and this SDK's own persisted fallback (if any), and writes a
 * freshly generated fallback back so it's reused on the next call instead
 * of changing every time. Left untested at JVM level (no Robolectric in
 * this project); [resolveDeviceId] carries the actual decision logic and
 * is what's unit-tested.
 */
internal object DeviceIdProvider {
    private const val PREFS_NAME = "kwiklink_sdk_prefs"
    private const val KEY_DEVICE_ID = "device_id"

    fun resolve(context: Context): ResolvedDeviceId {
        val androidId = try {
            Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
        } catch (e: Exception) {
            null
        }
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val result = resolveDeviceId(
            androidId = androidId,
            storedFallback = prefs.getString(KEY_DEVICE_ID, null),
            generateFallback = { UUID.randomUUID().toString() },
        )
        if (result.shouldPersist) {
            prefs.edit().putString(KEY_DEVICE_ID, result.id).apply()
        }
        return result
    }
}
