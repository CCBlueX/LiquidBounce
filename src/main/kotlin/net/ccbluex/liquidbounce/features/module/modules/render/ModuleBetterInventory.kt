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

import net.ccbluex.liquidbounce.config.types.nesting.ToggleableConfigurable
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.utils.inventory.InventoryManager
import net.minecraft.screen.slot.Slot

object ModuleBetterInventory : ClientModule("BetterInventory", Category.RENDER) {

    private val highlightClicked = object : ToggleableConfigurable(this, "HighlightClicked", enabled = true) {
        val color by color("Color", Color4b.GREEN)
    }

    init {
        tree(highlightClicked)
    }

    fun Slot.highlightColor(): Color4b? =
        if (running && highlightClicked.enabled && id == InventoryManager.lastClickedSlot) {
            highlightClicked.color
        } else {
            null
        }

}
