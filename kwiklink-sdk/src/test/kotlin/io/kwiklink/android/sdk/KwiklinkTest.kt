package io.kwiklink.android.sdk

import io.kwiklink.android.sdk.internal.log.KwiklinkLog
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test

class KwiklinkTest {

    @After
    fun resetLoggingFlag() {
        // Kwiklink is a process-wide singleton — leave logging back off so
        // this doesn't leak into other test classes' output.
        Kwiklink.enableLogging(false, KwiklinkLogLevel.DEBUG)
    }

    @Test
    fun `enableLogging turns logging on without requiring init to have run first`() {
        Kwiklink.enableLogging(true)
        assertEquals(true, KwiklinkLog.enabled)
    }

    @Test
    fun `enableLogging can turn logging back off`() {
        Kwiklink.enableLogging(true)
        Kwiklink.enableLogging(false)
        assertEquals(false, KwiklinkLog.enabled)
    }

    @Test
    fun `enableLogging with a level sets both the flag and the level`() {
        Kwiklink.enableLogging(true, KwiklinkLogLevel.WARN)
        assertEquals(true, KwiklinkLog.enabled)
        assertEquals(KwiklinkLogLevel.WARN, KwiklinkLog.minLevel)
    }

    @Test
    fun `enableLogging with a level can also turn logging off`() {
        Kwiklink.enableLogging(false, KwiklinkLogLevel.ERROR)
        assertEquals(false, KwiklinkLog.enabled)
        assertEquals(KwiklinkLogLevel.ERROR, KwiklinkLog.minLevel)
    }
}
