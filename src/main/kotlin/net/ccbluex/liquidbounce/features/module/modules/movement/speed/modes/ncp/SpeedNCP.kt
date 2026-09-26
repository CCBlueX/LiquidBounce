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
package net.ccbluex.liquidbounce.features.module.modules.movement.speed.modes.ncp

import net.ccbluex.liquidbounce.config.types.group.ModeValueGroup
import net.ccbluex.liquidbounce.config.types.group.ToggleableValueGroup
import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.events.PlayerJumpEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.features.module.modules.movement.speed.ModuleSpeed
import net.ccbluex.liquidbounce.features.module.modules.movement.speed.modes.SpeedBHopBase
import net.ccbluex.liquidbounce.utils.client.Timer
import net.ccbluex.liquidbounce.utils.entity.horizontalSpeed
import net.ccbluex.liquidbounce.utils.entity.moving
import net.ccbluex.liquidbounce.utils.entity.withStrafe
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.ccbluex.liquidbounce.utils.math.multiply
import net.minecraft.world.effect.MobEffects

/**
 * author: @larryngton
 * tested on anticheat.test.com and eu.loyisa.cn
 * made for ncp, works on uncp and other anticheats by changing some options
 */
class SpeedNCP(parent: ModeValueGroup<*>) : SpeedBHopBase("NCP", parent) {

    private inner class PullDown(parent: EventListener?) : ToggleableValueGroup(parent, "PullDown", true) {

        private val motionMultiplier by float("MotionMultiplier", 1f, 0.01f..10f)
        private val onTick by int("OnTick", 5, 1..9)
        private val onHurt by boolean("OnHurt", true)

        private var ticksInAir = 0

        @Suppress("unused")
        private val tickHandler = tickHandler {
            if (player.onGround()) {
                ticksInAir = 0
                return@tickHandler
            } else {
                ticksInAir++
                if (ticksInAir == onTick) {
                    player.deltaMovement = player.deltaMovement.withStrafe()
                    player.deltaMovement.y -= (0.1523351824467155 * motionMultiplier)
                }
            }

            if (onHurt && player.hurtTime >= 5 && player.deltaMovement.y >= 0) {
                player.deltaMovement.y -= 0.1
            }
        }
    }

    init {
        tree(PullDown(this))
    }

    private inner class Boost(parent: EventListener?) : ToggleableValueGroup(parent, "Boost", true) {
        private val initialBoostMultiplier by float("InitialBoostMultiplier", 1f,
            0.01f..10f)

        @Suppress("unused")
        private val tickHandler = tickHandler {
            if (player.moving) {
                player.deltaMovement = player.deltaMovement.multiply(
                    factorX = 1f + (BOOST_CONSTANT * initialBoostMultiplier),
                    factorZ = 1f + (BOOST_CONSTANT * initialBoostMultiplier),
                )
            }
        }
    }

    init {
        tree(Boost(this))
    }

    private val timerBoost by boolean("Timer", true)
    private val damageBoost by boolean("DamageBoost", true) // flags with morecrits
    private val shouldLowHop by boolean("LowHop", true)
    private val shouldStrafeInAir by boolean("AirStrafe", true)

    companion object {
        private const val SPEED_CONSTANT = 0.199999999
        private const val GROUND_CONSTANT = 0.281
        private const val AIR_CONSTANT = 0.2
        private const val BOOST_CONSTANT = 0.00718
    }

    @Suppress("unused")
    private val tickHandler = tickHandler {
        val speedMultiplier = player.getEffect(MobEffects.SPEED)?.amplifier ?: 0

        if (player.moving) {
            if (player.onGround()) {
                val groundMin = GROUND_CONSTANT + SPEED_CONSTANT * speedMultiplier

                player.deltaMovement = player.deltaMovement.withStrafe(
                    speed = player.horizontalSpeed.coerceAtLeast(groundMin)
                )
            } else if (shouldStrafeInAir) {
                val airMin = AIR_CONSTANT + SPEED_CONSTANT * speedMultiplier
                player.deltaMovement = player.deltaMovement.withStrafe(
                    strength = 0.7,
                    speed = player.horizontalSpeed.coerceAtLeast(airMin)
                )
            }
        }

        if (timerBoost) {
            Timer.requestTimerSpeed(1.08f, priority = Priority.IMPORTANT_FOR_USAGE_1, provider = ModuleSpeed)
        }

        if (player.hurtTime >= 1 && damageBoost) {
            player.deltaMovement = player.deltaMovement.withStrafe(speed = player.horizontalSpeed.coerceAtLeast(0.5))
        }
    }

    @Suppress("unused")
    private val jumpHandler = handler<PlayerJumpEvent> { event ->
        if (shouldLowHop) {
            event.motion = 0.4f
        }
    }
}
