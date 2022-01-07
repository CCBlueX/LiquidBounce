/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2016 - 2022 CCBlueX
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
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.modules.combat.ModuleAutoWeapon.PreferredWeapon.ANY
import net.ccbluex.liquidbounce.features.module.modules.combat.ModuleAutoWeapon.PreferredWeapon.AXE
import net.ccbluex.liquidbounce.features.module.modules.combat.ModuleAutoWeapon.PreferredWeapon.SWORD
import net.ccbluex.liquidbounce.features.module.modules.player.WeightedSwordItem.Companion.DAMAGE_ESTIMATOR
import net.ccbluex.liquidbounce.utils.item.attackDamage
import net.minecraft.item.AxeItem
import net.minecraft.item.SwordItem

/**
 * AutoWeapon module
 *
 * Automatically selects the best weapon from the hotbar
 */
object ModuleAutoWeapon : Module("AutoWeapon", Category.COMBAT) {

    enum class PreferredWeapon(override val choiceName: String) : NamedChoice {
        SWORD("Sword"), AXE("Axe"), ANY("Any");
    }

    // Preferred type of weapon e.g. a shovel (ANY)
    private val preferredWeapon by enumChoice(
        "Preferred Weapon", SWORD, PreferredWeapon.values()
    )

    // Automatic search for the best weapon
    private val search by boolean("Search", true)

    /* Slot with the best weapon
     * Useful if the weapons have special effects and their damage
     * cannot be determined
     *
     * NOTE: option [search] must be disabled
     */
    private val slot by int("Slot", 0, 0..8)

    val attackHandler = handler<AttackEvent> {
        val inventory = player.inventory

        // Find the best weapon in hotbar
        val index = if (search) {
            val (hotbarSlot, _) = (0..8)
                .map { Pair(it, inventory.getStack(it)) }
                .filter {
                    val item = it.second.item
                    when (preferredWeapon) {
                        SWORD -> item is SwordItem
                        AXE -> item is AxeItem
                        ANY -> true
                    }
                }
                .maxByOrNull {
                    it.second.item.attackDamage * (1.0f + DAMAGE_ESTIMATOR.estimateValue(
                        it.second
                    ))
                } ?: return@handler

            hotbarSlot
        } else {
            slot
        }

        // If in hand no need to swap
        if (inventory.selectedSlot == index) {
            return@handler
        }

        // Switch to best weapon
        inventory.selectedSlot = index
        inventory.updateItems()
    }
}
