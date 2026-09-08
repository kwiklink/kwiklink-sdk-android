package io.kwiklink.android.sdk.internal.log

import io.kwiklink.android.sdk.KwiklinkLogLevel
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test

class KwiklinkLogTest {

    @After
    fun resetState() {
        KwiklinkLog.enabled = false
        KwiklinkLog.minLevel = KwiklinkLogLevel.DEBUG
    }

    @Test
    fun `disabled never logs regardless of level`() {
        KwiklinkLog.enabled = false
        KwiklinkLog.minLevel = KwiklinkLogLevel.DEBUG
        assertEquals(false, KwiklinkLog.shouldLog(KwiklinkLogLevel.ERROR))
    }

    @Test
    fun `enabled with DEBUG minLevel logs everything`() {
        KwiklinkLog.enabled = true
        KwiklinkLog.minLevel = KwiklinkLogLevel.DEBUG
        assertEquals(true, KwiklinkLog.shouldLog(KwiklinkLogLevel.DEBUG))
        assertEquals(true, KwiklinkLog.shouldLog(KwiklinkLogLevel.WARN))
        assertEquals(true, KwiklinkLog.shouldLog(KwiklinkLogLevel.ERROR))
    }

    @Test
    fun `enabled with WARN minLevel suppresses DEBUG but not WARN or ERROR`() {
        KwiklinkLog.enabled = true
        KwiklinkLog.minLevel = KwiklinkLogLevel.WARN
        assertEquals(false, KwiklinkLog.shouldLog(KwiklinkLogLevel.DEBUG))
        assertEquals(true, KwiklinkLog.shouldLog(KwiklinkLogLevel.WARN))
        assertEquals(true, KwiklinkLog.shouldLog(KwiklinkLogLevel.ERROR))
    }

    @Test
    fun `enabled with ERROR minLevel only logs ERROR`() {
        KwiklinkLog.enabled = true
        KwiklinkLog.minLevel = KwiklinkLogLevel.ERROR
        assertEquals(false, KwiklinkLog.shouldLog(KwiklinkLogLevel.DEBUG))
        assertEquals(false, KwiklinkLog.shouldLog(KwiklinkLogLevel.WARN))
        assertEquals(true, KwiklinkLog.shouldLog(KwiklinkLogLevel.ERROR))
    }
}
