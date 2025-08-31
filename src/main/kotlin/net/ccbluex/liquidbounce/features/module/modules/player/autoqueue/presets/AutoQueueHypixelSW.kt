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

package net.ccbluex.liquidbounce.features.module.modules.player.autoqueue.presets

import net.ccbluex.liquidbounce.config.types.nesting.Choice
import net.ccbluex.liquidbounce.config.types.nesting.ChoiceConfigurable
import net.ccbluex.liquidbounce.config.types.NamedChoice
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.features.module.modules.player.autoqueue.ModuleAutoQueue.presets
import net.ccbluex.liquidbounce.utils.inventory.Slots
import net.minecraft.item.Items

object AutoQueueHypixelSW : Choice("HypixelSW") {

    override val parent: ChoiceConfigurable<Choice>
        get() = presets

    private val gameMode by enumChoice("GameMode", SkyWarsGameMode.NORMAL)

    private val hasPaper
        get() = Slots.Hotbar.findSlot(Items.PAPER) != null

    val repeatable = tickHandler {
        // Check if we have paper in our hotbar
        if (!hasPaper) {
            return@tickHandler
        }

        // Send join command
        network.sendCommand("play ${gameMode.joinName}")
        waitTicks(20)
    }

    @Suppress("unused")
    enum class SkyWarsGameMode(override val choiceName: String, val joinName: String) : NamedChoice {
        NORMAL("Normal", "solo_normal"),
        INSANE("Insane", "solo_insane");
    }

}
