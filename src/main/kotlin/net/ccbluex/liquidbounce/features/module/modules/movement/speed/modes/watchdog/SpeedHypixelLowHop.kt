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
import net.ccbluex.liquidbounce.event.events.PlayerJumpEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.repeatable
import net.ccbluex.liquidbounce.features.module.modules.movement.speed.ModuleSpeed
import net.ccbluex.liquidbounce.utils.entity.moving
import net.ccbluex.liquidbounce.utils.entity.sqrtSpeed
import net.ccbluex.liquidbounce.utils.entity.strafe
import net.minecraft.entity.effect.StatusEffects

/**
 * @anticheat Watchdog (NCP)
 * @anticheatVersion 01.10.24
 * @testedOn hypixel.net
 */
class SpeedHypixelLowHop(override val parent: ChoiceConfigurable<*>) : Choice("HypixelLowHop") {

    companion object {
        var shouldStrafe = false
    }

    private var glide by boolean("Glide", true)

    private var airTicks = 0
    @Suppress("unused")
    val repeatable = repeatable {
        shouldStrafe = false

        if (player.isOnGround) {
            player.strafe()
            shouldStrafe = true
            airTicks = 0
            return@repeatable
        } else {
            airTicks++

            when (airTicks) {
                1 -> {
                    player.strafe()
                    shouldStrafe = true
                }
                5 -> player.velocity.y -= 0.1905189780583944
                4 -> player.velocity.y -= 0.03
                6 -> player.velocity.y *= 1.01
                7 -> if (glide) player.velocity.y /= 1.5
            }

            if (airTicks >= 7 && glide) {
                player.strafe(speed = player.sqrtSpeed.coerceAtLeast(0.281), strength = 0.7)
                shouldStrafe = true
            }

            if (player.hurtTime == 9) {
                player.strafe()
                shouldStrafe = true
            }

            if ((player.getStatusEffect(StatusEffects.SPEED)?.amplifier ?: 0) == 2) {
                when (airTicks) {
                    1, 2, 5, 6, 8 -> player.velocity = player.velocity.multiply(1.2,1.0,1.2)
                }
            }
        }
    }

    val jumpEvent = handler<PlayerJumpEvent> {
        val atLeast = 0.281 + 0.13 * (player.getStatusEffect(StatusEffects.SPEED)?.amplifier ?: 0)

        player.strafe(speed = player.sqrtSpeed.coerceAtLeast(atLeast))
    }

    val moveHandler = handler<MovementInputEvent> {
        if (!player.isOnGround || !player.moving) {
            return@handler
        }

        if (ModuleSpeed.shouldDelayJump()) {
            return@handler
        }

        it.jumping = true
    }

    override fun disable() {
        airTicks = 0
    }

}
