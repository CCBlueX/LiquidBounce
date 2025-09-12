/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2025 CCBlueX
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

package net.ccbluex.liquidbounce.features.module.modules.misc.reporthelper

import net.ccbluex.liquidbounce.config.types.nesting.Choice
import net.ccbluex.liquidbounce.config.types.nesting.ChoiceConfigurable
import net.ccbluex.liquidbounce.config.types.nesting.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.events.ScreenEvent
import net.ccbluex.liquidbounce.event.sequenceHandler
import net.ccbluex.liquidbounce.utils.inventory.getSlotsInContainer
import net.ccbluex.liquidbounce.utils.inventory.syncId
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.item.Items
import net.minecraft.screen.slot.SlotActionType

internal object ReportHelperAutoConfirm : ToggleableConfigurable(ModuleReportHelper, "AutoConfirm", false) {

    private val mode = choices("Mode", 0) {
        arrayOf(Hypixel, Heypixel)
    }

    private sealed class Mode(name: String) : Choice(name) {
        final override val parent: ChoiceConfigurable<*>
            get() = mode

        protected abstract fun onScreenUpdated(screen: HandledScreen<*>)

        init {
            sequenceHandler<ScreenEvent> { event ->
                val screen = event.screen
                if (screen !is HandledScreen<*>) {
                    return@sequenceHandler
                }

                // Wait for screen update
                if (!waitConditional(5) { mc.currentScreen === screen }) {
                    return@sequenceHandler
                }

                onScreenUpdated(screen)
            }
        }
    }

    /**
     * Type: Confirm Screen (9x3)
     * Pattern:
     * - 9x air
     * - 2x air / yes / air / player head / air / no / 2x air
     * - 9x air
     */
    private object Hypixel : Mode("Hypixel") {
        private val emptyIndices = intArrayOf(
            0, 1, 2, 3, 4, 5, 6, 7, 8,
            9, 10, 12, 14, 16, 17,
            18, 19, 20, 21, 22, 23, 24, 25, 26
        )

        override fun onScreenUpdated(screen: HandledScreen<*>) {
            val slots = screen.getSlotsInContainer()
            if (slots.size != 27 || emptyIndices.any { !slots[it].itemStack.isEmpty }) {
                return
            }

            if (!slots[11].itemStack.isOf(Items.GREEN_TERRACOTTA) ||
                !slots[13].itemStack.isOf(Items.PLAYER_HEAD) ||
                !slots[15].itemStack.isOf(Items.RED_TERRACOTTA)) {
                return
            }

            interaction.clickSlot(
                screen.syncId,
                11,
                0,
                SlotActionType.PICKUP,
                player,
            )

            player.closeScreen()
        }
    }

    /**
     * Type: Selection Screen (9x1)
     * Pattern:
     * DiamondSword = report as hack
     */
    private object Heypixel : Mode("Heypixel") {
        override fun onScreenUpdated(screen: HandledScreen<*>) {
            val slots = screen.getSlotsInContainer()
            if (slots.size != 9) {
                return
            }

            val diamondSwordId = slots.firstOrNull { it.itemStack.isOf(Items.DIAMOND_SWORD) } ?: return

            interaction.clickSlot(
                screen.syncId,
                diamondSwordId.slotInContainer,
                0,
                SlotActionType.PICKUP,
                player,
            )

            player.closeScreen()
        }
    }
}
