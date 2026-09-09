package io.kwiklink.android.sdk.internal.net

import java.security.MessageDigest

/**
 * A composite hash of static device/OS/screen attributes — for fraud
 * detection, not identity: two installs producing the *same* fingerprint
 * signals "same device model/OS/screen config," useful for spotting
 * device-farm clones (many "different" installs on identical emulated
 * hardware) alongside other signals (IP, click timing, [DeviceIdProvider]'s
 * own id). It is deliberately not a stable per-device identifier on its
 * own — many real, distinct devices of the same model/OS/screen share one.
 *
 * Pure — [java.security.MessageDigest] is plain JDK, not an Android
 * framework dependency, so this is directly testable on the JVM with
 * explicit inputs, same reasoning as [buildAttributionHeaders]. The real
 * caller, [SdkAttributionInfo.initialize], feeds it actual `Build.*` and
 * `DisplayMetrics` values.
 */
internal fun computeDeviceFingerprint(
    manufacturer: String,
    model: String,
    brand: String,
    device: String,
    product: String,
    board: String,
    hardware: String,
    buildFingerprint: String,
    osVersion: String,
    apiLevel: Int,
    screenWidthPx: Int,
    screenHeightPx: Int,
    screenDensityDpi: Int,
    supportedAbis: List<String>,
): String {
    val canonical = listOf(
        manufacturer, model, brand, device, product, board, hardware, buildFingerprint,
        osVersion, apiLevel.toString(),
        screenWidthPx.toString(), screenHeightPx.toString(), screenDensityDpi.toString(),
        supportedAbis.joinToString(","),
    ).joinToString("|")

    val digest = MessageDigest.getInstance("SHA-256").digest(canonical.toByteArray(Charsets.UTF_8))
    return digest.joinToString(separator = "") { "%02x".format(it) }
}
