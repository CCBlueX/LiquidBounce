package net.ccbluex.liquidbounce.config

import kotlinx.serialization.json.Json
import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.features.module.modules.player.autoshop.*
import net.ccbluex.liquidbounce.features.module.modules.player.autoshop.ShopConfig
import net.ccbluex.liquidbounce.utils.client.logger
import net.ccbluex.liquidbounce.utils.io.HttpClient
import java.io.File

object AutoShopConfig {
    private val configFolder = File(
        ConfigSystem.rootFolder, "autoshop-configs"
    )

    private val jsonDecoder = Json { ignoreUnknownKeys = true }

    fun load(configFileName: String = ModuleAutoShop.configName): Boolean {
        try {
            val configFile = File(ConfigSystem.rootFolder, "autoshop-configs/$configFileName.json")
            val shopConfig = jsonDecoder.decodeFromString<ShopConfig>(configFile.readText())

            // add items to AutoShop
            ModuleAutoShop.disable()
            ModuleAutoShop.currentConfig = shopConfig
            ModuleAutoShop.prevCategorySlot = shopConfig.initialCategorySlot
            ModuleAutoShop.enable()
        } catch (throwable: Throwable) {
            logger.error("Failed to load items for AutoShop.", throwable)
            ModuleAutoShop.currentConfig = ShopConfig.emptyConfig()
            return false
        }

        return true
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
            HttpClient.download(
                "${LiquidBounce.CLIENT_CLOUD}/autoshop-configs/dexland.json",
                configFolder.resolve("dexland.json"))
            HttpClient.download(
                "${LiquidBounce.CLIENT_CLOUD}/autoshop-configs/pikanetwork.json",
                configFolder.resolve("pikanetwork.json"))
            HttpClient.download(
                "${LiquidBounce.CLIENT_CLOUD}/autoshop-configs/pikanetwork2.json",
                configFolder.resolve("pikanetwork2.json"))
            HttpClient.download(
                "${LiquidBounce.CLIENT_CLOUD}/autoshop-configs/test.json",
                configFolder.resolve("test.json"))
            logger.info("Successfully downloaded the default AutoShop configs")
        }.onFailure {
            logger.error("Failed to download the default AutoShop configs", it)
        }
    }
}
