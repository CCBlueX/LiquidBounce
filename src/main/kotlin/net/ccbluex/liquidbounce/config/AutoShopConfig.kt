package net.ccbluex.liquidbounce.config

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import net.ccbluex.liquidbounce.features.module.modules.player.autoshop.ModuleAutoShop
import net.ccbluex.liquidbounce.features.module.modules.player.autoshop.ShopElement
import net.ccbluex.liquidbounce.utils.client.logger
import net.minecraft.item.Item
import net.minecraft.registry.Registries
import net.minecraft.util.Identifier
import java.io.File

object AutoShopConfig {
    val configFolder = File(
        ConfigSystem.rootFolder, "autoshop-configs"
    ).apply {
        if (!exists()) {
            mkdir()
            // TODO: load the default config from LiquidCloud
        }
    }

    fun load(configFileName: String = ModuleAutoShop.configName): Boolean {
        val configFile = File(ConfigSystem.rootFolder, "autoshop-configs/$configFileName.json")

        try {
            val jsonObject = JsonParser().parse(configFile.bufferedReader()).asJsonObject
            val items = jsonObject.get("Items").asJsonArray

            val shopElements = mutableListOf<ShopElement>()
            for (jsonElement in items) {
                val currShopElement = getShopElement(jsonElement.asJsonObject)
                shopElements += currShopElement
            }

            // add the items to AutoShop
            ModuleAutoShop.shopElements.clear()
            ModuleAutoShop.shopElements += shopElements
            ModuleAutoShop.traderTitle = jsonObject.get("TraderTitle").asString
            ModuleAutoShop.initialCategorySlot = jsonObject.get("InitialCategorySlot").asInt
            ModuleAutoShop.prevCategorySlot = ModuleAutoShop.initialCategorySlot
        } catch (throwable: Throwable) {
            logger.error("Failed to load items for AutoShop.", throwable)
            return false
        }

        return true
    }

    /**
     * Returns a shop element from [jsonObject]
     */
    private fun getShopElement(jsonObject: JsonObject) : ShopElement {
        val item = getItemFromID(jsonObject.get("ItemID").asString)     // desired item
        val minAmount = jsonObject.get("MinAmount").asInt     // desired item amount
        val amountPerClick = jsonObject.get("AmountPerClick")?.asInt ?: 1 // item amount can be received per 1 click

        // it's about how to buy it (slots needed to click in order to buy an item)
        val categorySlot = jsonObject.get("CategorySlot").asInt
        val itemSlot = jsonObject.get("ItemSlot").asInt

        // price, basically, is a set of pairs of items and their amounts
        val price = jsonObject.get("Price")
            .asJsonArray
            .map { subArray -> subArray.asJsonArray.associate {
                    element -> Pair(
                        getItemFromID(element.asJsonObject.get("ItemID").asString),
                        element.asJsonObject.get("MinAmount").asInt)
                }
            }

        // items needed to be checked (if the player has something of those it shouldn't buy an item)
        // for example, it's useless to buy a stone sword if the player already has an iron sword or even a diamond one
        val checkItems = jsonObject.get("CheckItems")
        val itemsToCheckBeforeBuying = checkItems?.asJsonArray?.map { element -> Pair(
            getItemFromID(element.asJsonObject.get("ItemID").asString),
            element.asJsonObject.get("MinAmount").asInt) }

        return ShopElement(
            item,
            minAmount,
            amountPerClick,
            categorySlot,
            itemSlot,
            price,
            itemsToCheckBeforeBuying)
    }

    private fun getItemFromID(id: String): Item {
        // TODO: improve it so potions can be used
        val newID = if (id.lowercase() == "wool") "blue_wool" else id
        return Registries.ITEM.get(Identifier(newID))
    }
}
