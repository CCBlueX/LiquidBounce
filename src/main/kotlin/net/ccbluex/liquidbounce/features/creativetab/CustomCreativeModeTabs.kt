/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2026 CCBlueX
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
package net.ccbluex.liquidbounce.features.creativetab

import net.ccbluex.liquidbounce.features.creativetab.tabs.ExploitsCreativeModeTab
import net.ccbluex.liquidbounce.features.creativetab.tabs.HeadsCreativeModeTab
import net.ccbluex.liquidbounce.utils.client.logger

/**
 * LiquidBounce Creative Item Groups with useful items and blocks
 *
 * @depends FabricAPI (for page buttons)
 */
object CustomCreativeModeTabs {

    private var isInitialized = false

    /**
     * Since 1.20 we need to set this up at a more precise timing than just when the client starts.
     */
    fun init() {
        if (isInitialized) {
            return
        }

        // Check if FabricAPI is installed, otherwise we can't use the page buttons
        // Use net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab
        runCatching {
            Class.forName("net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab")
        }.onFailure {
            logger.error("FabricAPI is not installed, please install it to use the page buttons " +
                "in the creative inventory")
        }.onSuccess {
            runCatching {
                val creativeTabs = arrayOf(
                    HeadsCreativeModeTab(),
                    ExploitsCreativeModeTab()
                )

                for (creativeTab in creativeTabs) {
                    creativeTab.init()
                }
                isInitialized = true

                creativeTabs
            }.onFailure { exception ->
                logger.error("Unable to setup creative tabs", exception)
            }.onSuccess { creativeTabs ->
                logger.info("Creative Tabs: [ ${creativeTabs.joinToString { tab -> tab.plainName }} ]")
            }
        }
    }

}
