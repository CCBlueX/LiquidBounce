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
package net.ccbluex.liquidbounce.features.module.modules.movement.inventorymove.features

import net.ccbluex.liquidbounce.config.types.group.ToggleableValueGroup
import net.ccbluex.liquidbounce.event.events.MovementInputEvent
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.modules.movement.inventorymove.ModuleInventoryMove
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention.FINAL_DECISION
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention.FIRST_PRIORITY
import net.ccbluex.liquidbounce.utils.movement.DirectionalInput
import net.ccbluex.liquidbounce.utils.network.isC2SContainerPacket
import net.ccbluex.liquidbounce.utils.network.sendPacketSilently
import net.minecraft.network.protocol.Packet
import net.minecraft.world.entity.player.Input

object InventoryMoveStopOnActionFeature : ToggleableValueGroup(ModuleInventoryMove, "StopOnAction", false) {

    private val delayedContainerPackets = mutableListOf<Packet<*>>()

    override fun onDisabled() {
        delayedContainerPackets.clear()
        super.onDisabled()
    }

    @Suppress("unused")
    private val movementInputHandler = handler<MovementInputEvent>(FINAL_DECISION) {
        if (delayedContainerPackets.isEmpty()) return@handler

        val packetsSnapshot = delayedContainerPackets.toTypedArray()
        delayedContainerPackets.clear()
        it.sneak = false
        it.jump = false
        it.directionalInput = DirectionalInput.NONE
        // `schedule` will force the Runnable to be run in next loop
        mc.schedule { packetsSnapshot.forEach(::sendPacketSilently) }
    }

    @Suppress("unused")
    private val packetHandler = handler<PacketEvent>(FIRST_PRIORITY) { event ->
        val packet = event.packet

        if (packet.isC2SContainerPacket() && player.input.keyPresses != Input.EMPTY) {
            event.cancelEvent()
            // Here only be called from render thread because [packet] is c2s
            delayedContainerPackets += packet
        }
    }

}
