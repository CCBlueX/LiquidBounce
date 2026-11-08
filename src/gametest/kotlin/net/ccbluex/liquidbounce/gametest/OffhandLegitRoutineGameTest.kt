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

import com.mojang.blaze3d.platform.InputConstants
import net.ccbluex.liquidbounce.features.module.modules.player.offhand.ModuleOffhand
import net.ccbluex.liquidbounce.gametest.OffhandFixture.Companion.TIMEOUT
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.inventory.InventoryScreen

/**
 * The Offhand Totem/LegitRoutine option: the inventory opens on its own, the cursor travels to a totem, the user's
 * clicks and hotbar keys do nothing while it rests there, and the inventory closes after the swap.
 */
class OffhandLegitRoutineGameTest : FabricClientGameTest {

    override fun runTest(context: ClientGameTestContext) = context.inOffhandWorld {
        configure(
            "Totem/Legit" to "true",
            "Totem/LegitRoutine/Enabled" to "true",
            "Totem/LegitRoutine/Reaction" to "0..0",
            "Totem/LegitRoutine/MoveTime" to "$MOVE_MS..$MOVE_MS",
            "Constraints/StartDelay" to "$START_DELAY_TICKS..$START_DELAY_TICKS"
        )
        giveTotems(2)
        context.onClient { ModuleOffhand.enabled = true }

        context.waitFor({ client -> client.gui.screen() is InventoryScreen }, TIMEOUT)
        // the cursor comes to rest on the totem it went for
        val positions = mutableListOf<Pair<Double, Double>>()
        context.waitFor({ client ->
            val position = client.mouseHandler.xpos() to client.mouseHandler.ypos()
            val resting = position == positions.lastOrNull()
            positions += position
            resting && hoveredTotem(client) != null
        }, TIMEOUT)
        check(positions.distinct().size >= MIN_CURSOR_STEPS) { "The cursor jumped to the totem: $positions" }

        // what the user does while the cursor rests on the totem is ignored
        val hovered = context.fromClient { client -> hoveredTotem(client)!! }
        context.input.pressKey(InputConstants.KEY_2)
        context.input.pressMouse(InputConstants.MOUSE_BUTTON_LEFT)
        context.waitTicks(INPUT_TICKS)
        check(context.fromClient { client -> client.player!!.containerMenu.carried.isEmpty }) {
            "A click picked up the hovered totem"
        }
        checkServer("A hotbar key or click moved the hovered totem") {
            it.inventory.getItem(hovered).isTotem() && !it.inventory.getItem(1).isTotem() &&
                it.containerMenu.carried.isEmpty
        }
        checkServer("Swapped before the start delay") { !it.offhandItem.isTotem() }

        waitServer("the hovered totem in the offhand") {
            it.offhandItem.isTotem() && !it.inventory.getItem(hovered).isTotem()
        }
        context.waitFor({ client -> client.gui.screen() == null }, TIMEOUT)
    }

    /**
     * The inventory index of the totem under the cursor.
     */
    private fun hoveredTotem(client: Minecraft): Int? {
        val slot = (client.gui.screen() as? InventoryScreen)?.hoveredSlot ?: return null
        return slot.containerSlot.takeIf { slot.container === client.player!!.inventory && slot.item.isTotem() }
    }

    private companion object {
        const val MOVE_MS = 500
        const val START_DELAY_TICKS = 20
        const val MIN_CURSOR_STEPS = 4
        const val INPUT_TICKS = 3
    }

}
