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

package net.ccbluex.liquidbounce.features.module.modules.player.autobuff

import net.ccbluex.liquidbounce.config.types.group.ToggleableValueGroup
import net.ccbluex.liquidbounce.event.events.KeybindIsPressedEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.tickUntil
import net.ccbluex.liquidbounce.event.waitTicks
import net.ccbluex.liquidbounce.features.module.modules.player.autobuff.ModuleAutoBuff.AutoSwap
import net.ccbluex.liquidbounce.utils.client.SilentHotbar
import net.ccbluex.liquidbounce.utils.combat.CombatManager
import net.ccbluex.liquidbounce.utils.inventory.HotbarItemSlot
import net.ccbluex.liquidbounce.utils.inventory.InventoryManager
import net.ccbluex.liquidbounce.utils.inventory.Slots
import net.ccbluex.liquidbounce.utils.inventory.findClosestSlot
import net.minecraft.world.item.ItemStack

abstract class Buff(
    name: String,
) : ToggleableValueGroup(ModuleAutoBuff, name, true) {

    internal open val passesRequirements: Boolean
        get() = enabled && !InventoryManager.isInventoryOpen

    private var forceUseKey = false

    @Suppress("unused")
    private val keyBindIsPressedHandler = handler<KeybindIsPressedEvent> { event ->
        if (event.keyBinding == mc.options.keyUse && forceUseKey) {
            event.isPressed = true
        }
    }

    /**
     * Holds the use key until the buff is no longer required or the item is no longer a valid target,
     * so the loop cannot hang forever when neither condition changes (e.g. after a potion is consumed).
     */
    protected suspend fun holdUse(slot: HotbarItemSlot) {
        forceUseKey = true
        try {
            tickUntil { !passesRequirements || !isValidItem(slot.itemStack, true) }
        } finally {
            forceUseKey = false
        }
    }

    override fun onDisabled() {
        forceUseKey = false
        super.onDisabled()
    }

    /**
     * Try to run feature if possible, otherwise return false
     */
    internal suspend fun runIfPossible(): Boolean {
        if (!enabled || !passesRequirements) {
            return false
        }

        // Check if the item is in the hotbar
        val slot = Slots.OffhandWithHotbar.findClosestSlot { isValidItem(it, true) } ?: return false

        CombatManager.pauseCombatForAtLeast(ModuleAutoBuff.combatPauseTime)

        if (slot.isSelected) {
            // Check main hand and offhand
            execute(slot)
            return true
        } else if (AutoSwap.enabled) {
            // Check if we should auto swap
            // todo: do not hardcode ticksUntilReset
            SilentHotbar.selectSlotSilently(ModuleAutoBuff, slot, 300)
            waitTicks(AutoSwap.delayIn.random())
            execute(slot)
            waitTicks(AutoSwap.delayOut.random())
            SilentHotbar.resetSlot(ModuleAutoBuff)
            return true
        } else {
            return false
        }
    }

    abstract fun isValidItem(stack: ItemStack, forUse: Boolean): Boolean

    abstract suspend fun execute(slot: HotbarItemSlot)

}

