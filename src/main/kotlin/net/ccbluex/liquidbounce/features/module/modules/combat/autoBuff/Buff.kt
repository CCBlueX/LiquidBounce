/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2024 CCBlueX
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
 *
 *
 */

package net.ccbluex.liquidbounce.features.module.modules.combat.autoBuff

import net.ccbluex.liquidbounce.config.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.Sequence
import net.ccbluex.liquidbounce.utils.client.SilentHotbar
import net.ccbluex.liquidbounce.utils.item.findHotbarSlot
import net.minecraft.item.ItemStack
import net.minecraft.util.Hand

abstract class Buff(
    name: String,
    val isValidItem: (ItemStack) -> Boolean,
) : ToggleableConfigurable(ModuleAutoBuff, name, true) {

    private val health by int("Health", 15, 1..40, "HP")

    open val passesRequirements: Boolean
        get() = passesHealthRequirements

    internal val passesHealthRequirements: Boolean
        get() {
            val fullHealth = player.health + player.absorptionAmount
            return fullHealth <= health
        }

    suspend fun runIfPossible(sequence: Sequence<*>): Boolean {
        // Check main hand for item
        val mainHandStack = player.mainHandStack
        if (isValidItem(mainHandStack)) {
            execute(sequence, player.inventory.selectedSlot, Hand.MAIN_HAND)
            return true
        }

        val offHandStack = player.offHandStack
        if (isValidItem(offHandStack)) {
            execute(sequence, -1, Hand.OFF_HAND)
            return true
        }

        // Check if the item is in the hotbar
        val slot = findHotbarSlot(isValidItem)
        if (slot != null) {
            // TODO: Select slot until feature is complete
            SilentHotbar.selectSlotSilently(ModuleAutoBuff, slot, ticksUntilReset = 120)

            execute(sequence, slot, Hand.MAIN_HAND)

            SilentHotbar.resetSlot(ModuleAutoBuff)
            return true
        }

        return false
    }

    abstract suspend fun execute(sequence: Sequence<*>, slot: Int, hand: Hand)

    fun getStack(slot: Int): ItemStack =
        if (slot == -1) player.offHandStack else player.inventory.getStack(slot)

}

