package io.kwiklink.android.sdk.internal.autoinit

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * [KwiklinkAutoInitTest] covers the decision logic on the plain JVM; this
 * covers the one thing that can't be tested there — reading real manifest
 * meta-data through a real PackageManager. src/androidTest/AndroidManifest.xml
 * supplies the test-only meta-data values this asserts against
 * (io.kwiklink.android.sdk.AutoInit=true, io.kwiklink.android.sdk.ApiKey=
 * "instrumented-test-api-key").
 */
@RunWith(AndroidJUnit4::class)
class KwiklinkInitProviderInstrumentedTest {

    @Test
    fun onCreateReadsRealManifestMetaDataAndReturnsTrue() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val provider = KwiklinkInitProvider()
        provider.attachInfo(context, null)

        assertTrue(provider.onCreate())
    }
}
