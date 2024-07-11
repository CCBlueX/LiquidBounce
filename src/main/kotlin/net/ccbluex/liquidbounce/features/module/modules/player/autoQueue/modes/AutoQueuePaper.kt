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

package net.ccbluex.liquidbounce.features.module.modules.player.autoQueue.modes

import net.ccbluex.liquidbounce.config.Choice
import net.ccbluex.liquidbounce.config.ChoiceConfigurable
import net.ccbluex.liquidbounce.event.repeatable
import net.ccbluex.liquidbounce.features.module.modules.player.autoQueue.ModuleAutoQueue
import net.ccbluex.liquidbounce.features.module.modules.player.autoQueue.ModuleAutoQueue.modes
import net.ccbluex.liquidbounce.utils.client.SilentHotbar
import net.ccbluex.liquidbounce.utils.item.findHotbarSlot
import net.minecraft.item.Items
import net.minecraft.util.Hand

/**
 * Can be used for different server that use paper to join a game
 */
object AutoQueuePaper : Choice("Paper") {

    override val parent: ChoiceConfigurable<Choice>
        get() = modes

    val repeatable = repeatable {
        val paper = (findHotbarSlot { it.item == Items.PAPER } ?: -1)
        if (paper == -1) {
            return@repeatable
        }

        SilentHotbar.selectSlotSilently(ModuleAutoQueue, paper)
        waitTicks(1)
        interaction.interactItem(player, Hand.MAIN_HAND)

        waitTicks(20)
    }

}
