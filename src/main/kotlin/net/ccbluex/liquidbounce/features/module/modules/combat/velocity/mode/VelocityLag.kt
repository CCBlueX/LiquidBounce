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

package net.ccbluex.liquidbounce.features.module.modules.combat.velocity.mode

import net.ccbluex.liquidbounce.event.events.BlinkPacketEvent
import net.ccbluex.liquidbounce.event.events.GameTickEvent
import net.ccbluex.liquidbounce.event.events.MovementInputEvent
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.events.TickPacketProcessEvent
import net.ccbluex.liquidbounce.event.events.TransferOrigin
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.sequenceHandler
import net.ccbluex.liquidbounce.event.waitTicks
import net.ccbluex.liquidbounce.features.blink.BlinkManager
import net.ccbluex.liquidbounce.features.blink.BlinkManager.Action
import net.minecraft.network.protocol.common.ClientboundKeepAlivePacket
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket

/**
 * Lag mode. It delays some ticks of knockback.
 * It is equals to "delay velocity".
 */
internal object VelocityLag : VelocityMode("Lag") {

    private val lagTime by intRange("LagTime", 5..5, 1..20, "ticks")
    private val jumpReset by boolean("JumpReset", false)

    private var shouldLag = false
    private var lagTicks = 0
    private var shouldJump = false

    @Suppress("unused")
    private val packetHandler = handler<PacketEvent> { event ->
        val packet = event.packet

        if (packet is ClientboundSetEntityMotionPacket && packet.id == player.id) {
            shouldLag = true
            lagTicks = lagTime.random()
        }
    }

    @Suppress("unused")
    private val queuePacketHandler = handler<BlinkPacketEvent> { event ->
        if (!shouldLag || event.origin != TransferOrigin.INCOMING || event.packet is ClientboundKeepAlivePacket) {
            return@handler
        }

        event.action = Action.QUEUE
    }

    @Suppress("unused")
    private val tickHandler = handler<GameTickEvent> {
        if (shouldLag) {
            lagTicks--
        }
    }

    @Suppress("unused")
    private val tickPacketProcessHandler = sequenceHandler<TickPacketProcessEvent> {
        if (shouldLag && lagTicks == 0) {
            shouldLag = false
            lagTicks = 0
            BlinkManager.flush(TransferOrigin.INCOMING)
            shouldJump = true
            waitTicks(2)
            shouldJump = false
        }
    }

    @Suppress("unused")
    private val movementInputHandler = handler<MovementInputEvent> { event ->
        // To be able to alter velocity when receiving knockback, player must be sprinting.
        if (jumpReset && shouldJump && player.onGround() && player.isSprinting) {
            event.jump = true
            shouldJump = false
        }
    }

    override fun disable() {
        shouldLag = false
        lagTicks = 0
    }

}
