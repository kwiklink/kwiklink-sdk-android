package io.kwiklink.android.sdk

import android.content.Context
import android.content.Intent
import io.kwiklink.android.sdk.internal.link.DeferredLinkResolver
import io.kwiklink.android.sdk.internal.link.IntentLinkParser
import io.kwiklink.android.sdk.internal.link.LinkResolver
import io.kwiklink.android.sdk.internal.log.KwiklinkLog
import io.kwiklink.android.sdk.internal.referrer.InstallReferrerReader
import io.kwiklink.android.sdk.model.AttributionResult
import io.kwiklink.android.sdk.model.KwiklinkError
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Public entrypoint — see docs/android-sdk-plan.md for the phased build-out. */
object Kwiklink {
    private var config: KwiklinkConfig? = null
    private var appContext: Context? = null

    private val resolver = LinkResolver()
    private val deferredResolver = DeferredLinkResolver()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    fun init(context: Context, config: KwiklinkConfig) {
        this.appContext = context.applicationContext
        this.config = config
        KwiklinkLog.enabled = config.loggingEnabled
        KwiklinkLog.d("Kwiklink SDK initialized")
    }

    /**
     * Toggles this SDK's own Logcat output on/off at any point — before or
     * after [init], no [KwiklinkConfig] to rebuild — same shape as
     * `enableEdgeToEdge(Boolean)`. Safe to call before [init]: only
     * [KwiklinkLog]'s flag is touched then, since there's no stored
     * [KwiklinkConfig] yet to update. What is/isn't logged regardless of
     * this flag is unaffected — see
     * [io.kwiklink.android.sdk.internal.log.KwiklinkLog]'s own doc comment.
     */
    fun enableLogging(enabled: Boolean) {
        KwiklinkLog.enabled = enabled
        config = config?.copy(loggingEnabled = enabled)
    }

    /**
     * Same as [enableLogging] but also sets how verbose the output is —
     * e.g. `enableLogging(true, KwiklinkLogLevel.WARN)` shows warnings and
     * errors but suppresses routine debug lines. [level] is stored
     * regardless of [enabled]; it only has an observable effect once
     * logging is actually on.
     */
    fun enableLogging(enabled: Boolean, level: KwiklinkLogLevel) {
        KwiklinkLog.enabled = enabled
        KwiklinkLog.minLevel = level
        config = config?.copy(loggingEnabled = enabled)
    }

    /**
     * Warm open: call this from the activity that receives the App Link
     * `Intent` (its `android:autoVerify="true"` intent-filter tap) — every
     * intent this call is given gets sent to the backend as a resolve
     * lookup, so if the same activity also handles unrelated App Link
     * hosts, scope that at the manifest's own intent-filter level (separate
     * `<data android:host="...">` entries / a separate activity), not here.
     * Returns [AttributionResult.matched] = false for a link the backend
     * doesn't recognize; throws [KwiklinkError] only for a genuine failure
     * (network, server).
     */
    suspend fun resolveLinkFromIntent(intent: Intent): AttributionResult {
        val cfg = config ?: throw KwiklinkError.NotInitialized()
        val parsed = IntentLinkParser.parse(intent)
        if (parsed == null) {
            KwiklinkLog.d("Warm-open: intent has no parseable link data, ignoring")
            return AttributionResult.unmatched()
        }
        KwiklinkLog.d("Warm-open: resolving link")
        return try {
            resolver.resolve(parsed.domain, parsed.slug, cfg.apiKey).also {
                KwiklinkLog.d("Warm-open: ${if (it.matched) "matched" else "not matched"}")
            }
        } catch (e: KwiklinkError) {
            KwiklinkLog.e("Warm-open: failed", e)
            throw e
        }
    }

    /** Java-friendly variant of [resolveLinkFromIntent]. */
    fun resolveLinkFromIntent(intent: Intent, callback: Callback<AttributionResult>) {
        scope.launch {
            try {
                callback.onSuccess(resolveLinkFromIntent(intent))
            } catch (e: KwiklinkError) {
                callback.onError(e)
            }
        }
    }

    /**
     * Cold/deferred open: call this once, on the app's first launch after
     * install, to claim a deep link that was tapped before the app was
     * installed. Reads the Play Install Referrer for a `click_id` (best
     * effort — `null` for anything short of a clean referrer, see
     * [InstallReferrerReader]) and calls `POST /android/v1/attribution/match`;
     * a missing `click_id` still gets sent, since the backend falls back to
     * an IP+platform fingerprint match in that case rather than failing.
     */
    suspend fun resolveDeferredLink(): AttributionResult {
        val cfg = config ?: throw KwiklinkError.NotInitialized()
        val ctx = appContext ?: throw KwiklinkError.NotInitialized()
        KwiklinkLog.d("Deferred-open: reading install referrer")
        val clickId = InstallReferrerReader(ctx).readClickId()
        KwiklinkLog.d("Deferred-open: matching (${if (clickId != null) "by click_id" else "by IP+platform fingerprint fallback"})")
        return try {
            deferredResolver.resolve(clickId, cfg.apiKey).also {
                KwiklinkLog.d("Deferred-open: ${if (it.matched) "matched" else "not matched"}")
            }
        } catch (e: KwiklinkError) {
            KwiklinkLog.e("Deferred-open: failed", e)
            throw e
        }
    }

    /** Java-friendly variant of [resolveDeferredLink]. */
    fun resolveDeferredLink(callback: Callback<AttributionResult>) {
        scope.launch {
            try {
                callback.onSuccess(resolveDeferredLink())
            } catch (e: KwiklinkError) {
                callback.onError(e)
            }
        }
    }
}
