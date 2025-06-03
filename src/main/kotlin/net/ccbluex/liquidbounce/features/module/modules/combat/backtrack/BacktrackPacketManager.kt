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
 */
package net.ccbluex.liquidbounce.features.module.modules.combat.backtrack

import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.events.TickPacketProcessEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.modules.combat.backtrack.ModuleBacktrack.arePacketQueuesEmpty
import net.ccbluex.liquidbounce.features.module.modules.combat.backtrack.ModuleBacktrack.clear
import net.ccbluex.liquidbounce.features.module.modules.combat.backtrack.ModuleBacktrack.currentDelay
import net.ccbluex.liquidbounce.features.module.modules.combat.backtrack.ModuleBacktrack.delay
import net.ccbluex.liquidbounce.features.module.modules.combat.backtrack.ModuleBacktrack.packetProcessQueue
import net.ccbluex.liquidbounce.features.module.modules.combat.backtrack.ModuleBacktrack.processPackets
import net.ccbluex.liquidbounce.features.module.modules.combat.backtrack.ModuleBacktrack.shouldCancelPackets
import net.ccbluex.liquidbounce.utils.client.handlePacket
import net.ccbluex.liquidbounce.utils.client.inGame

/**
 * Backtrack's own packet manager. It is meant to be replaced by [PacketQueueManager]
 * but once the packet process logic is fixed.
 */
object BacktrackPacketManager : EventListener {

    /**
     * When we process packets, we want the delayed ones to be processed first before
     * the game proceeds with its own packet processing.
     *
     * @see net.minecraft.client.MinecraftClient.render
     *
     * profiler.push("scheduledExecutables");
     * this.runTasks();
     * profiler.pop();
     * profiler.push("tick");
     *
     */
    @Suppress("unused")
    private val handleTickPacketProcess = handler<TickPacketProcessEvent> {
        if (!inGame) {
            clear(clearOnly = true)
            return@handler
        }

        if (shouldCancelPackets()) {
            processPackets()
        } else {
            clear()
        }

        packetProcessQueue.removeIf {
            handlePacket(it)

            return@removeIf true
        }

        if (arePacketQueuesEmpty) {
            currentDelay = delay.random()
        }
    }

}
