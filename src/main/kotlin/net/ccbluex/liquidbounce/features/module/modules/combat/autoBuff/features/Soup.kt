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

import net.ccbluex.liquidbounce.config.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.Sequence
import net.ccbluex.liquidbounce.features.module.modules.combat.autoBuff.Buff
import net.ccbluex.liquidbounce.features.module.modules.combat.autoBuff.features.Soup.DropAfterUse.assumeEmptyBowl
import net.ccbluex.liquidbounce.features.module.modules.combat.autoBuff.features.Soup.DropAfterUse.wait
import net.minecraft.item.Items
import net.minecraft.util.Hand

object Soup : Buff(
    "Soup",
    isValidItem = {
        it.item == Items.MUSHROOM_STEW
    }) {

    object DropAfterUse : ToggleableConfigurable(this, "DropAfterUse", true) {
        val assumeEmptyBowl by boolean("AssumeEmptyBowl", true)
        val wait by intRange("Wait", 1..2, 1..20, "ticks")
    }

    override suspend fun execute(sequence: Sequence<*>, slot: Int, hand: Hand) {
        interaction.interactItem(player, hand).takeIf { it.isAccepted }?.let { result ->
            if (result.shouldSwingHand()) {
                player.swingHand(hand)
            }

            // If action was successful, drop the now-empty bowl
            if (DropAfterUse.enabled) {
                sequence.waitTicks(wait.random())

                if (assumeEmptyBowl || getStack(slot).item == Items.BOWL) {
                    player.dropSelectedItem(true)
                }
            }
        }
    }


}
