package io.kwiklink.android.sdk.internal.link

import android.content.Intent
import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

/**
 * [IntentLinkParserTest] covers [IntentLinkParser.parseUri]'s logic on the
 * plain JVM; this covers the one line that actually touches the Android
 * framework — [IntentLinkParser.parse] against a real `Intent`/`Uri`, which
 * `parseUri`'s plain-string tests can't exercise. Needs a device/emulator
 * to run (`connectedDebugAndroidTest`) — not executed in every environment
 * this SDK is built in, see docs/android-sdk-plan.md's Phase 1/2 notes.
 */
@RunWith(AndroidJUnit4::class)
class IntentLinkParserInstrumentedTest {

    @Test
    fun realHttpsAppLinkIntentParses() {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://1kwik.link/diwali-sale"))
        assertEquals(IntentLinkParser.ParsedLink("1kwik.link", "diwali-sale"), IntentLinkParser.parse(intent))
    }

    @Test
    fun queryParametersOnTheAppLinkAreIgnored() {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://1kwik.link/diwali-sale?utm_source=email"))
        assertEquals(IntentLinkParser.ParsedLink("1kwik.link", "diwali-sale"), IntentLinkParser.parse(intent))
    }

    /**
     * A custom-scheme fallback URI shaped like the real short link (domain
     * as authority) parses identically to the https version — [IntentLinkParser]
     * never looks at the scheme, only host + path. This is the mechanism
     * behind the custom-scheme fallback guidance in sdks/android/README.md:
     * a host app's own `yourapp://1kwik.link/<slug>` fallback intent-filter
     * needs no SDK changes to work.
     */
    @Test
    fun customSchemeUriWithTheDomainAsAuthorityParsesTheSame() {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("kwiklinksample://1kwik.link/diwali-sale"))
        assertEquals(IntentLinkParser.ParsedLink("1kwik.link", "diwali-sale"), IntentLinkParser.parse(intent))
    }

    @Test
    fun intentWithNoDataIsNull() {
        assertNull(IntentLinkParser.parse(Intent(Intent.ACTION_MAIN)))
    }
}
