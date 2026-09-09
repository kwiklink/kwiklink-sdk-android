package io.kwiklink.android.sdk.internal.net

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class AttributionHeadersTest {

    @Test
    fun `builds one header per field, using the real header names`() {
        val headers = buildAttributionHeaders(
            sdkVersion = "0.1.0",
            osVersion = "14",
            apiLevel = 34,
            deviceModel = "Pixel 6",
            deviceManufacturer = "Google",
            appPackage = "io.kwiklink.sample",
            appVersion = "1.2.3",
        )

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
            ),
            headers,
        )
    }

    @Test
    fun `sdk platform is always android, regardless of input`() {
        val headers = buildAttributionHeaders(
            sdkVersion = "0.1.0",
            osVersion = "14",
            apiLevel = 34,
            deviceModel = "Pixel 6",
            deviceManufacturer = "Google",
            appPackage = "io.kwiklink.sample",
            appVersion = null,
        )

        assertEquals("android", headers[HEADER_SDK_PLATFORM])
    }

    @Test
    fun `a null app version omits the header instead of sending a blank one`() {
        val headers = buildAttributionHeaders(
            sdkVersion = "0.1.0",
            osVersion = "14",
            apiLevel = 34,
            deviceModel = "Pixel 6",
            deviceManufacturer = "Google",
            appPackage = "io.kwiklink.sample",
            appVersion = null,
        )

        assertFalse(headers.containsKey(HEADER_APP_VERSION))
    }

    @Test
    fun `api level is sent as a plain decimal string`() {
        val headers = buildAttributionHeaders(
            sdkVersion = "0.1.0",
            osVersion = "14",
            apiLevel = 34,
            deviceModel = "Pixel 6",
            deviceManufacturer = "Google",
            appPackage = "io.kwiklink.sample",
            appVersion = null,
        )

        assertEquals("34", headers[HEADER_API_LEVEL])
    }
}
