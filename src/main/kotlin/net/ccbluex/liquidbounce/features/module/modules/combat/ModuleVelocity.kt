/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2024 CCBlueX
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
import net.ccbluex.liquidbounce.event.EventManager
import net.ccbluex.liquidbounce.event.events.*
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.repeatable
import net.ccbluex.liquidbounce.event.sequenceHandler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.entity.directionYaw
import net.ccbluex.liquidbounce.utils.entity.sqrtSpeed
import net.ccbluex.liquidbounce.utils.entity.strafe
import net.minecraft.network.listener.ClientPlayPacketListener
import net.minecraft.network.packet.Packet
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.Full
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket
import net.minecraft.network.packet.s2c.play.ExplosionS2CPacket

/**
 * Velocity module
 *
 * Modifies the amount of velocity you take.
 */

object ModuleVelocity : Module("Velocity", Category.COMBAT) {

    val modes = choices("Mode", { Modify }) {
        arrayOf(
            Modify, Strafe, AAC442, ExemptGrim117, Dexland, JumpReset
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

    private object Dexland : Choice("Dexland") {

        override val parent: ChoiceConfigurable
            get() = modes

        val hReduce by float("HReduce", 0.3f, 0f..1f)
        val times by int("AttacksToWork", 4, 1..10)

        var lastAttackTime = 0L
        var count = 0

        val attackHandler = handler<AttackEvent> {
            if (player.hurtTime > 0 && ++count % times == 0 && System.currentTimeMillis() - lastAttackTime <= 8000) {
                player.velocity.x *= hReduce
                player.velocity.z *= hReduce
            }
            lastAttackTime = System.currentTimeMillis()
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
                waitTicks(delay)

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

    /**
     * Jump Reset mode. A technique most players use to minimize the amount of knockback they get.
     */
    private object JumpReset : Choice("JumpReset") {
        override val parent: ChoiceConfigurable
            get() = modes

        object JumpByReceivedHits : ToggleableConfigurable(ModuleVelocity, "JumpByReceivedHits", false) {
            val hitsUntilJump by int("HitsUntilJump", 2, 0..10)
        }

        object JumpByDelay : ToggleableConfigurable(ModuleVelocity, "JumpByDelay", true) {
            val ticksUntilJump by int("TicksUntilJump", 2, 0..20)
        }

        init {
            tree(JumpByReceivedHits)
            tree(JumpByDelay)
        }

        var limitUntilJump = 0

        val tickJumpHandler = handler<TickJumpEvent> {
            // To be able to alter velocity when receiving knockback, player must be sprinting.
            if (player.hurtTime != 9 || !player.isOnGround || !player.isSprinting || !isCooldownOver()) {
                updateLimit()
                return@handler
            }

            player.jump()
            limitUntilJump = 0
        }

        fun isCooldownOver(): Boolean {
            return when {
                JumpByReceivedHits.enabled -> limitUntilJump >= JumpByReceivedHits.hitsUntilJump
                JumpByDelay.enabled -> limitUntilJump >= JumpByDelay.ticksUntilJump
                else -> true // If none of the options are enabled, it will go automatic
            }
        }

        fun updateLimit() {
            if (JumpByReceivedHits.enabled) {
                if (player.hurtTime == 9) {
                    limitUntilJump++
                }
                return
            }

            limitUntilJump++
        }
    }

    /**
     * Duplicate exempt grim
     * This is a technique that allows you to bypass the grim anti-cheat.
     *
     * It abuses the C06 duplicate exempt to bypass the velocity check.
     *
     * After sending a finish-mining digging packet that coincides with the player's
     * collision box and canceling the knockback packet sent by the server before the player's movement packet is sent,
     * grim seems to ignore the player's knockback
     *
     * https://github.com/GrimAnticheat/Grim/issues/1133
     */
    private object ExemptGrim117 : Choice("ExemptGrim117") {
        override val parent: ChoiceConfigurable
            get() = modes

        val packetHandler = sequenceHandler<PacketEvent> {
            val packet = it.packet

            if ((packet is EntityVelocityUpdateS2CPacket && packet.id == player.id || packet is ExplosionS2CPacket)) {
                it.cancelEvent()
                waitTicks(1)
                repeat(4) {
                    network.sendPacket(Full(player.x, player.y, player.z, player.yaw, player.pitch, player.isOnGround))
                }
                network.sendPacket(PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK,
                    player.blockPos,
                    player.horizontalFacing.opposite))
            }
        }
    }


}
