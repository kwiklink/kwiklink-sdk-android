package io.kwiklink.android.sdk.model

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull

/**
 * The free-form payload attached to a link (backend: `repo.Link.LinkData`,
 * a `map[string]any`). A handful of `~`-prefixed keys are reserved and
 * interpreted by the redirect engine itself (`~androidUrl`, `~fallbackUrl`,
 * `~og_title`, ...) — everything else is whatever the link's creator put
 * there, so this wraps the raw JSON rather than a fixed data class.
 */
class LinkData internal constructor(private val raw: JsonObject) {

    val keys: Set<String> get() = raw.keys

    fun getString(key: String): String? =
        (raw[key] as? JsonPrimitive)?.let { if (it.isString) it.content else null }

    fun getBooleanOrNull(key: String): Boolean? = (raw[key] as? JsonPrimitive)?.booleanOrNull

    fun getIntOrNull(key: String): Int? = (raw[key] as? JsonPrimitive)?.intOrNull

    fun getDoubleOrNull(key: String): Double? = (raw[key] as? JsonPrimitive)?.doubleOrNull

    override fun toString(): String = raw.toString()

    companion object {
        val EMPTY = LinkData(JsonObject(emptyMap()))
    }
}
