/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2026 CCBlueX
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
 */

package net.ccbluex.liquidbounce.features.module.modules.player.autoqueue.presets

import net.ccbluex.liquidbounce.config.types.group.Mode
import net.ccbluex.liquidbounce.config.types.group.ModeValueGroup
import net.ccbluex.liquidbounce.config.types.list.Tagged
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.event.waitTicks
import net.ccbluex.liquidbounce.features.module.modules.player.autoqueue.ModuleAutoQueue.presets
import net.ccbluex.liquidbounce.utils.inventory.Slots
import net.minecraft.world.item.Items

object AutoQueueHypixelSW : Mode("HypixelSW") {

    override val parent: ModeValueGroup<Mode>
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
    enum class SkyWarsGameMode(override val tag: String, val joinName: String) : Tagged {
        NORMAL("Normal", "solo_normal"),
        INSANE("Insane", "solo_insane");
    }

}
