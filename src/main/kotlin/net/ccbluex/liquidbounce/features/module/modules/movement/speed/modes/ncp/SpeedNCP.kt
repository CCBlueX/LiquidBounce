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
package net.ccbluex.liquidbounce.features.module.modules.movement.speed.modes.ncp

import net.ccbluex.liquidbounce.config.ChoiceConfigurable
import net.ccbluex.liquidbounce.event.events.PlayerAfterJumpEvent
import net.ccbluex.liquidbounce.event.events.PlayerJumpEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.repeatable
import net.ccbluex.liquidbounce.event.sequenceHandler
import net.ccbluex.liquidbounce.features.module.modules.movement.speed.ModuleSpeed
import net.ccbluex.liquidbounce.features.module.modules.movement.speed.modes.SpeedBHopBase
import net.ccbluex.liquidbounce.utils.client.Timer
import net.ccbluex.liquidbounce.utils.entity.moving
import net.ccbluex.liquidbounce.utils.entity.strafe
import net.ccbluex.liquidbounce.utils.kotlin.Priority

/**
 * author: @larryngton
 * tested on anticheat.test.com and eu.loyisa.cn
 * made for ncp, works on uncp and other anticheats by disabling some options
 */

class SpeedNCP(override val parent: ChoiceConfigurable<*>) : SpeedBHopBase("NCP", parent) {

    private val boost by boolean("Boost", true)
    private val timerboost by boolean("Timer", true)
    private val damageboost by boolean("DamageBoost", true) // flags with morecrits
    private val pulldown by boolean("PullDown", true)
    private val lowhop by boolean("LowHop", true)
    private val airstrafe by boolean("AirStrafe", true)
    private val morecrits by boolean("MoreCrits", true)

    val repeatable = repeatable {

        if (player.isOnGround && player.moving) {
            player.strafe(strength = 0.7)
        } else {
            if (player.moving && airstrafe) {
                player.strafe(strength = 0.7)
            }
        }

        if (timerboost && player.hurtTime <= 1) {
            if (player.isOnGround) {
                Timer.requestTimerSpeed(
                    timerSpeed = 1.5f,
                    priority = Priority.IMPORTANT_FOR_USAGE_1,
                    provider = ModuleSpeed
                )
            } else {
                Timer.requestTimerSpeed(
                    timerSpeed = 1.08f,
                    priority = Priority.IMPORTANT_FOR_USAGE_1,
                    provider = ModuleSpeed
                )
            }

            if (player.velocity.y <= 0) {
                Timer.requestTimerSpeed(
                    timerSpeed = 1.1f,
                    priority = Priority.IMPORTANT_FOR_USAGE_1,
                    provider = ModuleSpeed
                )
            }
        } else {
            if (timerboost) {
                Timer.requestTimerSpeed(
                    timerSpeed = 1.08f,
                    priority = Priority.IMPORTANT_FOR_USAGE_1,
                    provider = ModuleSpeed
                )
            }
        }

        if (player.moving && boost) {
            player.velocity.x *= 1f + 0.01
            player.velocity.z *= 1f + 0.01
        }

        if (player.velocity.y <= 0 && pulldown) {
            player.velocity.y -= 0.02
        }

        if (player.hurtTime >= 1 && damageboost) {
            player.strafe(speed = 0.5)
        }

        if (morecrits && player.hurtTime >= 5 && player.velocity.y >= 0) {
            player.velocity.y -= 0.1
        }
    }

    val afterJumpEvent = sequenceHandler<PlayerAfterJumpEvent> {
        if (boost) {
            player.velocity.x *= 1f + -0.035
            player.velocity.z *= 1f + -0.035
        }
    }

    val onJump = handler<PlayerJumpEvent> {
        if (lowhop) {
            it.motion = 0.4f
        }
    }
}
