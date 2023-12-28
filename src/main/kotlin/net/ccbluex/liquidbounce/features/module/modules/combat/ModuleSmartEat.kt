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
package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.event.repeatable
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.client.SilentHotbar
import net.ccbluex.liquidbounce.utils.item.Hotbar
import net.minecraft.entity.effect.StatusEffects
import net.minecraft.item.ItemStack
import net.minecraft.util.ActionResult
import net.minecraft.util.UseAction
import kotlin.math.max

/**
 * AutoClicker module
 *
 * Clicks automatically when holding down a mouse button.
 */

object ModuleSmartEat : Module("SmartEat", Category.PLAYER) {


    private val swapBackDelay by int("SwapBackDelay", 5, 1..20)

    val food: Pair<Int, ItemStack>?
        get() = Hotbar.findBestItem(0) { _, itemStack ->
            val foodComp =
                itemStack.item.foodComponent ?: return@findBestItem -1

            if(foodComp.statusEffects.any {it.first == StatusEffects.HUNGER})
                return@findBestItem 0
            foodComp.hunger
        }

    fun onInteraction(actionResult: ActionResult) {

        if(!enabled)
            return
        if(actionResult != ActionResult.PASS)
            return

        val currentFood = food ?: return

        SilentHotbar.selectSlotSilently(this@ModuleSmartEat, currentFood.first, max(swapBackDelay, 5))
    }

    val tickHandler = repeatable {
        if(player.activeItem.useAction != UseAction.EAT)
            return@repeatable
        if(!SilentHotbar.isSlotModified(this@ModuleSmartEat))
            return@repeatable
        // if we are already eating, we want to keep the silent slot
        SilentHotbar.selectSlotSilently(this@ModuleSmartEat, SilentHotbar.serversideSlot, swapBackDelay)


    }
}
