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
import net.ccbluex.liquidbounce.features.module.modules.player.autobuff.HealthBasedBuff
import net.ccbluex.liquidbounce.utils.inventory.HotbarItemSlot
import net.ccbluex.liquidbounce.utils.client.Chronometer
import net.ccbluex.liquidbounce.utils.inventory.useHotbarSlotOrOffhand
import net.minecraft.item.ItemStack
import net.minecraft.item.Items

object Head : HealthBasedBuff("Head") {

    val cooldown by int("Cooldown", 0, 0..120, "s")
    val chronometer = Chronometer()

    override val passesRequirements: Boolean
        get() = passesHealthRequirements && chronometer.hasElapsed(cooldown * 1000L)

    override fun isValidItem(stack: ItemStack, forUse: Boolean): Boolean {
        return stack.item == Items.PLAYER_HEAD
    }

    override suspend fun execute(sequence: Sequence, slot: HotbarItemSlot) {
        useHotbarSlotOrOffhand(slot)
        chronometer.reset()
    }


}
