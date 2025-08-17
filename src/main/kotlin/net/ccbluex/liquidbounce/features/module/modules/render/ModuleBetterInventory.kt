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

package net.ccbluex.liquidbounce.features.module.modules.render

import net.ccbluex.liquidbounce.config.types.nesting.Choice
import net.ccbluex.liquidbounce.config.types.nesting.ChoiceConfigurable
import net.ccbluex.liquidbounce.config.types.nesting.ToggleableConfigurable
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.utils.inventory.InventoryManager
import net.minecraft.client.gui.DrawContext
import net.minecraft.screen.slot.Slot

object ModuleBetterInventory : ClientModule("BetterInventory", Category.RENDER) {

    private object HighlightClicked : ToggleableConfigurable(this, "HighlightClicked", enabled = true) {
        val mode = choices("Mode", Mode.Border, arrayOf(Mode.Border))

        sealed class Mode(choiceName: String) : Choice(choiceName) {
            final override val parent: ChoiceConfigurable<*>
                get() = mode

            abstract fun drawHighlightSlot(drawContext: DrawContext, slot: Slot)

            object Border : Mode("Border") {
                private const val STACK_SIZE = 16
                val color by color("Color", Color4b.GREEN)

                override fun drawHighlightSlot(drawContext: DrawContext, slot: Slot) {
                    drawContext.drawBorder(slot.x, slot.y, STACK_SIZE, STACK_SIZE, color.toARGB())
                }
            }
        }
    }

    init {
        tree(HighlightClicked)
    }

    fun drawHighlightSlot(drawContext: DrawContext, slot: Slot) {
        if (!running) return

        HighlightClicked.mode.activeChoice.drawHighlightSlot(drawContext, slot)
    }

}
