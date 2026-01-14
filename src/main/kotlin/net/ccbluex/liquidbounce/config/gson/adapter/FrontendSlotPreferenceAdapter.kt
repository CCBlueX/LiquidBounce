@file:Suppress("WildcardImport")

package net.ccbluex.liquidbounce.config.gson.adapter

import com.google.gson.*
import net.ccbluex.liquidbounce.features.inventoryPreset.FrontendSlotPreference
import net.ccbluex.liquidbounce.features.inventoryPreset.FrontendSlotPreference.GroupSlotPreference.ItemGroupType
import net.minecraft.item.Item
import java.lang.reflect.Type

object FrontendSlotPreferenceAdapter :
    JsonSerializer<FrontendSlotPreference>, JsonDeserializer<FrontendSlotPreference> {
    override fun serialize(
        src: FrontendSlotPreference,
        typeOfSrc: Type?,
        context: JsonSerializationContext
    ): JsonObject {
        return src.serialize(context)
    }

    override fun deserialize(
        json: JsonElement,
        typeOfT: Type?,
        context: JsonDeserializationContext
    ): FrontendSlotPreference {
        val obj = json.asJsonObject

        return when (obj["type"].asString) {
            "SINGLE" -> FrontendSlotPreference.SingleSlotPreference(
                context.deserialize(
                    obj["item"],
                    Item::class.java
                )
            )
            "GROUP" -> FrontendSlotPreference.GroupSlotPreference(
                context.deserialize(
                    obj["group"],
                    ItemGroupType::class.java
                )
            )
            "IGNORE" -> FrontendSlotPreference.IgnoreSlotPreference
            "ANY" -> FrontendSlotPreference.AnySlotPreference
            else -> error("Unknown slot preference ${obj["type"]}")
        }
    }
}
