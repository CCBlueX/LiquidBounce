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

package net.ccbluex.liquidbounce.features.module.modules.movement.fly.modes.verus

import net.ccbluex.liquidbounce.config.types.group.Mode
import net.ccbluex.liquidbounce.config.types.group.ModeValueGroup
import net.ccbluex.liquidbounce.event.events.BlinkPacketEvent
import net.ccbluex.liquidbounce.event.events.BlockShapeEvent
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.events.PlayerJumpEvent
import net.ccbluex.liquidbounce.event.events.TransferOrigin
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.features.blink.BlinkManager
import net.ccbluex.liquidbounce.features.module.modules.movement.fly.ModuleFly
import net.ccbluex.liquidbounce.utils.client.Timer
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.ccbluex.liquidbounce.utils.math.copy
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket
import net.minecraft.world.level.block.LiquidBlock
import net.minecraft.world.phys.shapes.Shapes

/**
 * @anticheat Verus
 * @anticheatVersion b3896
 * @testedOn anticheat-test
 * @note it can rarely flag once | needs 1.9x or above
 */
internal object FlyVerusB3869Flat : Mode("VerusB3896Flat") {

    private val timer by float("Timer", 5.0f, 1.0f..20.0f)

    override val parent: ModeValueGroup<*>
        get() = ModuleFly.modes

    @Suppress("unused")
    private val packetHandler = handler<PacketEvent> { event ->
        val packet = event.packet

        if (packet is ServerboundMovePlayerPacket) {
            packet.onGround = true
        }
    }

    @Suppress("unused")
    private val shapeHandler = handler<BlockShapeEvent> { event ->
        if (event.state.block !is LiquidBlock && event.pos.y < player.y) {
            event.shape = Shapes.block()
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
    private val fakeLagHandler = handler<BlinkPacketEvent> { event ->
        if (event.origin == TransferOrigin.OUTGOING) {
            event.action = BlinkManager.Action.QUEUE
        }
    }

    override fun disable() {
        val player = mc.player ?: return
        player.deltaMovement = player.deltaMovement.copy(x = 0.0, z = 0.0)

        network.send(
            ServerboundMovePlayerPacket.Pos(
                player.x, player.y - 0.5, player.z,
                false, player.horizontalCollision
            )
        )
    }
}
