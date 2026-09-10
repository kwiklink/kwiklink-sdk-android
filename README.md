# Kwiklink Android SDK

Resolves Kwiklink deep links inside an Android host app: warm opens (the
app is already installed and a verified App Link is tapped) and deferred
opens (the app is installed *because of* a link tap, and needs to claim
that link on first launch).

Consume it as a local Gradle module (`includeBuild`/`project(":kwiklink-sdk")`)
or build the `.aar` yourself.

## Requirements

- `minSdk` 23 (the floor Android App Links verification itself supports)
- Kotlin host app, or Java via the `Callback<T>`-based method overloads

## How the SDK talks to Kwiklink

Every SDK call goes to Kwiklink's own fixed API domain,
`orbit-in1.kwiklink.io`, under an `/android` path prefix
(`/android/v1/links/resolve`, `/android/v1/attribution/match`) — never
directly to your own short-link domain (`your-domain.example`). That
domain still exists and still serves real redirects/App Links, but the
SDK only ever sends it as a request *parameter*, not as the host it
connects to. This isn't something you configure — it's fixed inside the
SDK.

## 1. Register your domain, create an API key

In the Kwiklink dashboard, under your app's settings, add your Android
`applicationId` and signing cert SHA256 fingerprint to `AndroidConfig`.
That's what `GET /.well-known/assetlinks.json` on your short-link domain
serves back — without it, Android's App Links verification silently falls
back to a disambiguation dialog instead of opening straight into your app.

Also create an API key under App Details -> API Keys, and use it as
`KwiklinkConfig.apiKey` below — every call this SDK makes now requires it
(`X-Api-Key` header), and fails with a 401 (`KwiklinkError.Server`)
without one. Worth being clear-eyed about what this key does and doesn't
provide: it ships inside your compiled APK, so it's recoverable by anyone
who decompiles it — the same non-secret "app identifier" role Branch's
`branch_key` or AppsFlyer's `dev_key` play, not a true secret. What it
does buy: a stranger without your key can't enumerate links across the
whole platform for free, and you can rate-limit or revoke per-key if one
ever does leak.

## 2. Manifest

```xml
<activity
    android:name=".YourLinkHandlingActivity"
    android:exported="true"
    android:launchMode="singleTask">

    <intent-filter android:autoVerify="true">
        <action android:name="android.intent.action.VIEW" />
        <category android:name="android.intent.category.DEFAULT" />
        <category android:name="android.intent.category.BROWSABLE" />
        <data android:scheme="https" android:host="your-domain.example" />
    </intent-filter>
</activity>
```

`launchMode="singleTask"` matters: without it, re-tapping a link while
your app is already open creates a second activity instance instead of
redelivering the intent to the existing one via `onNewIntent`.

### Custom-scheme fallback (belt-and-suspenders)

App Links verification can silently fail — an OEM skin, no network at
install time, a sideloaded APK. As a fallback, you can register your own
custom-scheme intent-filter pointing at the same activity:

```xml
<intent-filter>
    <action android:name="android.intent.action.VIEW" />
    <category android:name="android.intent.category.DEFAULT" />
    <category android:name="android.intent.category.BROWSABLE" />
    <data android:scheme="yourapp" android:host="your-domain.example" />
</intent-filter>
```

This needs no SDK changes to work: `IntentLinkParser` never looks at the
URI *scheme*, only the host and path — a `yourapp://your-domain.example/<slug>`
intent parses identically to the `https://` one, as long as your
custom-scheme URIs use the Kwiklink domain as the authority. There's no
backend or data-model support for a separate custom-scheme field; this is
a host-app-manifest-only pattern, entirely your own to wire up (typically
from your own web fallback page, or wherever else you'd otherwise show a
"open in app" prompt).

## 3. Initialize

```kotlin
class YourApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Kwiklink.init(
            this,
            KwiklinkConfig(
                apiKey = "...", // from App Details -> API Keys — required, see step 1
            ),
        )
    }
}
```

### Toggling logging after init

`KwiklinkConfig.loggingEnabled` sets the initial state, but you don't have
to rebuild a `KwiklinkConfig` to change it later — `Kwiklink.enableLogging(Boolean)`
is a plain, callable-anytime setter (same shape as `enableEdgeToEdge(Boolean)`):

```kotlin
Kwiklink.enableLogging(true)  // start seeing Kwiklink-tagged Logcat output
Kwiklink.enableLogging(false) // and stop again
```

A second overload also sets how verbose that output is:

```kotlin
Kwiklink.enableLogging(true, KwiklinkLogLevel.WARN) // warnings/errors only, no routine debug lines
```

`KwiklinkLogLevel` is `DEBUG` (everything, the default), `WARN`, or
`ERROR` — each level shows itself and anything more severe. Both overloads
work even before `init()` is called — useful if you want to see the
`"Kwiklink SDK initialized"` line itself. Neither changes *what's* logged,
only how much/whether: the API key, click id, domain+slug, `linkData`, and
the raw referrer string are never logged at any level — see `KwiklinkLog`'s
own doc comment for why.

### Alternative: manifest-based auto-init (no Application code needed)

Instead of the above, you can opt into having the SDK initialize itself —
the same trick Firebase/Branch use, via a manifest-declared
`ContentProvider` that runs before your `Application.onCreate()` does:

```xml
<application>
    <meta-data android:name="io.kwiklink.android.sdk.AutoInit" android:value="true" />
    <meta-data android:name="io.kwiklink.android.sdk.ApiKey" android:value="${kwiklinkApiKey}" />
    <meta-data android:name="io.kwiklink.android.sdk.LoggingEnabled" android:value="true" />
</application>
```

Off by default — omitting `io.kwiklink.android.sdk.AutoInit` (or setting it
to `false`) changes nothing; you still call `Kwiklink.init()` yourself as
above. Turning it on but leaving `io.kwiklink.android.sdk.ApiKey` missing or
blank logs an error (unconditionally, not gated by `KwiklinkConfig.loggingEnabled`
— this is a misconfiguration, not routine operation) and the SDK stays
uninitialized, same as never calling `init()`. `io.kwiklink.android.sdk.LoggingEnabled`
is optional and defaults to `false`, same as `KwiklinkConfig.loggingEnabled`
— set it to see this SDK's own logs while integrating (see Testing your
integration below). An explicit `Kwiklink.init()` call always wins if you
use both, since `Application.onCreate()` runs after the manifest-declared
provider.

`${kwiklinkApiKey}` above is a manifest placeholder — define it from your
own `build.gradle.kts` (`android.defaultConfig.manifestPlaceholders["kwiklinkApiKey"] = ...`)
so the real key still never appears as a literal in a committed manifest.

## 4. Warm open

Call this from the activity registered above, for both the initial
`Intent` and any redelivered one:

```kotlin
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    intent.data?.let { handleLink(intent) }
}

override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
    handleLink(intent)
}

private fun handleLink(intent: Intent) {
    lifecycleScope.launch {
        try {
            val result = Kwiklink.resolveLinkFromIntent(intent)
            if (result.matched) {
                // use result.linkId / result.linkData
            }
        } catch (e: KwiklinkError) {
            // network/server failure — result.matched == false is the
            // normal "not a Kwiklink link" outcome, not an exception
        }
    }
}
```

Java callers use the callback overload instead:
`Kwiklink.resolveLinkFromIntent(intent, callback)`.

## 5. Deferred (cold) open

Call this once, on a plain launch (no App Link `Intent`) — typically
gated so it only runs on the very first `onCreate` (`savedInstanceState
== null`), not every rotation:

```kotlin
if (intent.data == null && savedInstanceState == null) {
    lifecycleScope.launch {
        val result = Kwiklink.resolveDeferredLink()
        // same AttributionResult shape as the warm path
    }
}
```

Safe to call more than once — a click already claimed or expired just
resolves to `matched = false`, it doesn't error.

## Error handling

Every throwing call surfaces a `KwiklinkError` subtype: `NotInitialized`,
`Network`, `Server`, `InvalidResponse`. A normal "no such link" is
`AttributionResult.matched == false`, not an exception.

## Testing your integration

- **Warm open**, fully testable locally:
  ```
  adb shell am start -a android.intent.action.VIEW -d "https://your-domain.example/<slug>"
  adb shell pm get-app-links <your.application.id>
  ```
- **Deferred open**, referrer-parsing smoke test (real end-to-end needs a
  closed/internal Play testing track — this only proves the SDK reads and
  parses the referrer correctly, not a real Play Store install flow):
  ```
  adb shell am broadcast -a com.android.vending.INSTALL_REFERRER --es referrer "click_id=<test-id>"
  ```
  then launch the app.

## ProGuard / R8

Nothing to configure — `consumer-rules.pro` ships with the `.aar` and is
merged into your app's R8 config automatically.

## Module layout

```
kwiklink-sdk/   the library described above
sample-app/     a working example: real App Links manifest, both
                warm-open and deferred-open wired up in MainActivity
```
