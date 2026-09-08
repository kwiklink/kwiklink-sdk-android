package io.kwiklink.android.sdk.internal.referrer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReferrerParserTest {

    @Test
    fun `click_id alone`() {
        assertEquals("abc123", ReferrerParser.parseClickId("click_id=abc123"))
    }

    @Test
    fun `click_id among other utm params, in any position`() {
        assertEquals(
            "abc123",
            ReferrerParser.parseClickId("utm_source=google-play&click_id=abc123&utm_medium=organic"),
        )
    }

    @Test
    fun `no click_id present`() {
        assertNull(ReferrerParser.parseClickId("utm_source=google-play&utm_medium=organic"))
    }

    @Test
    fun `empty referrer string`() {
        assertNull(ReferrerParser.parseClickId(""))
    }

    @Test
    fun `blank click_id value is treated as absent`() {
        assertNull(ReferrerParser.parseClickId("click_id="))
    }

    @Test
    fun `url-encoded click_id value is decoded`() {
        assertEquals("a b+c", ReferrerParser.parseClickId("click_id=a%20b%2Bc"))
    }

    @Test
    fun `malformed percent-escape falls back to the raw value instead of throwing`() {
        assertEquals("abc%", ReferrerParser.parseClickId("click_id=abc%"))
    }
}
