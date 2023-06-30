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

import net.ccbluex.liquidbounce.config.NamedChoice
import net.ccbluex.liquidbounce.event.AttackEvent
import net.ccbluex.liquidbounce.event.PlayerTickEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.modules.combat.ModuleAutoWeapon.PreferredWeapon.*
import net.ccbluex.liquidbounce.features.module.modules.player.WeightedSwordItem.Companion.DAMAGE_ESTIMATOR
import net.ccbluex.liquidbounce.utils.item.attackDamage
import net.ccbluex.liquidbounce.utils.item.getEnchantment
import net.ccbluex.liquidbounce.utils.item.isNothing
import net.minecraft.enchantment.Enchantments.FIRE_ASPECT
import net.minecraft.enchantment.Enchantments.KNOCKBACK
import net.minecraft.item.AxeItem
import net.minecraft.item.PickaxeItem
import net.minecraft.item.ShovelItem
import net.minecraft.item.SwordItem
import net.minecraft.item.TridentItem

/**
 * AutoWeapon module
 *
 * Automatically selects the best weapon in your hotbar
 */
object ModuleAutoWeapon : Module("AutoWeapon", Category.COMBAT) {

    enum class PreferredWeapon(override val choiceName: String) : NamedChoice {
        ANY("Any"),
        AXE("Axe"),
        PICKAXE("Pickaxe"),
        SHOVEL("Shovel"),
        SWORD("Sword"),
        TRIDENT("Trident");
    }

    // Preferred type of weapon e.g. a shovel (ANY)
    private val preferredWeapon by enumChoice(
        "PreferredWeapon", ANY, PreferredWeapon.values()
    )

    // Ignore items with low durability
    private val ignoreDurability by boolean("IgnoreDurability", false)

    // Ignore items with knockback enchantment
    private val ignoreKnockback by boolean("IgnoreKnockback", false)

    // Automatic search for the best weapon
    private val search by boolean("Search", true)

    /* Slot with the best weapon
     * Useful if the weapons have special effects and their damage
     * cannot be determined
     *
     * NOTE: option [search] must be disabled
     */
    private val slot by int("Slot", 0, 0..8)

    // Swap the slot of the previous item
    private val swapPrevious by boolean("SwapPrevious", false)

    // Time (in ticks) to swap to the previous slot (0.5s - 2.5s)
    private val time by int("Time", 25, 10..50)

    // Time left to swap
    private var leftTime = 0

    // Previous slot
    private var prev = 0

    val attackHandler = handler<AttackEvent> {
        val inventory = player.inventory
        if (swapPrevious) {
            leftTime = time
        }
        val index = if (search) {
            val (hotbarSlot, _) = (0..8)
                .map { Pair(it, inventory.getStack(it)) }
                .filter {
                    val stack = it.second
                    if (stack.isNothing()
                        || (!player.isCreative && stack.damage >= (stack.maxDamage - 2) && ignoreDurability)
                        || (stack.getEnchantment(KNOCKBACK) != 0 && ignoreKnockback)
                    ) {
                        return@filter false
                    }
                    val item = stack.item
                    when (preferredWeapon) {
                        ANY -> true
                        AXE -> item is AxeItem
                        PICKAXE -> item is PickaxeItem
                        SHOVEL -> item is ShovelItem
                        SWORD -> item is SwordItem
                        TRIDENT -> item is TridentItem
                    }
                }
                .maxByOrNull {
                    val stack = it.second
                    stack.item.attackDamage * (1.0f + DAMAGE_ESTIMATOR.estimateValue(stack)
                        + stack.getEnchantment(FIRE_ASPECT)) * 4.0f * 0.625f * 0.9f
                } ?: return@handler
            hotbarSlot
        } else {
            slot
        }
        if (inventory.selectedSlot == index) {
            return@handler
        }
        prev = inventory.selectedSlot
        inventory.selectedSlot = index
        inventory.updateItems()
    }

    val playerTickHandler = handler<PlayerTickEvent> {
        if (prev == -1) {
            return@handler
        }
        if (leftTime > 0) {
            if (leftTime == 1) {
                player.inventory.selectedSlot = prev
                prev = -1
            }
            leftTime--
        }
    }
}
