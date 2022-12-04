/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2016 - 2022 CCBlueX
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
package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.config.Choice
import net.ccbluex.liquidbounce.config.ChoiceConfigurable
import net.ccbluex.liquidbounce.config.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.entity.directionYaw
import net.ccbluex.liquidbounce.utils.entity.sqrtSpeed
import net.ccbluex.liquidbounce.utils.entity.strafe
import net.minecraft.network.Packet
import net.minecraft.network.listener.ClientPlayPacketListener
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket
import net.minecraft.network.packet.s2c.play.ExplosionS2CPacket

/**
 * Velocity module
 *
 * Modifies the amount of velocity you take.
 */

object ModuleVelocity : Module("Velocity", Category.COMBAT) {

    val modes = choices("Mode", Modify) {
        arrayOf(
            Modify, Push, Strafe
        )
    }

    object Delayed : ToggleableConfigurable(this, "Delayed", false) {
        val ticks by intRange("Ticks", 3..6, 0..40)
    }

    init {
        tree(Delayed)
    }

    val packetHandler = sequenceHandler<PacketEvent>(priority = 1) {
        val packet = it.packet

        if ((packet is EntityVelocityUpdateS2CPacket && packet.id == player.id || packet is ExplosionS2CPacket) && it.normal && Delayed.enabled) {
            it.cancelEvent()

            Delayed.ticks.random().let { ticks ->
                if (ticks > 0) {
                    val timeToWait = System.currentTimeMillis() + (ticks * 50L)

                    waitUntil { System.currentTimeMillis() >= timeToWait }
                }
            }

            val packetEvent = PacketEvent(TransferOrigin.RECEIVE, packet, false)
            EventManager.callEvent(packetEvent)

            if (!packetEvent.isCancelled) {
                (packet as Packet<ClientPlayPacketListener>).apply(network)
            }
        }
    }

    /**
     *
     * Basic velocity which should bypass the most server with regular anti-cheats like NCP.
     */
    private object Modify : Choice("Modify") {

        override val parent: ChoiceConfigurable
            get() = modes

        val horizontal by float("Horizontal", 0f, 0f..1f)
        val vertical by float("Vertical", 0f, 0f..1f)

        val packetHandler = handler<PacketEvent> { event ->
            val packet = event.packet

            // Check if this is a regular velocity update
            if (packet is EntityVelocityUpdateS2CPacket && packet.id == player.id) {
                // It should just block the packet
                if (horizontal == 0f && vertical == 0f) {
                    event.cancelEvent()
                    return@handler
                }

                // Modify packet according to the specified values
                packet.velocityX *= horizontal.toInt()
                packet.velocityY *= vertical.toInt()
                packet.velocityZ *= horizontal.toInt()
            } else if (packet is ExplosionS2CPacket) { // Check if velocity is affected by explosion
                // note: explosion packets are being used by hypixel to trick poorly made cheats.

                // It should just block the packet
                if (horizontal == 0f && vertical == 0f) {
                    event.cancelEvent()
                    return@handler
                }

                //  Modify packet according to the specified values
                packet.playerVelocityX *= horizontal
                packet.playerVelocityY *= vertical
                packet.playerVelocityZ *= horizontal
            }
        }

    }

    /**
     * Push velocity
     *
     * todo: finish it what ever
     */
    private object Push : Choice("Push") {

        override val parent: ChoiceConfigurable
            get() = modes

        val packetHandler = sequenceHandler<PacketEvent> { event ->
            val packet = event.packet

            // Check if this is a regular velocity update
            if (packet is EntityVelocityUpdateS2CPacket && packet.id == player.id) {

            } else if (packet is ExplosionS2CPacket) { // Check if velocity is affected by explosion

            }
        }

    }

    /**
     * Strafe velocity
     */
    private object Strafe : Choice("Strafe") {

        override val parent: ChoiceConfigurable
            get() = modes

        val delay by int("Delay", 2, 0..10)
        val strength by float("Strength", 1f, 0.1f..2f)
        val untilGround by boolean("UntilGround", false)

        var applyStrafe = false

        val packetHandler = sequenceHandler<PacketEvent> { event ->
            val packet = event.packet

            // Check if this is a regular velocity update
            if ((packet is EntityVelocityUpdateS2CPacket && packet.id == player.id) || packet is ExplosionS2CPacket) {
                // A few anti-cheats can be easily tricked by applying the velocity a few ticks after being damaged
                wait(delay)

                // Apply strafe
                player.strafe(speed = player.sqrtSpeed * strength)

                if (untilGround) {
                    applyStrafe = true
                }
            }
        }

        val moveHandler = handler<PlayerMoveEvent> { event ->
            if (player.isOnGround) {
                applyStrafe = false
            } else if (applyStrafe) {
                event.movement.strafe(player.directionYaw, player.sqrtSpeed * strength)
            }
        }

    }

}