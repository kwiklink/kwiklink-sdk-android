package io.kwiklink.sample

import android.content.Intent
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.card.MaterialCardView
import io.kwiklink.android.sdk.Kwiklink
import io.kwiklink.android.sdk.model.AttributionResult
import io.kwiklink.android.sdk.model.KwiklinkError
import io.kwiklink.android.sdk.model.LinkData
import kotlinx.coroutines.launch

/**
 * `singleTask` launch mode means a re-tap of an already-verified App Link
 * while this activity is on top delivers here via [onNewIntent], not a new
 * [onCreate] — both paths route through [handleIntent] so warm-open works
 * identically either way.
 *
 * A plain launch (no App Link `Intent`, `savedInstanceState == null` so a
 * rotation doesn't re-trigger it) instead tries the deferred/cold path —
 * what the manual `adb shell am broadcast ... INSTALL_REFERRER` smoke test
 * from docs/android-sdk-plan.md's Testing section exercises. Safe to call
 * more than once: a claimed or expired pending click just resolves to
 * `matched = false`.
 */
class MainActivity : AppCompatActivity() {
    private lateinit var statusPill: LinearLayout
    private lateinit var progressIndicator: ProgressBar
    private lateinit var statusText: TextView
    private lateinit var resultCard: MaterialCardView
    private lateinit var titleText: TextView
    private lateinit var linkIdText: TextView
    private lateinit var detailsContainer: LinearLayout
    private lateinit var emptyStateText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        statusPill = findViewById(R.id.statusPill)
        progressIndicator = findViewById(R.id.progressIndicator)
        statusText = findViewById(R.id.statusText)
        resultCard = findViewById(R.id.resultCard)
        titleText = findViewById(R.id.titleText)
        linkIdText = findViewById(R.id.linkIdText)
        detailsContainer = findViewById(R.id.detailsContainer)
        emptyStateText = findViewById(R.id.emptyStateText)

        if (intent.data != null) {
            handleWarmOpen(intent)
        } else if (savedInstanceState == null) {
            handleDeferredOpen()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleWarmOpen(intent)
    }

    private fun handleWarmOpen(intent: Intent) {
        setStatusPill(State.RESOLVING)
        lifecycleScope.launch {
            try {
                render(Kwiklink.resolveLinkFromIntent(intent))
            } catch (e: KwiklinkError) {
                renderError(e)
            }
        }
    }

    private fun handleDeferredOpen() {
        setStatusPill(State.RESOLVING)
        lifecycleScope.launch {
            try {
                render(Kwiklink.resolveDeferredLink())
            } catch (e: KwiklinkError) {
                renderError(e)
            }
        }
    }

    private enum class State { WAITING, RESOLVING, MATCHED, NOT_MATCHED, ERROR }

    private fun setStatusPill(state: State) {
        val (bgColor, fgColor, label) = when (state) {
            State.WAITING -> Triple(R.color.status_not_matched_bg, R.color.status_not_matched_fg, R.string.status_waiting)
            State.RESOLVING -> Triple(R.color.status_resolving_bg, R.color.status_resolving_fg, R.string.status_resolving)
            State.MATCHED -> Triple(R.color.status_matched_bg, R.color.status_matched_fg, R.string.status_matched)
            State.NOT_MATCHED -> Triple(R.color.status_not_matched_bg, R.color.status_not_matched_fg, R.string.status_not_matched)
            State.ERROR -> Triple(R.color.status_error_bg, R.color.status_error_fg, R.string.status_error)
        }
        (statusPill.background as GradientDrawable).setColor(getColor(bgColor))
        statusText.setTextColor(getColor(fgColor))
        statusText.setText(label)
        progressIndicator.visibility = if (state == State.RESOLVING) View.VISIBLE else View.GONE
    }

    private fun render(result: AttributionResult) {
        if (!result.matched) {
            setStatusPill(State.NOT_MATCHED)
            resultCard.visibility = View.GONE
            emptyStateText.setText(R.string.empty_state_not_matched)
            emptyStateText.visibility = View.VISIBLE
            return
        }

        setStatusPill(State.MATCHED)
        emptyStateText.visibility = View.GONE
        resultCard.visibility = View.VISIBLE

        linkIdText.text = result.linkId ?: ""

        val data = result.linkData
        val title = data?.getString("title")
        if (title != null) {
            titleText.text = title
            titleText.visibility = View.VISIBLE
        } else {
            titleText.visibility = View.GONE
        }

        detailsContainer.removeAllViews()
        data?.keys
            ?.filter { it != "title" }
            ?.sorted()
            ?.forEach { key -> detailsContainer.addView(buildDetailRow(key, data)) }
    }

    /** One "LABEL / value" row per remaining [LinkData] key — the reserved
     * `~`-prefixed keys (fallbackUrl, og_title, ...) render just like any
     * custom key a link's creator added, just with the `~` stripped from
     * the label for readability. */
    private fun buildDetailRow(key: String, data: LinkData): View {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(10) }
        }
        val label = TextView(this).apply {
            text = key.removePrefix("~")
            setTextColor(getColor(R.color.text_muted))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
            isAllCaps = true
        }
        val value = TextView(this).apply {
            text = data.getString(key)
                ?: data.getBooleanOrNull(key)?.toString()
                ?: data.getIntOrNull(key)?.toString()
                ?: data.getDoubleOrNull(key)?.toString()
                ?: ""
            setTextColor(getColor(R.color.text_primary))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            setTextIsSelectable(true)
        }
        row.addView(label)
        row.addView(value)
        return row
    }

    private fun renderError(e: KwiklinkError) {
        setStatusPill(State.ERROR)
        resultCard.visibility = View.GONE
        emptyStateText.text = e.message
        emptyStateText.visibility = View.VISIBLE
    }

    private fun dp(value: Int): Int =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value.toFloat(), resources.displayMetrics).toInt()
}
