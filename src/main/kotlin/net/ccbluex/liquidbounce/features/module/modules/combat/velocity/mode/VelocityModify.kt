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
package net.ccbluex.liquidbounce.features.module.modules.combat.velocity.mode

import net.ccbluex.liquidbounce.config.types.NamedChoice
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.modules.player.nofall.modes.NoFallBlink
import net.ccbluex.liquidbounce.utils.entity.any
import net.minecraft.network.packet.c2s.common.CommonPongC2SPacket
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket
import net.minecraft.network.packet.s2c.play.ExplosionS2CPacket
import kotlin.random.Random

/**
 * Basic velocity which should bypass most servers with common anti-cheats like NCP.
 */
internal object VelocityModify : VelocityMode("Modify") {

    private val horizontal by float("Horizontal", 0f, -1f..1f)
    private val vertical by float("Vertical", 0f, -1f..1f)
    private val motionHorizontal by float("MotionHorizontal", 0f, 0f..1f)
    private val motionVertical by float("MotionVertical", 0f, 0f..1f)
    private val chance by int("Chance", 100, 0..100, "%")
    private val filter by enumChoice("Filter", VelocityTriggerFilter.ALWAYS)
    private val onlyMove by boolean("OnlyMove", false)
    private val transactionBufferAmount by int("TransactionBuffer", 0, 0..3)

    private var transactionBuffer = 0

    @Suppress("unused")
    private val packetHandler = handler<PacketEvent> { event ->
        val packet = event.packet

        // Check if this is a regular velocity update
        if (packet is EntityVelocityUpdateS2CPacket && packet.entityId == player.id) {
            if (chance != 100 && Random.nextInt(100) > chance) return@handler
            if (!filter.allow()) return@handler
            if (onlyMove && !player.input.playerInput.any) return@handler

            // It should just block the packet
            if (horizontal == 0f && vertical == 0f) {
                event.cancelEvent()
                NoFallBlink.waitUntilGround = true
                return@handler
            }

            val currentVelocity = player.velocity

            // Modify packet according to the specified values
            if (horizontal != 0f) {
                packet.velocityX = (packet.velocityX * horizontal).toInt()
                packet.velocityZ = (packet.velocityZ * horizontal).toInt()
            } else {
                // set the horizontal velocity to the player velocity to prevent horizontal slowdown
                packet.velocityX = ((currentVelocity.x * motionHorizontal) * 8000).toInt()
                packet.velocityZ = ((currentVelocity.z * motionHorizontal) * 8000).toInt()
            }

            if (vertical != 0f) {
                packet.velocityY = (packet.velocityY * vertical).toInt()
            } else {
                // set the vertical velocity to the player velocity to prevent vertical slowdown
                packet.velocityY = ((currentVelocity.y * motionVertical) * 8000).toInt()
            }

            NoFallBlink.waitUntilGround = true
            transactionBuffer += transactionBufferAmount
        } else if (packet is ExplosionS2CPacket) { // Check if velocity is affected by explosion
            if (chance != 100 && Random.nextInt(100) > chance) return@handler
            if (!filter.allow()) return@handler
            if (onlyMove && !player.input.playerInput.any) return@handler

            // note: explosion packets are being used by hypixel to trick poorly made cheats.

            //  Modify packet according to the specified values
            packet.playerKnockback.ifPresent { knockback ->
                knockback.x *= horizontal
                knockback.y *= vertical
                knockback.z *= horizontal

                NoFallBlink.waitUntilGround = true
                transactionBuffer += transactionBufferAmount
            }
        }

        if (packet is CommonPongC2SPacket && transactionBuffer > 0) {
            event.cancelEvent()
            transactionBuffer--
        }
    }

    @Suppress("unused")
    private enum class VelocityTriggerFilter(
        override val choiceName: String,
        val allow: () -> Boolean
    ) : NamedChoice {
        ALWAYS("Always", { true }),
        ON_GROUND("OnGround", { player.isOnGround }),
        IN_AIR("InAir", { !player.isOnGround }),
    }

}
