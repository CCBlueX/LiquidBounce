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

package net.ccbluex.liquidbounce.features.module.modules.misc.reporthelper

import net.ccbluex.liquidbounce.config.types.group.ModeValueGroup
import net.ccbluex.liquidbounce.config.types.group.ToggleableValueGroup
import net.ccbluex.liquidbounce.event.events.ScreenEvent
import net.ccbluex.liquidbounce.event.sequenceHandler
import net.ccbluex.liquidbounce.event.tickConditional
import net.ccbluex.liquidbounce.utils.inventory.getSlotsInContainer
import net.ccbluex.liquidbounce.utils.inventory.syncId
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.world.inventory.ClickType
import net.minecraft.world.item.Items

internal object ReportHelperAutoConfirm : ToggleableValueGroup(ModuleReportHelper, "AutoConfirm", false) {

    private val mode = choices("Mode", 0) {
        arrayOf(Hypixel, Heypixel)
    }

    private sealed class Mode(name: String) : net.ccbluex.liquidbounce.config.types.group.Mode(name) {
        final override val parent: ModeValueGroup<*>
            get() = mode

        protected abstract fun onScreenUpdated(screen: AbstractContainerScreen<*>)

        init {
            sequenceHandler<ScreenEvent> { event ->
                val screen = event.screen
                if (screen !is AbstractContainerScreen<*>) {
                    return@sequenceHandler
                }

                // Wait for screen update
                if (tickConditional(5) { mc.screen === screen }) {
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

        override fun onScreenUpdated(screen: AbstractContainerScreen<*>) {
            val slots = screen.getSlotsInContainer()
            if (slots.size != 27 || emptyIndices.any { !slots[it].itemStack.isEmpty }) {
                return
            }

            if (!slots[11].itemStack.`is`(Items.GREEN_TERRACOTTA) ||
                !slots[13].itemStack.`is`(Items.PLAYER_HEAD) ||
                !slots[15].itemStack.`is`(Items.RED_TERRACOTTA)) {
                return
            }

            interaction.handleInventoryMouseClick(
                screen.syncId,
                11,
                0,
                ClickType.PICKUP,
                player,
            )

            player.clientSideCloseContainer()
        }
    }

    /**
     * Type: Selection Screen (9x1)
     * Pattern:
     * DiamondSword = report as hack
     */
    private object Heypixel : Mode("Heypixel") {
        override fun onScreenUpdated(screen: AbstractContainerScreen<*>) {
            val slots = screen.getSlotsInContainer()
            if (slots.size != 9) {
                return
            }

            val diamondSwordId = slots.firstOrNull { it.itemStack.`is`(Items.DIAMOND_SWORD) } ?: return

            interaction.handleInventoryMouseClick(
                screen.syncId,
                diamondSwordId.slotInContainer,
                0,
                ClickType.PICKUP,
                player,
            )

            player.clientSideCloseContainer()
        }
    }
}
