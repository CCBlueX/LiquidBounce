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
import net.ccbluex.liquidbounce.features.module.modules.player.autobuff.StatusEffectBasedBuff
import net.ccbluex.liquidbounce.utils.inventory.HotbarItemSlot
import net.minecraft.item.ItemStack
import net.minecraft.item.PotionItem
import net.minecraft.item.SplashPotionItem

internal object Drink : StatusEffectBasedBuff("Drink") {

    private var forceUseKey = false

    override suspend fun Sequence.execute(slot: HotbarItemSlot) {
        forceUseKey = true
        waitUntil { !passesRequirements }
        forceUseKey = false
    }

    @Suppress("unused")
    private val keyBindIsPressedHandler = handler<KeybindIsPressedEvent> { event ->
        if (event.keyBinding == mc.options.useKey && forceUseKey) {
            event.isPressed = true
        }
    }

    override fun onDisabled() {
        forceUseKey = false
        super.onDisabled()
    }

    override fun isValidPotion(stack: ItemStack) =
        stack.item is PotionItem && stack.item !is SplashPotionItem

}
