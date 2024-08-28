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
import net.ccbluex.liquidbounce.features.module.modules.player.nofall.modes.NoFallBlink
import net.ccbluex.liquidbounce.utils.entity.directionYaw
import net.ccbluex.liquidbounce.utils.entity.sqrtSpeed
import net.ccbluex.liquidbounce.utils.entity.strafe
import net.minecraft.network.listener.ClientPlayPacketListener
import net.minecraft.network.packet.Packet
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.Full
import net.minecraft.network.packet.s2c.play.EntityDamageS2CPacket
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket
import net.minecraft.network.packet.s2c.play.ExplosionS2CPacket
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket

/**
 * Velocity module
 *
 * Modifies the amount of velocity you take.
 */

object ModuleVelocity : Module("Velocity", Category.COMBAT) {

    init {
        enableLock()
    }

    val modes = choices<Choice>("Mode", { Modify }) {
        arrayOf(
            Modify, Watchdog, Strafe, AAC442, ExemptGrim117, Dexland, JumpReset
        )
    }.apply { tagBy(this) }

    private val delay by intRange("Delay", 0..0, 0..40, "ticks")
    private val pauseOnFlag by int("PauseOnFlag", 0, 0..5, "ticks")

    var pause = 0

    val repeatable = repeatable {
        if (pause > 0) {
            pause--
        }
    }

    val packetHandler = sequenceHandler<PacketEvent>(priority = 1) {
        val packet = it.packet

        if (!it.original) {
            return@sequenceHandler
        }

        if (packet is EntityVelocityUpdateS2CPacket && packet.entityId == player.id || packet is ExplosionS2CPacket) {
            // When delay is above 0, we will delay the velocity update
            if (delay.last > 0) {
                it.cancelEvent()

                delay.random().let { ticks ->
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
        } else if (packet is PlayerPositionLookS2CPacket) {
            pause = pauseOnFlag
        }
    }

    /**
     *
     * Velocity for AAC4.4.2, pretty sure, it works on other versions
     */

    private object AAC442 : Choice("AAC4.4.2") {

        override val parent: ChoiceConfigurable<Choice>
            get() = modes

        val aac442MotionReducer by float("AAC4.4.2MotionReducer", 0.62f, 0f..1f)

        val repeatable = repeatable {
            if (player.hurtTime > 0 && !player.isOnGround) {
                val reduce = aac442MotionReducer
                player.velocity.x *= reduce
                player.velocity.z *= reduce
            }
        }

        override fun handleEvents() = super.handleEvents() && pause == 0

    }

    /**
     * Basic velocity which should bypass the most server with regular anti-cheats like NCP.
     */
    private object Modify : Choice("Modify") {

        override val parent: ChoiceConfigurable<Choice>
            get() = modes

        val horizontal by float("Horizontal", 0f, -1f..1f)
        val vertical by float("Vertical", 0f, -1f..1f)

        val motionHorizontal by float("MotionHorizontal", 0f, 0f..1f)
        val motionVertical by float("MotionVertical", 0f, 0f..1f)

        val packetHandler = handler<PacketEvent> { event ->
            val packet = event.packet

            // Check if this is a regular velocity update
            if (packet is EntityVelocityUpdateS2CPacket && packet.entityId == player.id) {
                // It should just block the packet
                if (horizontal == 0f && vertical == 0f) {
                    event.cancelEvent()
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
            } else if (packet is ExplosionS2CPacket) { // Check if velocity is affected by explosion
                // note: explosion packets are being used by hypixel to trick poorly made cheats.

                //  Modify packet according to the specified values
                packet.playerVelocityX *= horizontal
                packet.playerVelocityY *= vertical
                packet.playerVelocityZ *= horizontal

                NoFallBlink.waitUntilGround = true
            }
        }

        override fun handleEvents() = super.handleEvents() && pause == 0

    }

    private object Watchdog : Choice("Watchdog") {

        override val parent: ChoiceConfigurable<Choice>
            get() = modes

        val packetHandler = handler<PacketEvent> { event ->
            val packet = event.packet

            // Check if this is a regular velocity update
            if (packet is EntityVelocityUpdateS2CPacket && packet.entityId == player.id) {
                if (player.isOnGround) {
                    packet.velocityX = (player.velocity.x * 8000).toInt()
                    packet.velocityZ = (player.velocity.z * 8000).toInt()
                } else {
                    event.cancelEvent()
                }
            }
        }

        override fun handleEvents() = super.handleEvents() && pause == 0

    }

    private object Dexland : Choice("Dexland") {

        override val parent: ChoiceConfigurable<Choice>
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

        override fun handleEvents() = super.handleEvents() && pause == 0

    }

    /**
     * Strafe velocity
     */
    private object Strafe : Choice("Strafe") {

        override val parent: ChoiceConfigurable<Choice>
            get() = modes

        val delay by int("Delay", 2, 0..10, "ticks")
        val strength by float("Strength", 1f, 0.1f..2f)
        val untilGround by boolean("UntilGround", false)

        var applyStrafe = false

        val packetHandler = sequenceHandler<PacketEvent> { event ->
            val packet = event.packet

            // Check if this is a regular velocity update
            if ((packet is EntityVelocityUpdateS2CPacket && packet.entityId == player.id)
                || packet is ExplosionS2CPacket) {
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

        override fun handleEvents() = super.handleEvents() && pause == 0

    }

    /**
     * Jump Reset mode. A technique most players use to minimize the amount of knockback they get.
     */
    private object JumpReset : Choice("JumpReset") {

        override val parent: ChoiceConfigurable<Choice>
            get() = modes

        object JumpByReceivedHits : ToggleableConfigurable(ModuleVelocity, "JumpByReceivedHits", false) {
            val hitsUntilJump by int("HitsUntilJump", 2, 0..10)
        }

        object JumpByDelay : ToggleableConfigurable(ModuleVelocity, "JumpByDelay", true) {
            val ticksUntilJump by int("UntilJump", 2, 0..20, "ticks")
        }

        init {
            tree(JumpByReceivedHits)
            tree(JumpByDelay)
        }

        var limitUntilJump = 0

        val tickJumpHandler = handler<MovementInputEvent> {
            // To be able to alter velocity when receiving knockback, player must be sprinting.
            if (player.hurtTime != 9 || !player.isOnGround || !player.isSprinting || !isCooldownOver()) {
                updateLimit()
                return@handler
            }

            it.jumping = true
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

        override fun handleEvents() = super.handleEvents() && pause == 0

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
        override val parent: ChoiceConfigurable<Choice>
            get() = modes

        private var canCancel = false

        override fun enable() {
            canCancel = false
        }

        val packetHandler = sequenceHandler<PacketEvent> {
            val packet = it.packet

            // Check for damage to make sure it will only cancel
            // damage velocity (that all we need) and not affect other types of velocity
            if (packet is EntityDamageS2CPacket && packet.entityId == player.id) {
                canCancel = true
            }

            if ((packet is EntityVelocityUpdateS2CPacket && packet.entityId == player.id
                    || packet is ExplosionS2CPacket)
                && canCancel) {
                it.cancelEvent()
                waitTicks(1)
                repeat(4) {
                    network.sendPacket(Full(player.x, player.y, player.z, player.yaw, player.pitch, player.isOnGround))
                }
                network.sendPacket(PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK,
                    player.blockPos,
                    player.horizontalFacing.opposite))
                canCancel = false
            }
        }

        override fun handleEvents() = super.handleEvents() && pause == 0

    }

}
