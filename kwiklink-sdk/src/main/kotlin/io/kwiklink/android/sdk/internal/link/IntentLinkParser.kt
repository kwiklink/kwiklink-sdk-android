package io.kwiklink.android.sdk.internal.link

import android.content.Intent

/**
 * Pulls a domain+slug pair out of the `Intent` an App Link tap hands the
 * activity. [parseUri] takes plain host/path values rather than an
 * `android.net.Uri` so it's testable on the JVM without Robolectric — the
 * one-line [parse] wrapper around the real `Intent.data` is the only piece
 * that touches the Android framework.
 */
internal object IntentLinkParser {

    data class ParsedLink(val domain: String, val slug: String)

    fun parse(intent: Intent): ParsedLink? {
        val uri = intent.data ?: return null
        return parseUri(uri.host, uri.pathSegments)
    }

    internal fun parseUri(host: String?, pathSegments: List<String>): ParsedLink? {
        if (host.isNullOrBlank()) return null
        // A short link is exactly one path segment (the slug) — reject
        // anything else rather than guessing which segment was meant.
        val slug = pathSegments.singleOrNull { it.isNotBlank() } ?: return null
        return ParsedLink(domain = host, slug = slug)
    }
}
