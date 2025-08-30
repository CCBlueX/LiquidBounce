package net.ccbluex.liquidbounce.config.gson.adapter

import com.google.gson.*
import net.minecraft.util.Identifier
import java.lang.reflect.Type

object IdentifierAdapter : JsonSerializer<Identifier>, JsonDeserializer<Identifier> {
    override fun serialize(src: Identifier?, typeOfSrc: Type, context: JsonSerializationContext) =
        src?.let { JsonPrimitive(it.toString()) }
    override fun deserialize(json: JsonElement, typeOfT: Type?, context: JsonDeserializationContext?): Identifier? =
        Identifier.tryParse(json.asString)
}
