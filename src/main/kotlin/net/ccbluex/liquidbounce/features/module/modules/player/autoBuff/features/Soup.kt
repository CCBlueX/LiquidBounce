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

import net.ccbluex.liquidbounce.config.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.Sequence
import net.ccbluex.liquidbounce.features.module.modules.player.autoBuff.HealthBasedBuff
import net.ccbluex.liquidbounce.features.module.modules.player.autoBuff.features.Soup.DropAfterUse.assumeEmptyBowl
import net.ccbluex.liquidbounce.features.module.modules.player.autoBuff.features.Soup.DropAfterUse.wait
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.HotbarItemSlot
import net.ccbluex.liquidbounce.utils.inventory.useHotbarSlotOrOffhand
import net.minecraft.item.Items
import net.minecraft.util.Hand

object Soup : HealthBasedBuff("Soup", isValidItem = { stack, _ -> stack.item == Items.MUSHROOM_STEW }) {

    object DropAfterUse : ToggleableConfigurable(this, "DropAfterUse", true) {
        val assumeEmptyBowl by boolean("AssumeEmptyBowl", true)
        val wait by intRange("Wait", 1..2, 1..20, "ticks")
    }

    init {
        tree(DropAfterUse)
    }

    override suspend fun execute(sequence: Sequence<*>, slot: HotbarItemSlot) {
        // Use item (be aware, it will always return false in this case)
        useHotbarSlotOrOffhand(slot)

        if (DropAfterUse.enabled) {
            sequence.waitTicks(wait.random())

            if (assumeEmptyBowl || slot.itemStack.item == Items.BOWL) {
                if (player.dropSelectedItem(true)) {
                    player.swingHand(Hand.MAIN_HAND)
                }
            }
        }
    }


}
