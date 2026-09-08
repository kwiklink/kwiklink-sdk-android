package io.kwiklink.android.sdk.internal.referrer

import android.content.Context
import com.android.installreferrer.api.InstallReferrerClient
import com.android.installreferrer.api.InstallReferrerStateListener
import io.kwiklink.android.sdk.internal.log.KwiklinkLog
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Binds Google Play's `InstallReferrerClient` and returns whatever
 * `click_id` it can extract — `null` for anything short of a clean OK
 * response (Play Store not installed, service unavailable, malformed
 * referrer, timeout). Deferred attribution has a legitimate fallback for
 * "no click id" (the backend's IP+platform fingerprint match, see
 * `HandleAttributionMatch`), so failing loud here would trade a working
 * best-effort path for a hard error over what's normally a soft signal.
 *
 * Every `null` outcome here used to be silent — with logging enabled
 * (KwiklinkConfig.loggingEnabled), each distinct reason now gets its own
 * line, so a real integrator whose deferred-open isn't working has
 * something to look at other than a guess. Never logs the referrer string
 * or the extracted click_id itself — see KwiklinkLog's own doc comment for
 * why.
 */
internal class InstallReferrerReader(private val context: Context) {

    suspend fun readClickId(): String? {
        var completed = false
        val clickId = withTimeoutOrNull(TIMEOUT_MS) {
            suspendCancellableCoroutine { cont ->
                val client = InstallReferrerClient.newBuilder(context).build()
                client.startConnection(object : InstallReferrerStateListener {
                    override fun onInstallReferrerSetupFinished(responseCode: Int) {
                        completed = true
                        val clickId = if (responseCode == InstallReferrerClient.InstallReferrerResponse.OK) {
                            runCatching { client.installReferrer.installReferrer }
                                .onFailure { KwiklinkLog.w("Deferred-open: failed to read install referrer", it) }
                                .getOrNull()
                                ?.let(ReferrerParser::parseClickId)
                                .also {
                                    if (it == null) {
                                        KwiklinkLog.d("Deferred-open: no click_id present in install referrer")
                                    } else {
                                        KwiklinkLog.d("Deferred-open: click_id extracted from install referrer")
                                    }
                                }
                        } else {
                            KwiklinkLog.d("Deferred-open: install referrer setup finished with non-OK response ($responseCode)")
                            null
                        }
                        runCatching { client.endConnection() }
                        if (cont.isActive) cont.resume(clickId) { _, _, _ -> }
                    }

                    override fun onInstallReferrerServiceDisconnected() {
                        completed = true
                        KwiklinkLog.d("Deferred-open: install referrer service disconnected before a response")
                        if (cont.isActive) cont.resume(null) { _, _, _ -> }
                    }
                })
                cont.invokeOnCancellation { runCatching { client.endConnection() } }
            }
        }
        if (!completed) {
            KwiklinkLog.w("Deferred-open: timed out waiting for install referrer (>${TIMEOUT_MS}ms)")
        }
        return clickId
    }

    private companion object {
        const val TIMEOUT_MS = 5_000L
    }
}
