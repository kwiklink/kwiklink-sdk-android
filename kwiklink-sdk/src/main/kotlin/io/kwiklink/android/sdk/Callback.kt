package io.kwiklink.android.sdk

/** Java-friendly counterpart to the `suspend fun` entry points. */
interface Callback<T> {
    fun onSuccess(result: T)
    fun onError(error: Throwable)
}
