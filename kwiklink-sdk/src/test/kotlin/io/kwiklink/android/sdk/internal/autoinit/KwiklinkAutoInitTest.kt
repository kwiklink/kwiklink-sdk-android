package io.kwiklink.android.sdk.internal.autoinit

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class KwiklinkAutoInitTest {

    @Test
    fun `auto-init false is a no-op regardless of api key`() {
        assertNull(KwiklinkAutoInit.configFor(autoInit = false, apiKey = "a-real-key", loggingEnabled = false))
    }

    @Test
    fun `auto-init false with no api key is still a no-op`() {
        assertNull(KwiklinkAutoInit.configFor(autoInit = false, apiKey = null, loggingEnabled = false))
    }

    @Test
    fun `auto-init true with a null api key is treated as misconfigured`() {
        assertNull(KwiklinkAutoInit.configFor(autoInit = true, apiKey = null, loggingEnabled = false))
    }

    @Test
    fun `auto-init true with a blank api key is treated as misconfigured`() {
        assertNull(KwiklinkAutoInit.configFor(autoInit = true, apiKey = "   ", loggingEnabled = false))
    }

    @Test
    fun `auto-init true with a real api key produces a config carrying it`() {
        val config = KwiklinkAutoInit.configFor(autoInit = true, apiKey = "a-real-key", loggingEnabled = false)
        assertEquals("a-real-key", config?.apiKey)
    }

    @Test
    fun `loggingEnabled false flows through to the config`() {
        val config = KwiklinkAutoInit.configFor(autoInit = true, apiKey = "a-real-key", loggingEnabled = false)
        assertEquals(false, config?.loggingEnabled)
    }

    @Test
    fun `loggingEnabled true flows through to the config`() {
        val config = KwiklinkAutoInit.configFor(autoInit = true, apiKey = "a-real-key", loggingEnabled = true)
        assertEquals(true, config?.loggingEnabled)
    }
}
