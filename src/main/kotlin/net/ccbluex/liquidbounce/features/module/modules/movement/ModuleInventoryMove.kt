/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2023 CCBlueX
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

package net.ccbluex.liquidbounce.features.module.modules.movement

import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.minecraft.client.gui.screen.ChatScreen
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.client.option.KeyBinding
import net.minecraft.item.ItemGroups

/**
 * InventoryMove module
 *
 * Allows you to walk while an inventory is opened.
 */

object ModuleInventoryMove : Module("InventoryMove", Category.MOVEMENT) {

    val undetectable by boolean("Undetectable", false)
    val passthroughSneak by boolean("PassthroughSneak", false)

    fun shouldHandleInputs(keyBinding: KeyBinding) =
        enabled && mc.currentScreen !is ChatScreen && !isInCreativeSearchField() && (!undetectable || mc.currentScreen !is HandledScreen<*>) && (passthroughSneak || keyBinding != mc.options.sneakKey)

    private fun isInCreativeSearchField(): Boolean {
        val currentScreen = mc.currentScreen

        return currentScreen is CreativeInventoryScreen && CreativeInventoryScreen.selectedTab == ItemGroups.SEARCH
    }

}
