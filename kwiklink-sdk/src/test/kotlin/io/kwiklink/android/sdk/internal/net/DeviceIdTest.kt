package io.kwiklink.android.sdk.internal.net

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.fail
import org.junit.Test

class DeviceIdTest {

    @Test
    fun `a real android id is used as-is and never persisted`() {
        val result = resolveDeviceId(
            androidId = "abcdef0123456789",
            storedFallback = null,
            generateFallback = { fail("should never generate when Android ID is present"); "" },
        )

        assertEquals("abcdef0123456789", result.id)
        assertEquals(ResolvedDeviceId.SOURCE_ANDROID_ID, result.source)
        assertFalse(result.shouldPersist)
    }

    @Test
    fun `a null android id falls back to a previously stored generated id`() {
        val result = resolveDeviceId(
            androidId = null,
            storedFallback = "already-generated-uuid",
            generateFallback = { fail("should never generate when a fallback is already stored"); "" },
        )

        assertEquals("already-generated-uuid", result.id)
        assertEquals(ResolvedDeviceId.SOURCE_GENERATED, result.source)
        assertFalse(result.shouldPersist)
    }

    @Test
    fun `a blank android id (not just null) also falls back`() {
        val result = resolveDeviceId(
            androidId = "   ",
            storedFallback = "already-generated-uuid",
            generateFallback = { fail("should never generate when a fallback is already stored"); "" },
        )

        assertEquals("already-generated-uuid", result.id)
    }

    @Test
    fun `no android id and no stored fallback generates a new one and asks to persist it`() {
        val result = resolveDeviceId(
            androidId = null,
            storedFallback = null,
            generateFallback = { "brand-new-uuid" },
        )

        assertEquals("brand-new-uuid", result.id)
        assertEquals(ResolvedDeviceId.SOURCE_GENERATED, result.source)
        assertEquals(true, result.shouldPersist)
    }

    @Test
    fun `android id takes priority over an already-stored fallback`() {
        val result = resolveDeviceId(
            androidId = "real-android-id",
            storedFallback = "stale-generated-uuid",
            generateFallback = { fail("should never generate when Android ID is present"); "" },
        )

        assertEquals("real-android-id", result.id)
        assertEquals(ResolvedDeviceId.SOURCE_ANDROID_ID, result.source)
    }
}
