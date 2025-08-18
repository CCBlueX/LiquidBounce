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
 *
 *
 */

package net.ccbluex.liquidbounce.features.module.modules.player.autobuff.features

import net.ccbluex.liquidbounce.event.Sequence
import net.ccbluex.liquidbounce.event.events.KeybindIsPressedEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.modules.player.autobuff.HealthBasedBuff
import net.ccbluex.liquidbounce.utils.inventory.HotbarItemSlot
import net.minecraft.item.ItemStack
import net.minecraft.item.Items

object Gapple : HealthBasedBuff("Gapple") {

    private var forceUseKey = false

    override fun isValidItem(stack: ItemStack, forUse: Boolean): Boolean {
        return stack.item == Items.GOLDEN_APPLE
    }

    override suspend fun execute(sequence: Sequence, slot: HotbarItemSlot) {
        forceUseKey = true
        sequence.waitUntil { !passesRequirements }
        forceUseKey = false
    }

    override fun onDisabled() {
        forceUseKey = false
        super.onDisabled()
    }

    @Suppress("unused")
    private val keyBindIsPressedHandler = handler<KeybindIsPressedEvent> { event ->
        if (event.keyBinding == mc.options.useKey && forceUseKey) {
            event.isPressed = true
        }
    }

}
