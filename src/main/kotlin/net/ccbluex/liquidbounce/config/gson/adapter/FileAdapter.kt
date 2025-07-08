package net.ccbluex.liquidbounce.config.gson.adapter

import com.google.gson.*
import java.io.File
import java.lang.reflect.Type

object FileAdapter : JsonSerializer<File>, JsonDeserializer<File> {
    override fun serialize(src: File?, typeOfSrc: Type?, context: JsonSerializationContext?): JsonElement? {
        if (src == null) {
            return null
        }
        return JsonPrimitive(src.path)
    }

    override fun deserialize(json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext?): File? {
        if (json == null || json.isJsonNull) {
            return null
        }
        return File(json.asString)
    }
}
