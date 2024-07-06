/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015-2024 CCBlueX
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
package net.ccbluex.liquidbounce.features.module.modules.movement.speed.modes.watchdog

import net.ccbluex.liquidbounce.config.Choice
import net.ccbluex.liquidbounce.config.ChoiceConfigurable
import net.ccbluex.liquidbounce.event.events.MovementInputEvent
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.events.PlayerJumpEvent
import net.ccbluex.liquidbounce.event.events.PlayerMoveEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.repeatable
import net.ccbluex.liquidbounce.event.sequenceHandler
import net.ccbluex.liquidbounce.features.module.modules.movement.speed.ModuleSpeed
import net.ccbluex.liquidbounce.utils.aiming.AimPlan
import net.ccbluex.liquidbounce.utils.aiming.Rotation
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.aiming.angleSmooth.AngleSmoothMode
import net.ccbluex.liquidbounce.utils.aiming.angleSmooth.ConditionalLinearAngleSmoothMode
import net.ccbluex.liquidbounce.utils.aiming.angleSmooth.LinearAngleSmoothMode
import net.ccbluex.liquidbounce.utils.aiming.angleSmooth.SigmoidAngleSmoothMode
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.entity.directionYaw
import net.ccbluex.liquidbounce.utils.entity.moving
import net.ccbluex.liquidbounce.utils.entity.sqrtSpeed
import net.ccbluex.liquidbounce.utils.entity.strafe
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.ccbluex.liquidbounce.utils.movement.DirectionalInput
import net.minecraft.entity.Entity
import net.minecraft.entity.MovementType
import net.minecraft.entity.effect.StatusEffects
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket
import net.minecraft.util.math.Vec3d
import kotlin.math.max
import kotlin.math.min

/**
 * @anticheat Watchdog (NCP)
 * @anticheatVersion 12.12.2023
 * @testedOn hypixel.net
 */
class SpeedHypixelDev(override val parent: ChoiceConfigurable<*>) : Choice("HypixelDev") {

    private var accel = 0.0
    private var ticksNoStrafe = 0
    private var airTicks = 0
    private var lastDirection = 0.0F
    private var strafed = false

    private val thing = object : AngleSmoothMode("Snap") {
        override fun limitAngleChange(
            currentRotation: Rotation,
            targetRotation: Rotation,
            vec3d: Vec3d?,
            entity: Entity?
        ): Rotation {
            return targetRotation
        }

        override fun howLongToReach(currentRotation: Rotation, targetRotation: Rotation): Int {
            return 1
        }

        override val parent: ChoiceConfigurable<*>
            get() = angleSmooth
    }

    var angleSmooth: ChoiceConfigurable<AngleSmoothMode> = choices(this, "AngleSmooth", { it.choices[0] }) {
        arrayOf(
            thing
        )
    }

    companion object {

        private const val BASE_HORIZONTAL_MODIFIER = 0.00175

        private const val GLIDE_VALUE = 0.0008

        /**
         * Vanilla maximum speed
         * w/o: 0.2857671997172534
         * w/ Speed 1: 0.2919055664000211
         * w/ Speed 2: 0.2999088445964323
         *
         * Speed mod: 0.008003278196411223
         */
        private const val AT_LEAST = 0.281
        private const val BASH = 0.2857671997172534
        private const val SPEED_EFFECT_CONST = 0.008003278196411223

    }

    val moveHandler = handler<PlayerMoveEvent> {
        if (it.type != MovementType.SELF) return@handler

        if (!player.moving) {
            accel = 0.0
        }

        if (player.isOnGround) {
            ticksNoStrafe = 0
            airTicks = 0
        } else {
            ticksNoStrafe--
            airTicks++

            if (ticksNoStrafe > 0) {
                return@handler
            }

            when (airTicks) {
                1,5,7,9  -> {
                    val curDirection = player.directionYaw
                    val delta = RotationManager.angleDifference(lastDirection, curDirection)

                    val strength = if (strafed) {
                        0.4
                    } else {
                        0.8
                    } + 0.2 * min(1.0, delta / 180.0)

                    chat("delta $delta on tick $airTicks - going to $curDirection")

                    it.movement.strafe(
                        speed = if (airTicks == 1) {
                            (BASH / if (delta > 45) {
                                1.0
                            } else {
                                1.175
                            }) * 0.97
                        } else {
                            player.sqrtSpeed * 0.96 - BASE_HORIZONTAL_MODIFIER * airTicks
                        },
                        yaw = curDirection,
                        strength = strength
                    )

                    RotationManager.aimAt(AimPlan(
                        Rotation(player.directionYaw, player.pitch),
                        angleSmooth = thing,
                        applyVelocityFix = false,
                        changeLook = false,
                        considerInventory = false,
                        resetThreshold = 360.0F,
                        ticksUntilReset = 1
                    ), Priority.IMPORTANT_FOR_USER_SAFETY, ModuleSpeed)

                    if (airTicks != 1) {
                        player.velocity.y *= 1 - GLIDE_VALUE * max(1, 8 - airTicks)
                    }

                    strafed = true
                    lastDirection = curDirection
                }

            }

            if (airTicks < 9) {
                return@handler
            }

            it.movement.x *= 0.98
            it.movement.z *= 0.98

        }
    }

    val jumpEvent = handler<PlayerJumpEvent> {
        if (player.sqrtSpeed > 0.25) {
            player.strafe(speed = player.sqrtSpeed.coerceAtLeast(AT_LEAST))
            accel = 0.0
        } else {
            player.strafe(speed = if (strafed) {
                AT_LEAST
            } else {
                BASH
            } * 0.975 + (SPEED_EFFECT_CONST * (player.getStatusEffect(StatusEffects.SPEED)?.amplifier ?: 0)) * 0.99)
        }
        strafed = false
        lastDirection = player.directionYaw
    }

    val moveInputHandler = handler<MovementInputEvent> {
        if (!player.isOnGround || !player.moving) {
            return@handler
        }

        if (ModuleSpeed.shouldDelayJump()) {
            return@handler
        }

        it.jumping = true
    }

    val packetHandler = handler<PacketEvent> {
        val packet = it.packet

        if (packet is EntityVelocityUpdateS2CPacket && packet.id == player.id) {
            val velocityX = packet.velocityX / 8000.0
            val velocityY = packet.velocityY / 8000.0
            val velocityZ = packet.velocityZ / 8000.0

            if (velocityX == 0.0 && velocityZ == 0.0) {
                accel = 0.0
                return@handler
            }

            if (velocityY < player.velocity.y || player.fallDistance > 9.4E-3) {
                player.velocity.y = velocityY
            }

            val speed = player.sqrtSpeed
            val horizontalModifier = AT_LEAST + BASE_HORIZONTAL_MODIFIER +
                SPEED_EFFECT_CONST * (player.getStatusEffect(StatusEffects.SPEED)?.amplifier ?: 0)

            if (speed < horizontalModifier) {
                return@handler
            }

            player.strafe(yaw = RotationManager.serverRotation.yaw, speed = horizontalModifier)
            ticksNoStrafe = 3

        }
    }

    override fun disable() {
        accel = 0.0
        airTicks = 0
        lastDirection = 0.0F
    }

    override fun enable() {
        ticksNoStrafe = 9999
    }

}
