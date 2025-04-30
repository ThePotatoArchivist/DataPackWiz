package archives.tater.datapackwiz.lib

import archives.tater.datapackwiz.DataPackWiz
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive

fun JsonObject(vararg entries: Pair<String, JsonElement>) = JsonObject().apply {
    for ((key, value) in entries) {
        this[key] = value
    }
}

@JvmName("JsonObjectFromObjects")
fun JsonObject(vararg entries: Pair<String, Any>) = JsonObject().apply {
    for ((key, value) in entries)
        this[key] = when (value) {
            is JsonElement -> value
            is Number -> JsonPrimitive(value)
            is Char -> JsonPrimitive(value)
            is String -> JsonPrimitive(value)
            is Boolean -> JsonPrimitive(value)
            else -> {
                DataPackWiz.logger.warn("Invalid element when creating JsonObject: $value")
                continue
            }
        }
}

operator fun JsonObject.set(property: String, value: JsonElement) {
    add(property, value)
}
operator fun JsonObject.set(property: String, value: Number) {
    addProperty(property, value)
}
operator fun JsonObject.set(property: String, value: Char) {
    addProperty(property, value)
}
operator fun JsonObject.set(property: String, value: String) {
    addProperty(property, value)
}
operator fun JsonObject.set(property: String, value: Boolean) {
    addProperty(property, value)
}

@JvmName("toJsonElementArray") fun Iterable<JsonElement>.toJsonArray() = JsonArray().also { forEach(it::add) }
@JvmName("toJsonStringArray") fun Iterable<String>.toJsonArray() = JsonArray().also { forEach(it::add) }
@JvmName("toJsonNumberArray") fun Iterable<Number>.toJsonArray() = JsonArray().also { forEach(it::add) }
@JvmName("toJsonCharArray") fun Iterable<Char>.toJsonArray() = JsonArray().also { forEach(it::add) }
@JvmName("toJsonBooleanArray") fun Iterable<Boolean>.toJsonArray() = JsonArray().also { forEach(it::add) }
