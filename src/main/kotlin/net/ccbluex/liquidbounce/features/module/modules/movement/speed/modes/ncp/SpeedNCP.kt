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
import net.ccbluex.liquidbounce.config.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.Listenable
import net.ccbluex.liquidbounce.event.events.PlayerJumpEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.repeatable
import net.ccbluex.liquidbounce.features.module.modules.movement.speed.ModuleSpeed
import net.ccbluex.liquidbounce.features.module.modules.movement.speed.modes.SpeedBHopBase
import net.ccbluex.liquidbounce.utils.client.Timer
import net.ccbluex.liquidbounce.utils.entity.moving
import net.ccbluex.liquidbounce.utils.entity.sqrtSpeed
import net.ccbluex.liquidbounce.utils.entity.strafe
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.minecraft.entity.effect.StatusEffects

/**
 * author: @larryngton
 * tested on anticheat.test.com and eu.loyisa.cn
 * made for ncp, works on uncp and other anticheats by disabling some options
 */

class SpeedNCP(override val parent: ChoiceConfigurable<*>) : SpeedBHopBase("NCP", parent) {

    private class PullDown(parent: Listenable?) : ToggleableConfigurable(parent, "PullDown", true) {
        private val ontick by int("OnTick", 5, 5..9)
        private val onhurt by boolean("OnHurt", true)

        var airticks = 0

        val repeatable = repeatable {
            if (player.isOnGround) {
                airticks = 0
                return@repeatable
            } else {
                airticks++
                if (airticks == ontick) {
                    player.strafe()
                    player.velocity.y = -0.1523351824467155
                }
            }
            if (onhurt && player.hurtTime >= 5 && player.velocity.y >= 0) {
                player.velocity.y -= 0.1
            }
        }
    }

    init {
        tree(PullDown(this))
    }

    private val boost by boolean("Boost", true)
    private val timerboost by boolean("Timer", true)
    private val damageboost by boolean("DamageBoost", true) // flags with morecrits
    private val lowhop by boolean("LowHop", true)
    private val airstrafe by boolean("AirStrafe", true)

    val repeatable = repeatable {

        val speedconst = 0.199999999

        val groundmin = 0.281 + speedconst * (player.getStatusEffect(StatusEffects.SPEED)?.amplifier ?: 0)
        val airmin = 0.2 + speedconst * (player.getStatusEffect(StatusEffects.SPEED)?.amplifier ?: 0)

        if (player.isOnGround && player.moving) {
            player.strafe(speed = player.sqrtSpeed.coerceAtLeast(groundmin))
        } else {
            if (player.moving && airstrafe) {
                player.strafe(strength = 0.7, speed = player.sqrtSpeed.coerceAtLeast(airmin))
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
            player.velocity.x *= 1f + 0.00718
            player.velocity.z *= 1f + 0.00718
        }

        if (player.hurtTime >= 1 && damageboost) {
            player.strafe(speed = player.sqrtSpeed.coerceAtLeast(0.5))
        }
    }

    val onJump = handler<PlayerJumpEvent> {
        if (lowhop) {
            it.motion = 0.4f
        }
    }
}
