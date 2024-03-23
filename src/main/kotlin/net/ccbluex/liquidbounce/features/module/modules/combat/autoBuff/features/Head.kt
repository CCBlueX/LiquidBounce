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

package net.ccbluex.liquidbounce.features.module.modules.combat.autoBuff.features

import net.ccbluex.liquidbounce.event.Sequence
import net.ccbluex.liquidbounce.features.module.modules.combat.autoBuff.Buff
import net.ccbluex.liquidbounce.utils.client.Chronometer
import net.minecraft.item.Items
import net.minecraft.util.Hand

object Head : Buff(
    "Head",
    isValidItem = {
        it.item == Items.PLAYER_HEAD
    }) {

    val cooldown by int("Cooldown", 100, 0..1000, "ms")
    val chronometer = Chronometer()

    override val passesRequirements: Boolean
        get() = passesHealthRequirements && chronometer.hasElapsed(cooldown.toLong())

    override suspend fun execute(sequence: Sequence<*>, slot: Int, hand: Hand) {
        interaction.interactItem(player, hand).takeIf { it.isAccepted }?.let { result ->
            if (result.shouldSwingHand()) {
                player.swingHand(hand)
            }

            chronometer.reset()
        }
    }


}
