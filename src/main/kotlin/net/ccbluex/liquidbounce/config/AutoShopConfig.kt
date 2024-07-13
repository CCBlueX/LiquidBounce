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
import net.ccbluex.liquidbounce.event.events.NotificationEvent
import net.ccbluex.liquidbounce.features.module.modules.player.autoshop.ModuleAutoShop
import net.ccbluex.liquidbounce.features.module.modules.player.autoshop.serializable.*
import net.ccbluex.liquidbounce.features.module.modules.player.autoshop.serializable.conditions.ConditionNode
import net.ccbluex.liquidbounce.features.module.modules.player.autoshop.serializable.conditions.ConditionNodeDeserializer
import net.ccbluex.liquidbounce.utils.client.logger
import net.ccbluex.liquidbounce.utils.client.notification

object AutoShopConfig {

    private val autoShopGson = GsonBuilder()
        .setPrettyPrinting()
        .registerTypeAdapter(ShopElement::class.javaObjectType, ShopElementDeserializer())
        .registerTypeAdapter(ItemInfo::class.javaObjectType, ItemInfoDeserializer())
        .registerTypeAdapter(ConditionNode::class.javaObjectType, ConditionNodeDeserializer())
        .create()

    /**
     * Loads [shopConfigPreset] and displays a notification depending on the result
     */
    fun loadAutoShopConfig(shopConfigPreset: ShopConfigPreset) : Boolean {
        val result = load(shopConfigPreset)
        val message = ModuleAutoShop.message(if (result) "reloadSuccess" else "loadError")

        notification(message, ModuleAutoShop.name,
            if (result) NotificationEvent.Severity.INFO else NotificationEvent.Severity.ERROR
        )
        return result
    }

    fun load(shopConfigPreset: ShopConfigPreset): Boolean {
        runCatching {
            javaClass.getResourceAsStream(shopConfigPreset.internalPath).use { inputStream ->
                check(inputStream != null) { "Failed to load resource: ${shopConfigPreset.internalPath}" }

                val shopConfig = autoShopGson.fromJson(inputStream.reader(), ShopConfig::class.java)

                // add items to AutoShop
                ModuleAutoShop.disable()
                ModuleAutoShop.currentConfig = shopConfig
                ModuleAutoShop.enable()
            }
        }.onFailure {
            logger.error("Failed to load items for AutoShop.", it)
            ModuleAutoShop.currentConfig = ShopConfig.emptyConfig()
            return false
        }

        return true
    }

}

/**
 * Represents the locally available shop configurations
 */
@Suppress("unused")
enum class ShopConfigPreset(override val choiceName: String, val localFileName: String) : NamedChoice {

    PIKA_NETWORK("PikaNetwork", "pika-network"),
    BLOCKSMC("BlocksMC", "blocksmc"),
    CUBECRAFT("CubeCraft", "cubecraft"),
    TEAMHOLY("TeamHoly", "teamholy"),
    DEXLAND("Dexland", "dexland");

    val internalPath = "/assets/liquidbounce/data/shops/${localFileName}.json"

}
