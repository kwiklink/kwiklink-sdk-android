package io.kwiklink.android.sdk.internal.net

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

private fun testInput(appVersion: String? = "1.2.3") = AttributionInfoInput(
    sdkVersion = "0.1.0",
    osVersion = "14",
    apiLevel = 34,
    deviceModel = "Pixel 6",
    deviceManufacturer = "Google",
    appPackage = "io.kwiklink.sample",
    appVersion = appVersion,
    screenWidthPx = 1080,
    screenHeightPx = 2400,
    screenDensityDpi = 420,
    deviceFingerprint = "abc123fingerprint",
    deviceId = "device-id-1",
    deviceIdSource = ResolvedDeviceId.SOURCE_ANDROID_ID,
)

class AttributionHeadersTest {

    @Test
    fun `builds one header per field, using the real header names`() {
        val headers = buildAttributionHeaders(testInput())

        assertEquals(
            mapOf(
                HEADER_SDK_VERSION to "0.1.0",
                HEADER_SDK_PLATFORM to "android",
                HEADER_OS_VERSION to "14",
                HEADER_API_LEVEL to "34",
                HEADER_DEVICE_MODEL to "Pixel 6",
                HEADER_DEVICE_MANUFACTURER to "Google",
                HEADER_APP_PACKAGE to "io.kwiklink.sample",
                HEADER_APP_VERSION to "1.2.3",
                HEADER_SCREEN_WIDTH to "1080",
                HEADER_SCREEN_HEIGHT to "2400",
                HEADER_SCREEN_DENSITY to "420",
                HEADER_DEVICE_FINGERPRINT to "abc123fingerprint",
                HEADER_DEVICE_ID to "device-id-1",
                HEADER_DEVICE_ID_SOURCE to "android_id",
            ),
            headers,
        )
    }

    @Test
    fun `sdk platform is always android, regardless of input`() {
        val headers = buildAttributionHeaders(testInput(appVersion = null))

        assertEquals("android", headers[HEADER_SDK_PLATFORM])
    }

    @Test
    fun `a null app version omits the header instead of sending a blank one`() {
        val headers = buildAttributionHeaders(testInput(appVersion = null))

        assertFalse(headers.containsKey(HEADER_APP_VERSION))
    }

    @Test
    fun `api level is sent as a plain decimal string`() {
        val headers = buildAttributionHeaders(testInput())

        assertEquals("34", headers[HEADER_API_LEVEL])
    }

    @Test
    fun `screen dimensions are sent as plain decimal strings`() {
        val headers = buildAttributionHeaders(testInput())

        assertEquals("1080", headers[HEADER_SCREEN_WIDTH])
        assertEquals("2400", headers[HEADER_SCREEN_HEIGHT])
        assertEquals("420", headers[HEADER_SCREEN_DENSITY])
    }

    @Test
    fun `device id source of generated is sent verbatim, not translated`() {
        val headers = buildAttributionHeaders(
            testInput().copy(deviceId = "generated-uuid", deviceIdSource = ResolvedDeviceId.SOURCE_GENERATED),
        )

        assertEquals("generated-uuid", headers[HEADER_DEVICE_ID])
        assertEquals("generated", headers[HEADER_DEVICE_ID_SOURCE])
    }
}
