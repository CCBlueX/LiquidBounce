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
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.entity.directionYaw
import net.ccbluex.liquidbounce.utils.entity.moving
import net.ccbluex.liquidbounce.utils.entity.sqrtSpeed
import net.ccbluex.liquidbounce.utils.entity.strafe
import net.minecraft.network.listener.ClientPlayPacketListener
import net.minecraft.network.packet.Packet
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
            Modify, Strafe, AAC442, Intave, IntaveHit, Test
        )
    }

    object Delayed : ToggleableConfigurable(this, "Delayed", false) {
        val ticks by intRange("Ticks", 3..6, 0..40)
    }

    init {
        tree(Delayed)
    }

    var count = 0
    var wasOnGround = false

    val packetHandler = sequenceHandler<PacketEvent>(priority = 1) {
        val packet = it.packet

        if (packet is EntityVelocityUpdateS2CPacket && packet.id == player.id || packet is ExplosionS2CPacket)
            wasOnGround = player.isOnGround
        if ((packet is EntityVelocityUpdateS2CPacket && packet.id == player.id || packet is ExplosionS2CPacket) && it.original && Delayed.enabled) {
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
     * Velocity for AAC4.4.2, pretty sure, it works on other versions
     */

    private object AAC442 : Choice("AAC4.4.2") {

        override val parent: ChoiceConfigurable
            get() = modes

        val aac442MotionReducer by float("AAC4.4.2MotionReducer", 0.62f, 0f..1f)

        val repeatable = repeatable {
            if (player.hurtTime > 0 && !player.isOnGround) {
                val reduce = aac442MotionReducer
                player.velocity.x *= reduce
                player.velocity.z *= reduce
            }
        }
    }

    private object Intave : Choice("Intave") {
        override val parent: ChoiceConfigurable
            get() = modes

        val vertical by float("Vertical", 0f, 0f..1f)
        val horizontal by float("Horizontal", 1f, 0f..1f)
        val delay by int("Delay", 20, 0..20)
        val wasOn by boolean("WasOnGround", true)

        val packetHandler = sequenceHandler<PacketEvent> { event ->
            val packet = event.packet

            // Check if this is a regular velocity update
            if (packet is EntityVelocityUpdateS2CPacket && packet.id == player.id && (player.isOnGround || !wasOn) && player.moving && (packet.velocityX != 0 || packet.velocityY != 0 ||packet.velocityZ != 0)) {
                val doHorizontal = packet.velocityX != 0 || packet.velocityZ != 0
                val doVertical = packet.velocityY != 0
                wait { delay }
                chat("reduced")
                if (doHorizontal) {
                    player.velocity.x *= horizontal
                    player.velocity.z *= horizontal
                }
                if (doVertical)
                    player.velocity.y *= vertical
            }
        }
    }

    private object Test : Choice("Test") {

        override val parent: ChoiceConfigurable
            get() = modes

        private val reduce6 by float("Reduce6", 1f, -1f..1.5f)
        private val reduce7 by float("Reduce7", 1f, -1f..1.5f)
        private val reduce8 by float("Reduce8", 1f, -1f..1.5f)
        private val reduce9 by float("Reduce9", 1f, -1f..1.5f)
        val wasOn by boolean("WasOnGround", true)

        val repeatable = repeatable {
            if (wasOnGround != wasOn)
                return@repeatable
            when (player.hurtTime) {
                9 -> {
                    if (reduce9 != 1f) {
                        chat("reduced")
                    }
                    player.velocity.x *= reduce9
                    player.velocity.z *= reduce9
                }

                8 -> {
                    if (reduce8 != 1f) {
                        chat("reduced")
                    }
                    player.velocity.x *= reduce8
                    player.velocity.z *= reduce8
                }

                7 -> {
                    if (reduce7 != 1f) {
                        chat("reduced")
                    }
                    player.velocity.x *= reduce7
                    player.velocity.z *= reduce7
                }

                6 -> {
                    if (reduce6 != 1f) {
                        chat("reduced")
                    }
                    player.velocity.x *= reduce6
                    player.velocity.z *= reduce6
                }
            }
        }
    }

    private object IntaveHit : Choice("IntaveHit") {
        override val parent: ChoiceConfigurable
            get() = modes

        val testY by float("testY", 0f, 0f..1f)
        val testXZ by float("testXZ", 1f, 0f..1f)
        val sprint by boolean("Sprint", false)

        val attackHandler = handler<AttackEvent> {
            if (player.handSwinging && player.hurtTime > 0) {
                player.velocity.y *= testY.toDouble()
                player.velocity.x *= testXZ.toDouble()
                player.velocity.z *= testXZ.toDouble()
                chat("reduced")
                if (sprint) {
                    player.isSprinting = false
                }
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
                packet.velocityX = (packet.velocityX * horizontal).toInt()
                packet.velocityY = (packet.velocityY * vertical).toInt()
                packet.velocityZ = (packet.velocityZ * horizontal).toInt()
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
