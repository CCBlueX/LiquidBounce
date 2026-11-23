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

package net.ccbluex.liquidbounce.features.module.modules.player.autobuff.features

import net.ccbluex.liquidbounce.config.types.group.ToggleableValueGroup
import net.ccbluex.liquidbounce.features.module.modules.player.autobuff.ModuleAutoBuff
import net.ccbluex.liquidbounce.utils.inventory.InventoryAction
import net.ccbluex.liquidbounce.utils.inventory.PlayerInventoryConstraints
import net.ccbluex.liquidbounce.utils.inventory.Slots
import net.ccbluex.liquidbounce.utils.inventory.inventoryPlan
import net.ccbluex.liquidbounce.utils.item.getPotionEffects
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.minecraft.world.effect.MobEffects
import net.minecraft.world.item.ItemStack

object Refill : ToggleableValueGroup(ModuleAutoBuff, "Refill", true) {

    private val inventoryConstraints = tree(PlayerInventoryConstraints())
    private val healingOnly by boolean("HealingPotionsOnly", false)

    @Suppress("unused")
    private val refill = inventoryPlan(inventoryConstraints) {
        val validFeatures = ModuleAutoBuff.activeFeatures
        val validItems = Slots.Inventory.filter {
            val itemStack = it.itemStack
            validFeatures.any { f -> f.isValidItem(itemStack, false) } && (!healingOnly || itemStack.isHealing)
        }

        for (slot in validItems) {
            // An earlier step may have filled the hotbar
            if (Slots.Hotbar.any { it.itemStack.isEmpty } || canMergeIntoHotbar(slot.itemStack)) {
                perform(InventoryAction.Click.performQuickMove(slot = slot), priority = Priority.IMPORTANT_FOR_USAGE_1)
            }
        }
    }

    // Shift-clicking from the main inventory only moves into hotbar 0..8, never into offhand.
    private fun canMergeIntoHotbar(stack: ItemStack): Boolean {
        if (!stack.isStackable) {
            return false
        }
        return Slots.Hotbar.any { slot ->
            val target = slot.itemStack
            !target.isEmpty && target.count < target.maxStackSize &&
                ItemStack.isSameItemSameComponents(target, stack)
        }
    }

    private val ItemStack.isHealing
        get() = getPotionEffects().any { it.effect == MobEffects.INSTANT_HEALTH }
}
