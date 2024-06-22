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
package net.ccbluex.liquidbounce.features.module.modules.movement.speed.modes

import net.ccbluex.liquidbounce.config.Choice
import net.ccbluex.liquidbounce.config.ChoiceConfigurable
import net.ccbluex.liquidbounce.config.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.Listenable
import net.ccbluex.liquidbounce.event.events.MovementInputEvent
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.events.PlayerAfterJumpEvent
import net.ccbluex.liquidbounce.event.events.PlayerJumpEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.repeatable
import net.ccbluex.liquidbounce.event.sequenceHandler
import net.ccbluex.liquidbounce.features.module.modules.combat.ModuleCriticals
import net.ccbluex.liquidbounce.features.module.modules.movement.speed.ModuleSpeed
import net.ccbluex.liquidbounce.features.module.modules.movement.speed.SpeedAntiCornerBump
import net.ccbluex.liquidbounce.utils.client.Timer
import net.ccbluex.liquidbounce.utils.entity.moving
import net.ccbluex.liquidbounce.utils.entity.sqrtSpeed
import net.ccbluex.liquidbounce.utils.entity.strafe
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket

/**
 * A highly adjustable speed mode
 *
 * Features include:
 * - Horizontal acceleration
 * - Horizontal jump off boost
 * - Vertical jump height
 * - Vertical pull down
 * - Vertical pull down during fall
 * - Strafe
 * - Timer speed
 * - Optimize for criticals
 * - Avoid edge bump
 *
 */
class SpeedCustom(override val parent: ChoiceConfigurable<*>) : Choice("Custom") {

    private class HorizontalModification(parent: Listenable?) : ToggleableConfigurable(parent,
        "HorizontalModification", true) {

        private val horizontalAcceleration by float("HorizontalAcceleration", 0f, -0.1f..0.2f)
        private val horizontalJumpOffModifier by float("HorizontalJumpOff", 0f, -0.5f..1f)

        /**
         * Allows for a delayed boost to be applied when the player jumps off the ground
         */
        private val ticksToBoostOff by int("TicksToBoostOff", 0, 0..20, "ticks")

        val repeatable = repeatable {
            if (!player.moving) {
                return@repeatable
            }

            if (horizontalAcceleration != 0f) {
                player.velocity.x *= 1f + horizontalAcceleration
                player.velocity.z *= 1f + horizontalAcceleration
            }
        }

        val onJump = sequenceHandler<PlayerAfterJumpEvent> {
            if (horizontalJumpOffModifier != 0f) {
                waitTicks(ticksToBoostOff)

                player.velocity.x *= 1f + horizontalJumpOffModifier
                player.velocity.z *= 1f + horizontalJumpOffModifier
            }
        }

    }

    private class VerticalModification(parent: Listenable?) : ToggleableConfigurable(parent,
        "VerticalModification", true) {

        private val jumpHeight by float("JumpHeight", 0.42f, 0.0f..3f)

        private val pullDown by float("Pulldown", 0f, 0f..1f)
        private val pullDownDuringFall by float("PullDownDuringFall", 0f, 0f..1f)

        val repeatable = repeatable {
            if (!player.moving) {
                return@repeatable
            }

            val pullDown = if (player.velocity.y <= 0.0) pullDownDuringFall else pullDown
            player.velocity.y -= pullDown
        }

        val onJump = handler<PlayerJumpEvent> {
            if (jumpHeight != 0.42f) {
                it.motion = jumpHeight
            }
        }

    }

    private class Strafe(parent: Listenable?) : ToggleableConfigurable(parent, "Strafe", true) {

        private val strength by float("Strength", 1f, 0.1f..1f)

        private val customSpeed by boolean("CustomSpeed", false)
        private val speed by float("Speed", 1f, 0.1f..10f)

        private val velocityTimeout by int("VelocityTimeout", 0, 0..20, "ticks")
        private val strafeKnock by boolean("StrafeKnock", false)

        private var ticksTimeout = 0

        val repeatable = repeatable {
            if (ticksTimeout > 0) {
                ticksTimeout--
                return@repeatable
            }

            if (!player.moving) {
                return@repeatable
            }

            when {
                customSpeed -> player.strafe(speed = speed.toDouble(), strength = strength.toDouble())
                else -> player.strafe(strength = strength.toDouble())
            }
        }

        val packetHandler = sequenceHandler<PacketEvent> {
            val packet = it.packet

            if (packet is EntityVelocityUpdateS2CPacket && packet.id == player.id) {
                val velocityX = packet.velocityX / 8000.0
                val velocityY = packet.velocityY / 8000.0
                val velocityZ = packet.velocityZ / 8000.0

                ticksTimeout = velocityTimeout

                if (strafeKnock) {
                    waitTicks(1)

                    // Fall damage velocity
                    val speed = if (velocityX == 0.0 && velocityZ == 0.0 && velocityY == -0.078375) {
                        player.sqrtSpeed.coerceAtLeast(0.2857671997172534)
                    } else {
                        player.sqrtSpeed
                    }
                    player.strafe(speed = speed)
                }
            }
        }

    }

    init {
        tree(HorizontalModification(this))
        tree(VerticalModification(this))
    }

    private val timerSpeed by float("TimerSpeed", 1f, 0.1f..10f)

    private val optimizeForCriticals by boolean("OptimizeForCriticals", true)
    private val avoidEdgeBump by boolean("AvoidEdgeBump", true)

    init {
        tree(Strafe(this))
    }

    val repeatable = repeatable {
        if (!player.moving) {
            return@repeatable
        }

        if (timerSpeed != 1f) {
            Timer.requestTimerSpeed(timerSpeed, Priority.IMPORTANT_FOR_USAGE_1, ModuleSpeed)
        }
    }

    val handleJump = handler<MovementInputEvent> {
        if (!player.moving || doOptimizationsPreventJump()) {
            return@handler
        }

        it.jumping = true
    }

    private fun doOptimizationsPreventJump(): Boolean {
        if (optimizeForCriticals && ModuleCriticals.shouldWaitForJump(0.42f)) {
            return true
        }

        if (avoidEdgeBump && SpeedAntiCornerBump.shouldDelayJump()) {
            return true
        }

        return false
    }

}
