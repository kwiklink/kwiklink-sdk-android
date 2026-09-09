package io.kwiklink.android.sdk.internal.net

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

private fun fingerprint(model: String = "Pixel 6", screenWidthPx: Int = 1080) = computeDeviceFingerprint(
    manufacturer = "Google",
    model = model,
    brand = "google",
    device = "oriole",
    product = "oriole",
    board = "oriole",
    hardware = "oriole",
    buildFingerprint = "google/oriole/oriole:14/UP1A.231005.007/10754064:user/release-keys",
    osVersion = "14",
    apiLevel = 34,
    screenWidthPx = screenWidthPx,
    screenHeightPx = 2400,
    screenDensityDpi = 420,
    supportedAbis = listOf("arm64-v8a"),
)

class DeviceFingerprintTest {

    @Test
    fun `identical device attributes always produce the identical hash`() {
        assertEquals(fingerprint(), fingerprint())
    }

    @Test
    fun `a different device model changes the hash`() {
        assertNotEquals(fingerprint(model = "Pixel 6"), fingerprint(model = "SM-S911B"))
    }

    @Test
    fun `a different screen width changes the hash`() {
        assertNotEquals(fingerprint(screenWidthPx = 1080), fingerprint(screenWidthPx = 1440))
    }

    @Test
    fun `the hash is a 64-character lowercase hex string (sha-256)`() {
        val hash = fingerprint()
        assertEquals(64, hash.length)
        assertEquals(hash, hash.lowercase())
        assertEquals(true, hash.all { it in "0123456789abcdef" })
    }
}
