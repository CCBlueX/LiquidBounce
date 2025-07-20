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
 */
package net.ccbluex.liquidbounce.features.module.modules.combat.velocity.mode

import net.ccbluex.liquidbounce.event.events.AttackEntityEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.utils.entity.moving

/**
 * Hylex Velocity
 *
 * Works because of a silly exemption from Hylex
 * @author @liquidsquid1
 */
object VelocityHylex : VelocityMode("Hylex") {

    @Suppress("unused")
    private val attackHandler = handler<AttackEntityEvent> { event ->
        if (event.isCancelled || !player.moving || !player.isSprinting) {
            return@handler
        }

        when (player.hurtTime) {
            9 -> {
                player.velocity = player.velocity.multiply(
                    0.8,
                    1.0,
                    0.8
                )
            }
            8 -> {
                player.velocity = player.velocity.multiply(
                    0.11,
                    1.0,
                    0.11
                )
            }
            7 -> player.velocity = player.velocity.multiply(
                0.4,
                1.0,
                0.4
            )
            4 -> player.velocity = player.velocity.multiply(
                0.37,
                1.0,
                0.37
            )
        }
    }

    @Suppress("unused")
    private val repeatable = tickHandler {
        val shouldJump = player.hurtTime > 5
        val canJump = player.isOnGround

        if (shouldJump && canJump) {
            player.jump()
        }
    }
}
