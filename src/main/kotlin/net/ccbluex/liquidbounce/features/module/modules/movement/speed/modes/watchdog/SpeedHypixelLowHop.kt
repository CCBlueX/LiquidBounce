/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2025 CCBlueX
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

import net.ccbluex.liquidbounce.config.types.ChoiceConfigurable
import net.ccbluex.liquidbounce.event.events.PlayerJumpEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.features.module.modules.movement.speed.modes.SpeedBHopBase
import net.ccbluex.liquidbounce.utils.entity.airTicks
import net.ccbluex.liquidbounce.utils.entity.sqrtSpeed
import net.ccbluex.liquidbounce.utils.entity.withStrafe
import net.minecraft.entity.effect.StatusEffects
import net.minecraft.util.shape.VoxelShapes

/**
 * @anticheat Watchdog (NCP)
 * @anticheatVersion 25.01.25
 * @testedOn hypixel.net
 */
class SpeedHypixelLowHop(override val parent: ChoiceConfigurable<*>) : SpeedBHopBase("HypixelLowHop", parent) {

    companion object {
        var shouldStrafe = false
    }

    private var glide by boolean("Glide", false)

    @Suppress("unused")
    val tickHandler = tickHandler {
        shouldStrafe = false

        if (player.isOnGround) {
            player.velocity = player.velocity.withStrafe()
            shouldStrafe = true
        } else {
            when (player.airTicks) {
                1 -> {
                    player.velocity = player.velocity.withStrafe()
                    shouldStrafe = true
                    player.velocity.y += 0.0568
                }
                3 -> {
                    player.velocity.x *= 0.95
                    player.velocity.y -= 0.13
                    player.velocity.z *= 0.95
                }
                4 -> player.velocity.y -= 0.2
                7 -> {
                    if (glide && isGroundExempt()) {
                        player.velocity.y = 0.0
                    }
                }
            }

            if (isGroundExempt()) {
                player.velocity = player.velocity.withStrafe()
            }

            if (player.hurtTime == 9) {
                player.velocity = player.velocity.withStrafe(speed = player.sqrtSpeed.coerceAtLeast(0.281))
            }

            if ((player.getStatusEffect(StatusEffects.SPEED)?.amplifier ?: 0) == 2) {
                when (player.airTicks) {
                    1, 2, 5, 6, 8 -> player.velocity = player.velocity.multiply(1.2,1.0,1.2)
                }
            }
        }
    }

    @Suppress("unused")
    private val jumpHandler = handler<PlayerJumpEvent> {
        val atLeast = 0.247 + 0.15 * (player.getStatusEffect(StatusEffects.SPEED)?.amplifier ?: 0)

        player.velocity = player.velocity.withStrafe(speed = player.sqrtSpeed.coerceAtLeast(atLeast))
        shouldStrafe = true
    }

    private fun isGroundExempt() =
        world.getBlockCollisions(player, player.boundingBox.offset(0.0, -0.66, 0.0)).any { shape ->
            shape != VoxelShapes.empty()
        } && player.velocity.y < 0

}
