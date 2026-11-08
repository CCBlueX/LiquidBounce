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
package net.ccbluex.liquidbounce.gametest

import net.ccbluex.liquidbounce.config.ConfigSystem
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.ModuleInventoryCleaner
import net.ccbluex.liquidbounce.features.module.modules.player.offhand.ModuleOffhand
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext

/**
 * The InventoryCleaner Totem sort choice next to Offhand: the hotbar slot gets a totem from the inventory, and
 * another one once Offhand moved it to the offhand.
 */
class InventoryCleanerTotemSlotGameTest : FabricClientGameTest {

    override fun runTest(context: ClientGameTestContext) = context.inOffhandWorld {
        val saved = context.fromClient { ConfigSystem.serializeValueGroup(ModuleInventoryCleaner) }

        try {
            giveTotems(3)
            context.onClient {
                for (slot in 1 until TOTEM_SLOT) {
                    ModuleInventoryCleaner.set("SlotItem-$slot", "None")
                }
                ModuleInventoryCleaner.set("SlotItem-$TOTEM_SLOT", "Totem")
                ModuleInventoryCleaner.enabled = true
                ModuleOffhand.enabled = true
            }

            waitServer("a totem in the offhand and in hotbar slot $TOTEM_SLOT") {
                it.offhandItem.isTotem() && it.inventory.getItem(TOTEM_SLOT - 1).isTotem() && it.inventoryTotems() == 1
            }

            removeOffhand()
            waitServer("the offhand and hotbar slot $TOTEM_SLOT refilled") {
                it.offhandItem.isTotem() && it.inventory.getItem(TOTEM_SLOT - 1).isTotem() && it.inventoryTotems() == 0
            }
        } finally {
            context.onClient {
                ModuleInventoryCleaner.enabled = false
                ConfigSystem.deserializeValueGroup(ModuleInventoryCleaner, saved)
            }
        }
    }

    private companion object {
        const val TOTEM_SLOT = 9
    }

}
