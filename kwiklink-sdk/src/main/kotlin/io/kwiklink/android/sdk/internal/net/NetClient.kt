package io.kwiklink.android.sdk.internal.net

import io.kwiklink.android.sdk.internal.log.KwiklinkLog
import io.kwiklink.android.sdk.model.KwiklinkError
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

/** What a GET returned, once HTTP-level and decode-level errors are ruled out. */
internal sealed class NetResult<out T> {
    data class Success<T>(val value: T) : NetResult<T>()
    data object NotFound : NetResult<Nothing>()
}

/**
 * Thin OkHttp + kotlinx.serialization wrapper shared by every backend call
 * the SDK makes. A non-2xx/404 response and a decode failure both become a
 * [KwiklinkError] — a 404 becomes [NetResult.NotFound] instead, since every
 * endpoint this SDK calls uses 404 for an ordinary "no match" outcome, not
 * a real error.
 */
internal class NetClient(private val httpClient: OkHttpClient = OkHttpClient()) {
    // encodeDefaults matters for request bodies specifically: kotlinx.serialization
    // omits a property left at its default value by default, and
    // AttributionMatchRequestDto.platform defaults to "android" — dropping
    // it would leave the backend's platform fallback to sniff it from the
    // User-Agent header instead, which OkHttp's default UA won't match.
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    suspend fun <T> get(url: HttpUrl, apiKey: String, serializer: KSerializer<T>): NetResult<T> =
        withContext(Dispatchers.IO) {
            val request = Request.Builder().url(url).header("X-Api-Key", apiKey).get().build()
            val startMs = System.currentTimeMillis()
            // encodedPath only — never the full url, which for this call
            // carries domain/slug as query parameters (see KwiklinkLog's
            // doc comment on why those aren't logged even though they
            // aren't secrets in the auth sense).
            val path = url.encodedPath
            val response = try {
                httpClient.newCall(request).execute()
            } catch (e: IOException) {
                KwiklinkLog.e("GET $path failed (network)", e)
                throw KwiklinkError.Network(e)
            }
            response.use {
                val elapsedMs = System.currentTimeMillis() - startMs
                val bodyStr = it.body?.string().orEmpty()
                when {
                    it.isSuccessful -> {
                        KwiklinkLog.d("GET $path -> ${it.code} (${elapsedMs}ms)")
                        NetResult.Success(decode(bodyStr, serializer))
                    }
                    it.code == 404 -> {
                        KwiklinkLog.d("GET $path -> 404 (${elapsedMs}ms)")
                        NetResult.NotFound
                    }
                    else -> {
                        KwiklinkLog.e("GET $path -> ${it.code} (${elapsedMs}ms)")
                        throw KwiklinkError.Server(it.code, bodyStr.ifBlank { null })
                    }
                }
            }
        }

    /**
     * `/android/v1/attribution/match`'s only current caller: unlike [get], a 404
     * here still carries a decodable body (`{"matched": false}`, per
     * `HandleAttributionMatch`) rather than an error envelope, so both 2xx
     * and 404 decode straight to [TRes] instead of going through
     * [NetResult].
     */
    suspend fun <TReq, TRes> post(
        url: HttpUrl,
        apiKey: String,
        body: TReq,
        requestSerializer: KSerializer<TReq>,
        responseSerializer: KSerializer<TRes>,
    ): TRes = withContext(Dispatchers.IO) {
        val requestBody = json.encodeToString(requestSerializer, body)
            .toRequestBody("application/json".toMediaType())
        val request = Request.Builder().url(url).header("X-Api-Key", apiKey).post(requestBody).build()
        val startMs = System.currentTimeMillis()
        val path = url.encodedPath
        val response = try {
            httpClient.newCall(request).execute()
        } catch (e: IOException) {
            KwiklinkLog.e("POST $path failed (network)", e)
            throw KwiklinkError.Network(e)
        }
        response.use {
            val elapsedMs = System.currentTimeMillis() - startMs
            val bodyStr = it.body?.string().orEmpty()
            if (it.isSuccessful || it.code == 404) {
                KwiklinkLog.d("POST $path -> ${it.code} (${elapsedMs}ms)")
                decode(bodyStr, responseSerializer)
            } else {
                KwiklinkLog.e("POST $path -> ${it.code} (${elapsedMs}ms)")
                throw KwiklinkError.Server(it.code, bodyStr.ifBlank { null })
            }
        }
    }

    private fun <T> decode(body: String, serializer: KSerializer<T>): T =
        try {
            json.decodeFromString(serializer, body)
        } catch (e: SerializationException) {
            // Never log `body` itself — it's exactly the linkData/attribution
            // payload KwiklinkLog's doc comment says not to.
            KwiklinkLog.e("Failed to decode response body", e)
            throw KwiklinkError.InvalidResponse(e)
        }
}
