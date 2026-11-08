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

import net.ccbluex.liquidbounce.features.module.modules.player.offhand.ModuleOffhand
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.inventory.InventoryScreen

/**
 * The Offhand Totem/Legit option: nothing is swapped until the cursor is on a totem in the open inventory, and then
 * that totem is the one taken.
 */
class OffhandLegitGameTest : FabricClientGameTest {

    override fun runTest(context: ClientGameTestContext) = context.inOffhandWorld {
        configure("Totem/Legit" to "true")
        giveTotems(2)
        context.onClient { ModuleOffhand.enabled = true }

        context.waitTicks(IDLE_TICKS)
        checkServer("Legit swapped with the inventory closed") { !it.offhandItem.isTotem() }

        context.input.pressKey { it.keyInventory }
        context.waitForScreen(InventoryScreen::class.java)
        context.input.setCursorPos(0.0, 0.0)
        context.waitTicks(IDLE_TICKS)
        checkServer("Legit swapped without the cursor on a totem") { !it.offhandItem.isTotem() }

        val (hovered, x, y) = context.fromClient { client -> lastTotemOnScreen(client) }
        context.input.setCursorPos(x, y)
        waitServer("the hovered totem in the offhand") { it.offhandItem.isTotem() }
        checkServer("Legit took another totem than the hovered one") {
            !it.inventory.getItem(hovered).isTotem() && it.inventoryTotems() == 1
        }

        context.setScreen { null }
    }

    /**
     * The inventory index and the window coordinates of the last totem in the open inventory screen.
     */
    private fun lastTotemOnScreen(client: Minecraft): Triple<Int, Double, Double> {
        val screen = client.gui.screen() as InventoryScreen
        val slot = screen.menu.slots.last { it.item.isTotem() }
        val scale = client.window.guiScale.toDouble()
        val left = (screen.width - INVENTORY_WIDTH) / 2
        val top = (screen.height - INVENTORY_HEIGHT) / 2
        return Triple(slot.containerSlot, (left + slot.x + SLOT_CENTER) * scale, (top + slot.y + SLOT_CENTER) * scale)
    }

    private companion object {
        const val IDLE_TICKS = 20
        const val INVENTORY_WIDTH = 176
        const val INVENTORY_HEIGHT = 166
        const val SLOT_CENTER = 8
    }

}
