package io.kwiklink.sample

import android.app.Application
import io.kwiklink.android.sdk.Kwiklink
import io.kwiklink.android.sdk.KwiklinkConfig

class KwiklinkSampleApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Kwiklink.init(
            this,
            KwiklinkConfig(
                // Set via local.properties (KWIKLINK_API_KEY, gitignored —
                // see local.properties.example), not hardcoded here. Falls
                // back to a placeholder that gets a 401
                // (KwiklinkError.Server) from both endpoints until a real
                // key (App Details -> API Keys in the dashboard, for
                // whichever app owns the domain being tested against) is
                // set locally.
                apiKey = BuildConfig.API_KEY,
            ),
        )
    }
}
