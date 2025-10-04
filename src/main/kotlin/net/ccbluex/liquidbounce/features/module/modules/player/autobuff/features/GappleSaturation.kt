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

import net.ccbluex.liquidbounce.event.events.KeybindIsPressedEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.tickUntil
import net.ccbluex.liquidbounce.features.module.modules.player.autobuff.Buff
import net.ccbluex.liquidbounce.utils.inventory.HotbarItemSlot
import net.ccbluex.liquidbounce.utils.inventory.interactItem
import net.minecraft.item.ItemStack
import net.minecraft.item.Items

internal object GappleSaturation : Buff("GappleBySaturation") {

    private var forceUseKey = false

    override val isLongConsumable = true

    override val passesRequirements: Boolean
        get() {
            val passesSaturationRequirements = 20 - player.hungerManager.saturationLevel +
                (player.maxHealth - player.health) * 1.5f >= 9.6f && player.hungerManager.saturationLevel <= 14.9f
            return super.passesRequirements && passesSaturationRequirements
        }

    override fun isValidItem(stack: ItemStack, forUse: Boolean): Boolean {
        return stack.isOf(Items.GOLDEN_APPLE)
    }

    override suspend fun execute(slot: HotbarItemSlot) {
        forceUseKey = true
        interactItem(slot.useHand)
        tickUntil { !passesRequirements || !player.isUsingItem }
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
