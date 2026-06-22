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

import net.ccbluex.liquidbounce.config.types.list.Tagged
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.modules.player.nofall.modes.NoFallBlink
import net.ccbluex.liquidbounce.utils.entity.anyHorizontal
import net.minecraft.network.protocol.common.ServerboundPongPacket
import net.minecraft.network.protocol.game.ClientboundExplodePacket
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket
import java.util.function.BooleanSupplier
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
    private val considerExplosion by boolean("ConsiderExplosion", true)

    private var transactionBuffer = 0

    override fun disable() {
        super.disable()
        transactionBuffer = 0
    }

    @Suppress("unused")
    private val packetHandler = handler<PacketEvent> { event ->
        when (val packet = event.packet) {
            // Check if this is a regular velocity update
            is ClientboundSetEntityMotionPacket if packet.id == player.id -> {
                if (chance != 100 && Random.nextInt(100) > chance) return@handler
                if (!filter.condition.asBoolean) return@handler
                if (onlyMove && !player.input.keyPresses.anyHorizontal) return@handler

                // It should just block the packet
                if (horizontal == 0f && vertical == 0f) {
                    event.cancelEvent()
                    NoFallBlink.waitUntilGround = true
                    return@handler
                }

                val currentVelocity = player.deltaMovement

                // Modify packet according to the specified values
                if (horizontal != 0f) {
                    packet.movement.x = packet.movement.x * horizontal
                    packet.movement.z = packet.movement.z * horizontal
                } else {
                    // set the horizontal velocity to the player velocity to prevent horizontal slowdown
                    packet.movement.x = currentVelocity.x * motionHorizontal
                    packet.movement.z = currentVelocity.z * motionHorizontal
                }

                if (vertical != 0f) {
                    packet.movement.y = packet.movement.y * vertical
                } else {
                    // set the vertical velocity to the player velocity to prevent vertical slowdown
                    packet.movement.y = currentVelocity.y * motionVertical
                }

                NoFallBlink.waitUntilGround = true
                transactionBuffer += transactionBufferAmount
            }

            // Check if velocity is affected by explosion
            is ClientboundExplodePacket if packet.playerKnockback.isPresent && considerExplosion -> {
                if (chance != 100 && Random.nextInt(100) > chance) return@handler
                if (!filter.condition.asBoolean) return@handler
                if (onlyMove && !player.input.keyPresses.anyHorizontal) return@handler

                // note: explosion packets are being used by hypixel to trick poorly made cheats.

                //  Modify packet according to the specified values
                val knockback = packet.playerKnockback.get()
                knockback.x *= horizontal
                knockback.y *= vertical
                knockback.z *= horizontal

                NoFallBlink.waitUntilGround = true
                transactionBuffer += transactionBufferAmount
            }

            is ServerboundPongPacket if transactionBuffer > 0 -> {
                event.cancelEvent()
                transactionBuffer--
            }
        }
    }

    @Suppress("unused")
    private enum class VelocityTriggerFilter(
        override val tag: String,
        val condition: BooleanSupplier,
    ) : Tagged {
        ALWAYS("Always", { true }),
        ON_GROUND("OnGround", { player.onGround() }),
        IN_AIR("InAir", { !player.onGround() }),
    }

}
