package net.ccbluex.liquidbounce.config.gson.adapter

import com.google.gson.*
import com.google.gson.reflect.TypeToken
import net.ccbluex.liquidbounce.features.inventoryPreset.InventoryPreset
import net.ccbluex.liquidbounce.features.inventoryPreset.FrontendSlotPreference
import net.ccbluex.liquidbounce.features.inventoryPreset.FrontendItemLimitRules
import net.ccbluex.liquidbounce.utils.kotlin.mapArray
import java.lang.reflect.Type

object InventoryPresetAdapter : JsonSerializer<InventoryPreset>, JsonDeserializer<InventoryPreset> {
    override fun serialize(
        src: InventoryPreset,
        typeOfSrc: Type,
        context: JsonSerializationContext
    ): JsonElement = JsonObject().apply {
        add("items", context.serialize(src.itemRulesToArray()))
        add("maxStacks", context.serialize(src.itemLimitRules))
    }

    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext
    ): InventoryPreset = with (json.asJsonObject) {
        val items = getAsJsonArray("items").map { context.decode<List<FrontendSlotPreference>>(it) }
        val throws = getAsJsonArray("maxStacks").map { context.decode<FrontendItemLimitRules>(it) }

        return InventoryPreset(items.toTypedArray(), throws)
    }

    private inline fun <reified T> JsonDeserializationContext.decode(element: JsonElement): T {
        return deserialize(element, object: TypeToken<T>() {}.type)
    }
}
