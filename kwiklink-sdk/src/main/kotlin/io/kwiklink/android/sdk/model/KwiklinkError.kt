package io.kwiklink.android.sdk.model

/**
 * Thrown for anything that isn't a normal "no match" outcome (that's
 * [AttributionResult.matched] = false instead). Sealed so callers can
 * exhaustively `when` on failure cause.
 */
sealed class KwiklinkError(message: String, cause: Throwable? = null) : Exception(message, cause) {

    class NotInitialized : KwiklinkError("Kwiklink.init() must be called before this method")

    class Network(cause: Throwable) : KwiklinkError("network request failed", cause)

    class Server(val statusCode: Int, val body: String?) :
        KwiklinkError("server returned HTTP $statusCode")

    class InvalidResponse(cause: Throwable? = null) :
        KwiklinkError("could not parse the server's response", cause)
}
