/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2024 CCBlueX
 *
 * LiquidBounce is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LiquidBounce is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LiquidBounce. If not, see <https://www.gnu.org/licenses/>.
 */
package net.ccbluex.liquidbounce.config

import com.google.gson.GsonBuilder
import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.config.util.decode
import net.ccbluex.liquidbounce.event.events.NotificationEvent
import net.ccbluex.liquidbounce.features.module.modules.player.autoshop.*
import net.ccbluex.liquidbounce.features.module.modules.player.autoshop.serializable.ShopConfig
import net.ccbluex.liquidbounce.features.module.modules.player.autoshop.serializable.conditions.ConditionNode
import net.ccbluex.liquidbounce.features.module.modules.player.autoshop.serializable.conditions.ConditionNodeDeserializer
import net.ccbluex.liquidbounce.utils.client.logger
import net.ccbluex.liquidbounce.utils.client.notification
import net.ccbluex.liquidbounce.utils.io.HttpClient
import java.io.File

object AutoShopConfig {

    private val configFolder = ConfigSystem.rootFolder.resolve("autoshop-configs")

    private val autoShopGson = GsonBuilder()
        .setPrettyPrinting()
        .registerTypeAdapter(ConditionNode::class.javaObjectType, ConditionNodeDeserializer())
        .create()

    /**
     * Loads [configFileName] and displays a notification depending on the result
     */
    fun loadAutoShopConfig(configFileName: String) : Boolean {
        val result = load(configFileName)
        val message = ModuleAutoShop.message(if (result) "reloadSuccess" else "loadError")

        notification(message, ModuleAutoShop.name,
            if (result) NotificationEvent.Severity.INFO else NotificationEvent.Severity.ERROR
        )
        return result
    }

    fun load(configFileName: String = ModuleAutoShop.configName): Boolean {
        runCatching {
            val configFile = File(ConfigSystem.rootFolder, "autoshop-configs/$configFileName.json")
            val shopConfig = autoShopGson.fromJson(configFile.readText(), ShopConfig::class.java)

            // add items to AutoShop
            ModuleAutoShop.disable()
            ModuleAutoShop.currentConfig = shopConfig
            ModuleAutoShop.enable()
        }.onFailure {
            logger.error("Failed to load items for AutoShop.", it)
            ModuleAutoShop.currentConfig = ShopConfig.emptyConfig()
            return false
        }

        return true
    }

    /**
     * Downloads the default AutoShop configs from the cloud.
     */
    fun downloadDefaultConfigs() {
        if (configFolder.exists()) {
            return
        }

        configFolder.mkdir()
        this.runCatching {
            // TODO: make it download all the configs under the 'autoshop-configs' directory
            logger.info("Downloading the default AutoShop configs...")
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
