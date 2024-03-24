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

package net.ccbluex.liquidbounce.features.module.modules.player.autoBuff.features

import net.ccbluex.liquidbounce.event.Sequence
import net.ccbluex.liquidbounce.features.module.modules.player.autoBuff.HealthBasedBuff
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.HotbarItemSlot
import net.minecraft.item.Items

object Gapple : HealthBasedBuff("Gapple", isValidItem = { stack, _ -> stack.item == Items.GOLDEN_APPLE }) {

    override suspend fun execute(sequence: Sequence<*>, slot: HotbarItemSlot) {
        mc.options.useKey.isPressed = true

        sequence.waitUntil {
            val stopItemUse = !passesRequirements

            if (stopItemUse) {
                releaseUseKey()
            }
            return@waitUntil stopItemUse
        }
    }

    private fun releaseUseKey() {
        mc.options.useKey.isPressed = false
    }

    override fun disable() {
        releaseUseKey()
        super.disable()
    }

}
