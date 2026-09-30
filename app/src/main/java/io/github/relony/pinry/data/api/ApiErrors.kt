package io.github.relony.pinry.data.api

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import retrofit2.HttpException

/**
 * Field → message from a Pinry error body. DRF sends `{"field": ["msg"]}` or `{"field": "msg"}`;
 * the login view sends JSON labelled as text/html. Empty when the body isn't a JSON object.
 */
fun HttpException.fieldErrors(): Map<String, String> {
    val body = response()?.errorBody()?.string().orEmpty()
    val json = runCatching { PinryJson.parseToJsonElement(body) }.getOrNull() as? JsonObject
        ?: return emptyMap()
    return json.mapValues { (_, value) ->
        when (value) {
            is JsonPrimitive -> value.content
            is JsonArray -> value.joinToString(" ") { (it as? JsonPrimitive)?.content ?: it.toString() }
            else -> value.toString()
        }
    }
}
