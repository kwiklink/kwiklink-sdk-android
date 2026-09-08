package io.kwiklink.android.sdk.internal.link

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class IntentLinkParserTest {

    @Test
    fun `single path segment resolves to domain and slug`() {
        val parsed = IntentLinkParser.parseUri("1kwik.link", listOf("diwali-sale"))
        assertEquals(IntentLinkParser.ParsedLink("1kwik.link", "diwali-sale"), parsed)
    }

    @Test
    fun `null host is rejected`() {
        assertNull(IntentLinkParser.parseUri(null, listOf("diwali-sale")))
    }

    @Test
    fun `blank host is rejected`() {
        assertNull(IntentLinkParser.parseUri("", listOf("diwali-sale")))
    }

    @Test
    fun `no path segments is rejected`() {
        assertNull(IntentLinkParser.parseUri("1kwik.link", emptyList()))
    }

    @Test
    fun `multiple path segments is rejected rather than guessed at`() {
        assertNull(IntentLinkParser.parseUri("1kwik.link", listOf("diwali-sale", "extra")))
    }

    @Test
    fun `blank trailing segment from a trailing slash is ignored`() {
        val parsed = IntentLinkParser.parseUri("1kwik.link", listOf("diwali-sale", ""))
        assertEquals(IntentLinkParser.ParsedLink("1kwik.link", "diwali-sale"), parsed)
    }
}
