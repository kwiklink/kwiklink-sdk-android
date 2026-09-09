package io.kwiklink.android.sdk.internal.net

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class AttributionInterceptorTest {
    private lateinit var server: MockWebServer
    private lateinit var client: OkHttpClient

    private val testHeaders = mapOf(
        HEADER_SDK_VERSION to "0.1.0",
        HEADER_SDK_PLATFORM to "android",
        HEADER_OS_VERSION to "14",
        HEADER_API_LEVEL to "34",
        HEADER_DEVICE_MODEL to "Pixel 6",
        HEADER_DEVICE_MANUFACTURER to "Google",
        HEADER_APP_PACKAGE to "io.kwiklink.sample",
        HEADER_APP_VERSION to "1.2.3",
    )

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        client = OkHttpClient.Builder().addInterceptor(AttributionInterceptor()).build()
    }

    @After
    fun tearDown() {
        server.shutdown()
        SdkAttributionInfo.headers = emptyMap()
    }

    @Test
    fun `attaches every current SdkAttributionInfo header to an outgoing request`() {
        SdkAttributionInfo.headers = testHeaders
        server.enqueue(MockResponse().setResponseCode(200))

        client.newCall(Request.Builder().url(server.url("/anything")).build()).execute().close()

        val recorded = server.takeRequest()
        testHeaders.forEach { (name, value) -> assertEquals(value, recorded.getHeader(name)) }
    }

    @Test
    fun `no SdkAttributionInfo yet means no attribution headers, not a crash`() {
        SdkAttributionInfo.headers = emptyMap()
        server.enqueue(MockResponse().setResponseCode(200))

        client.newCall(Request.Builder().url(server.url("/anything")).build()).execute().close()

        val recorded = server.takeRequest()
        assertNull(recorded.getHeader(HEADER_SDK_VERSION))
    }

    @Test
    fun `attribution headers never override a header the request already set`() {
        SdkAttributionInfo.headers = mapOf(HEADER_SDK_VERSION to "0.1.0")
        server.enqueue(MockResponse().setResponseCode(200))

        val request = Request.Builder().url(server.url("/anything")).header("X-Api-Key", "test-key").build()
        client.newCall(request).execute().close()

        val recorded = server.takeRequest()
        assertEquals("test-key", recorded.getHeader("X-Api-Key"))
        assertEquals("0.1.0", recorded.getHeader(HEADER_SDK_VERSION))
    }
}
