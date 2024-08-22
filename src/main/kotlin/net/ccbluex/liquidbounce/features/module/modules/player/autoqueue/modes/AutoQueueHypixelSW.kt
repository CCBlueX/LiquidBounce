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

package net.ccbluex.liquidbounce.features.module.modules.player.autoqueue.modes

import net.ccbluex.liquidbounce.config.Choice
import net.ccbluex.liquidbounce.config.ChoiceConfigurable
import net.ccbluex.liquidbounce.config.NamedChoice
import net.ccbluex.liquidbounce.event.repeatable
import net.ccbluex.liquidbounce.features.module.modules.player.autoqueue.ModuleAutoQueue.modes
import net.ccbluex.liquidbounce.utils.item.findHotbarSlot
import net.minecraft.item.Items

object AutoQueueHypixelSW : Choice("HypixelSW") {

    override val parent: ChoiceConfigurable<Choice>
        get() = modes

    private val gameMode by enumChoice("GameMode", SkyWarsGameMode.NORMAL, SkyWarsGameMode.values())

    private val hasPaper
        get() = (findHotbarSlot { it.item == Items.PAPER } ?: -1) != -1

    val repeatable = repeatable {
        // Check if we have paper in our hotbar
        if (!hasPaper) {
            return@repeatable
        }

        // Send join command
        network.sendCommand("play ${gameMode.joinName}")
        waitTicks(20)
    }


    enum class SkyWarsGameMode(override val choiceName: String, val joinName: String) : NamedChoice {
        NORMAL("Normal", "solo_normal"),
        INSANE("Insane", "solo_insane");
    }

}
