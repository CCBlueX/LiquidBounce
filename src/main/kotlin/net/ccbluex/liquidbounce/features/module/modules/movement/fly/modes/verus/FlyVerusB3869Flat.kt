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

package net.ccbluex.liquidbounce.features.module.modules.movement.fly.modes.verus

import net.ccbluex.liquidbounce.config.types.nesting.Choice
import net.ccbluex.liquidbounce.config.types.nesting.ChoiceConfigurable
import net.ccbluex.liquidbounce.event.events.*
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.features.module.modules.movement.fly.ModuleFly
import net.ccbluex.liquidbounce.utils.client.PacketQueueManager
import net.ccbluex.liquidbounce.utils.client.Timer
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.minecraft.block.FluidBlock
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket
import net.minecraft.util.shape.VoxelShapes

/**
 * @anticheat Verus
 * @anticheatVersion b3896
 * @testedOn anticheat-test
 * @note it can rarely flag once | needs 1.9x or above
 */
internal object FlyVerusB3869Flat : Choice("VerusB3896Flat") {

    private val timer by float("Timer", 5.0f, 1.0f..20.0f)

    override val parent: ChoiceConfigurable<*>
        get() = ModuleFly.modes

    @Suppress("unused")
    private val packetHandler = handler<PacketEvent> { event ->
        val packet = event.packet

        if (packet is PlayerMoveC2SPacket) {
            packet.onGround = true
        }
    }

    @Suppress("unused")
    private val shapeHandler = handler<BlockShapeEvent> { event ->
        if (event.state.block !is FluidBlock && event.pos.y < player.y) {
            event.shape = VoxelShapes.fullCube()
        }
    }

    @Suppress("unused")
    private val jumpEvent = handler<PlayerJumpEvent> { event ->
        event.cancelEvent()
    }

    @Suppress("unused")
    private val tickHandler = tickHandler {
        Timer.requestTimerSpeed(timer, Priority.IMPORTANT_FOR_USAGE_1, ModuleFly)
    }

    @Suppress("unused")
    private val fakeLagHandler = handler<QueuePacketEvent> { event ->
        if (event.origin == TransferOrigin.OUTGOING) {
            event.action = PacketQueueManager.Action.QUEUE
        }
    }

    override fun disable() {
        player.velocity.x = 0.0
        player.velocity.z = 0.0

        network.sendPacket(
            PlayerMoveC2SPacket.PositionAndOnGround(
                player.x, player.y - 0.5, player.z,
                false, player.horizontalCollision
            )
        )
    }
}
