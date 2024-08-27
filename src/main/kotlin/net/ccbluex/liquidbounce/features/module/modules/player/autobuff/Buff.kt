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

package net.ccbluex.liquidbounce.features.module.modules.player.autobuff

import net.ccbluex.liquidbounce.config.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.Sequence
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.HotbarItemSlot
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.OffHandSlot
import net.ccbluex.liquidbounce.utils.client.SilentHotbar
import net.ccbluex.liquidbounce.utils.combat.CombatManager
import net.ccbluex.liquidbounce.utils.inventory.InventoryManager
import net.ccbluex.liquidbounce.utils.item.findHotbarSlot
import net.minecraft.item.ItemStack

abstract class Buff(
    name: String,
    val isValidItem: (ItemStack, Boolean) -> Boolean,
) : ToggleableConfigurable(ModuleAutoBuff, name, true) {

    internal open val passesRequirements: Boolean
        get() = enabled && !player.isDead && !InventoryManager.isInventoryOpenServerSide
            && !interaction.currentGameMode.isCreative

    /**
     * Try to run feature if possible, otherwise return false
     */
    internal suspend fun runIfPossible(sequence: Sequence<*>): Boolean {
        if (!enabled || !passesRequirements) {
            return false
        }

        // Check main hand for item
        val mainHandStack = player.mainHandStack
        if (isValidItem(mainHandStack, true)) {
            CombatManager.pauseCombatForAtLeast(ModuleAutoBuff.combatPauseTime)
            execute(sequence, HotbarItemSlot(player.inventory.selectedSlot))
            return true
        }

        // Check off-hand for item
        val offHandStack = player.offHandStack
        if (isValidItem(offHandStack, true)) {
            CombatManager.pauseCombatForAtLeast(ModuleAutoBuff.combatPauseTime)
            execute(sequence, OffHandSlot)
            return true
        }

        // Check if we should auto swap
        ModuleAutoBuff.AutoSwap.takeIf { autoSwap -> autoSwap.enabled }?.run {
            // Check if the item is in the hotbar
            val slot = findHotbarSlot { stack -> isValidItem(stack, true) }

            if (slot != null) {
                CombatManager.pauseCombatForAtLeast(ModuleAutoBuff.combatPauseTime)

                // todo: do not hardcode ticksUntilReset
                SilentHotbar.selectSlotSilently(ModuleAutoBuff, slot, ticksUntilReset = 300)
                sequence.waitTicks(delayIn.random())
                execute(sequence, HotbarItemSlot(slot))
                sequence.waitTicks(delayOut.random())
                SilentHotbar.resetSlot(ModuleAutoBuff)
                return true
            }
        }
        return false
    }

    abstract suspend fun execute(sequence: Sequence<*>, slot: HotbarItemSlot)

    internal fun getStack(slot: Int): ItemStack =
        if (slot == -1) player.offHandStack else player.inventory.getStack(slot)

}

