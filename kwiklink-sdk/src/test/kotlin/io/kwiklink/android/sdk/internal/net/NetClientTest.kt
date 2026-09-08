package io.kwiklink.android.sdk.internal.net

import io.kwiklink.android.sdk.model.KwiklinkError
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

private const val TEST_API_KEY = "test-key-123"

class NetClientTest {
    private lateinit var server: MockWebServer
    private lateinit var client: NetClient

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        client = NetClient()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `200 with a valid body decodes to Success`() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(200)
                .setBody("""{"linkId":"link-1","linkData":{"campaign":"spring"}}"""),
        )

        val result = client.get(server.url("/v1/links/resolve"), TEST_API_KEY, ResolveLinkResponseDto.serializer())

        assertTrue(result is NetResult.Success)
        val dto = (result as NetResult.Success).value
        assertEquals("link-1", dto.linkId)
    }

    @Test
    fun `404 becomes NotFound, not an exception`() = runTest {
        server.enqueue(MockResponse().setResponseCode(404).setBody("""{"error":"link not found"}"""))

        val result = client.get(server.url("/v1/links/resolve"), TEST_API_KEY, ResolveLinkResponseDto.serializer())

        assertEquals(NetResult.NotFound, result)
    }

    @Test
    fun `500 throws KwiklinkError Server with the status code`() = runTest {
        server.enqueue(MockResponse().setResponseCode(500).setBody("""{"error":"internal error"}"""))

        try {
            client.get(server.url("/v1/links/resolve"), TEST_API_KEY, ResolveLinkResponseDto.serializer())
            fail("expected KwiklinkError.Server")
        } catch (e: KwiklinkError.Server) {
            assertEquals(500, e.statusCode)
        }
    }

    @Test
    fun `malformed JSON on a 200 throws KwiklinkError InvalidResponse`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody("not json"))

        try {
            client.get(server.url("/v1/links/resolve"), TEST_API_KEY, ResolveLinkResponseDto.serializer())
            fail("expected KwiklinkError.InvalidResponse")
        } catch (e: KwiklinkError.InvalidResponse) {
            // expected
        }
    }

    @Test
    fun `post 200 with matched true decodes normally`() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(200)
                .setBody("""{"matched":true,"linkId":"link-1","linkData":{"campaign":"spring"}}"""),
        )

        val dto = client.post(
            server.url("/v1/attribution/match"),
            TEST_API_KEY,
            AttributionMatchRequestDto(clickId = "click-1"),
            AttributionMatchRequestDto.serializer(),
            AttributionMatchResponseDto.serializer(),
        )

        assertEquals(true, dto.matched)
        assertEquals("link-1", dto.linkId)
    }

    @Test
    fun `post 404 with matched false still decodes, unlike get`() = runTest {
        server.enqueue(MockResponse().setResponseCode(404).setBody("""{"matched":false}"""))

        val dto = client.post(
            server.url("/v1/attribution/match"),
            TEST_API_KEY,
            AttributionMatchRequestDto(),
            AttributionMatchRequestDto.serializer(),
            AttributionMatchResponseDto.serializer(),
        )

        assertEquals(false, dto.matched)
    }

    @Test
    fun `post sends clickId and platform as the JSON request body`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"matched":false}"""))

        client.post(
            server.url("/v1/attribution/match"),
            TEST_API_KEY,
            AttributionMatchRequestDto(clickId = "click-1"),
            AttributionMatchRequestDto.serializer(),
            AttributionMatchResponseDto.serializer(),
        )

        val recorded = server.takeRequest()
        assertEquals("POST", recorded.method)
        assertEquals("""{"clickId":"click-1","platform":"android"}""", recorded.body.readUtf8())
    }

    @Test
    fun `get sends the api key as the X-Api-Key header`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"linkId":"link-1","linkData":{}}"""))

        client.get(server.url("/v1/links/resolve"), TEST_API_KEY, ResolveLinkResponseDto.serializer())

        assertEquals(TEST_API_KEY, server.takeRequest().getHeader("X-Api-Key"))
    }

    @Test
    fun `post sends the api key as the X-Api-Key header`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"matched":false}"""))

        client.post(
            server.url("/v1/attribution/match"),
            TEST_API_KEY,
            AttributionMatchRequestDto(),
            AttributionMatchRequestDto.serializer(),
            AttributionMatchResponseDto.serializer(),
        )

        assertEquals(TEST_API_KEY, server.takeRequest().getHeader("X-Api-Key"))
    }

    @Test
    fun `post 500 throws KwiklinkError Server`() = runTest {
        server.enqueue(MockResponse().setResponseCode(500).setBody("""{"error":"internal error"}"""))

        try {
            client.post(
                server.url("/v1/attribution/match"),
                TEST_API_KEY,
                AttributionMatchRequestDto(),
                AttributionMatchRequestDto.serializer(),
                AttributionMatchResponseDto.serializer(),
            )
            fail("expected KwiklinkError.Server")
        } catch (e: KwiklinkError.Server) {
            assertEquals(500, e.statusCode)
        }
    }
}
