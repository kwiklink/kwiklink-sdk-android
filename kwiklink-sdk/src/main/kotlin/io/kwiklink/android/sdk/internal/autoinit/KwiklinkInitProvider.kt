package io.kwiklink.android.sdk.internal.autoinit

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.util.Log
import io.kwiklink.android.sdk.Kwiklink

/**
 * Auto-init entrypoint. Android instantiates every manifest-declared
 * `ContentProvider` before `Application.onCreate()` runs, handing it a real
 * [Context] — the same trick Firebase/Branch/WorkManager use to get a
 * [Context] without an integrator writing any code. Merged into every host
 * app's manifest automatically ([android:name=".internal.autoinit.KwiklinkInitProvider"
 * in kwiklink-sdk/src/main/AndroidManifest.xml]) but a complete no-op unless
 * that app's own manifest opts in — see [KwiklinkAutoInit].
 *
 * A host app that already calls [Kwiklink.init] manually is unaffected
 * either way: that call simply overwrites whatever (or nothing) this
 * provider set, since it runs later, in `Application.onCreate()`.
 */
internal class KwiklinkInitProvider : ContentProvider() {

    override fun onCreate(): Boolean {
        val ctx = context ?: return false
        val metaData = readMetaData(ctx) ?: return false
        val autoInit = metaData.getBoolean(KwiklinkAutoInit.META_AUTO_INIT, false)
        val apiKey = metaData.getString(KwiklinkAutoInit.META_API_KEY)
        val loggingEnabled = metaData.getBoolean(KwiklinkAutoInit.META_LOGGING_ENABLED, false)

        val config = KwiklinkAutoInit.configFor(autoInit, apiKey, loggingEnabled)
        if (config == null) {
            if (autoInit) {
                // Always logged, regardless of KwiklinkConfig.loggingEnabled
                // — this is a build misconfiguration an integrator opted
                // into fixing by setting META_AUTO_INIT, not routine
                // operation that flag is meant to quiet.
                Log.e(
                    "Kwiklink",
                    "${KwiklinkAutoInit.META_AUTO_INIT} is true but " +
                        "${KwiklinkAutoInit.META_API_KEY} is missing/blank in " +
                        "AndroidManifest.xml — Kwiklink was not auto-initialized.",
                )
            }
            return false
        }

        Kwiklink.init(ctx, config)
        return true
    }

    private fun readMetaData(ctx: Context): Bundle? =
        try {
            ctx.packageManager
                .getApplicationInfo(ctx.packageName, PackageManager.GET_META_DATA)
                .metaData
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }

    override fun query(
        uri: Uri,
        projection: Array<String>?,
        selection: String?,
        selectionArgs: Array<String>?,
        sortOrder: String?,
    ): Cursor? = null

    override fun getType(uri: Uri): String? = null

    override fun insert(uri: Uri, values: ContentValues?): Uri? = null

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<String>?): Int = 0

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<String>?,
    ): Int = 0
}
