package net.ccbluex.liquidbounce.config

import kotlinx.serialization.json.Json
import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.features.module.modules.player.autoshop.AutoShopConfig
import net.ccbluex.liquidbounce.features.module.modules.player.autoshop.ModuleAutoShop
import net.ccbluex.liquidbounce.features.module.modules.player.autoshop.AutoShopElement
import net.ccbluex.liquidbounce.features.module.modules.player.autoshop.RawShopConfig
import net.ccbluex.liquidbounce.utils.client.logger
import net.ccbluex.liquidbounce.utils.io.HttpClient
import net.minecraft.item.Item
import net.minecraft.registry.Registries
import net.minecraft.util.Identifier
import java.io.File

object AutoShopConfig {
    private val configFolder = File(
        ConfigSystem.rootFolder, "autoshop-configs"
    )

    private val jsonDecoder = Json { ignoreUnknownKeys = true }

    fun load(configFileName: String = ModuleAutoShop.configName): Boolean {
        try {
            val configFile = File(ConfigSystem.rootFolder, "autoshop-configs/$configFileName.json")
            val autoShopConfig = parseJsonFile(configFile)

            // add items to AutoShop
            ModuleAutoShop.disable()
            ModuleAutoShop.currentConfig = autoShopConfig
            ModuleAutoShop.prevCategorySlot = autoShopConfig.initialCategorySlot
            ModuleAutoShop.enable()
        } catch (throwable: Throwable) {
            logger.error("Failed to load items for AutoShop.", throwable)
            return false
        }

        return true
    }


    /**
     * Reads a raw shop config from a json file and returns a shop config ready to use
     */
    private fun parseJsonFile(file: File): AutoShopConfig {
        val json = file.readText()
        val rawShopConfig = jsonDecoder.decodeFromString<RawShopConfig>(json)
        val autoShopConfig = AutoShopConfig(
            rawShopConfig.traderTitle,
            rawShopConfig.initialCategorySlot,
            mutableListOf()
        )

        // parse shop elements
        rawShopConfig.items.forEach { rawShopElement ->
            val item = getItemFromID(rawShopElement.itemID)

            // Price block
            val price = rawShopElement.price.map { subList -> subList.associate {
                rawItemInfo -> Pair(
                    getItemFromID(rawItemInfo.itemID),
                    rawItemInfo.minAmount)
                }
            }

            // CheckItems block
            val itemsToCheckBeforeBuying = if (rawShopElement.checkItems == null) null
                                        else mutableListOf<Pair<Item, Int>>()

            rawShopElement.checkItems?.forEach { itemInfo ->
                itemsToCheckBeforeBuying?.add(Pair(
                    getItemFromID(itemInfo.itemID),
                    itemInfo.minAmount))
            }

            autoShopConfig.elements.add(
                AutoShopElement(
                item,
                rawShopElement.minAmount,
                rawShopElement.amountPerClick,
                rawShopElement.categorySlot,
                rawShopElement.itemSlot,
                price,
                itemsToCheckBeforeBuying
            ))
        }

        return autoShopConfig
    }

    private fun getItemFromID(id: String): Item {
        // TODO: improve it so potions can be used
        val newID = if (id.lowercase() == "wool") "blue_wool" else id
        return Registries.ITEM.get(Identifier(newID))
    }

    /**
     * Downloads the default autoShop configs from the cloud.
     */
    fun downloadDefaultConfigs() {
        if (configFolder.exists()) {
            return
        }

        configFolder.mkdir()
        this.runCatching {
            logger.info("Downloading the default AutoShop configs...")
            // TODO: make it download the whole folder
            HttpClient.download("${LiquidBounce.CLIENT_CLOUD}/autoshop-configs/dexland.json", configFolder.resolve("dexland.json"))
            HttpClient.download("${LiquidBounce.CLIENT_CLOUD}/autoshop-configs/pikanetwork.json", configFolder.resolve("pikanetwork.json"))
            HttpClient.download("${LiquidBounce.CLIENT_CLOUD}/autoshop-configs/pikanetwork2.json", configFolder.resolve("pikanetwork2.json"))
            HttpClient.download("${LiquidBounce.CLIENT_CLOUD}/autoshop-configs/test.json", configFolder.resolve("test.json"))
            logger.info("Successfully downloaded the default AutoShop configs")
        }.onFailure {
            logger.error("Failed to download the default AutoShop configs", it)
        }
    }
}
