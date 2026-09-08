package io.kwiklink.android.sdk.internal.net

import io.kwiklink.android.sdk.BuildConfig

/**
 * Kwiklink's own SDK-facing API domain — still not customer-configurable (no
 * [io.kwiklink.android.sdk.KwiklinkConfig] field exposes it; a host app integrating
 * this SDK has no way to change it at runtime), but now picked per build
 * flavor (`dev`/`prod`, see kwiklink-sdk/build.gradle.kts's `productFlavors`)
 * so this repo's own dev/prod builds can point at different backends without
 * hand-editing this file. Every host app's SDK traffic goes here regardless
 * of which short-link domain a given link belongs to; `domain` travels as a
 * request parameter instead (see [io.kwiklink.android.sdk.internal.link.LinkResolver]).
 * Not the same as a customer's own short-link domain (e.g. `1kwik.link`) —
 * those still serve real redirects/App Links, but never SDK API traffic
 * directly.
 */
internal val API_HOST: String = BuildConfig.API_HOST
