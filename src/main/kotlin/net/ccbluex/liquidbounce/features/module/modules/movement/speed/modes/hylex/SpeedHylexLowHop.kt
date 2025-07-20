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
package net.ccbluex.liquidbounce.features.module.modules.movement.speed.modes.hylex

import net.ccbluex.liquidbounce.config.types.ChoiceConfigurable
import net.ccbluex.liquidbounce.event.events.PlayerJumpEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.features.module.modules.movement.speed.modes.SpeedBHopBase
import net.ccbluex.liquidbounce.utils.entity.moving
import net.ccbluex.liquidbounce.utils.entity.sqrtSpeed

/**
 * Hylex LowHop
 *
 * Works because of a silly exemption from Hylex
 * @author @liquidsquid1
 */
class SpeedHylexLowHop(override val parent: ChoiceConfigurable<*>) : SpeedBHopBase("HylexLowHop", parent) {

    private var airTicks = 0

    @Suppress("unused")
    private val tickHandler = tickHandler {
        if (player.isOnGround) {
            airTicks = 0
            if (player.moving && player.sqrtSpeed < 0.32) {

                player.velocity = player.velocity.multiply(
                    1.1,
                    1.0,
                    1.1
                )
            }
            return@tickHandler
        }
        airTicks++

        if (airTicks == 9 && player.sqrtSpeed < 0.29) {
            player.velocity = player.velocity.multiply(
                1.007,
                1.0,
                1.007
            )
        }

        if (airTicks == 1 && player.sqrtSpeed < 0.20) {
            player.velocity = player.velocity.multiply(
                1.01,
                1.0,
                1.01
            )
        }

        if (player.velocity.y > 0 && airTicks <= 2 && player.sqrtSpeed < 0.2) {
            player.velocity = player.velocity.multiply(
                1.02,
                1.0,
                1.02
            )
        }

    }

    override fun enable() {
        airTicks = 0
        super.enable()
    }

    @Suppress("unused")
    private val jumpHandler = handler<PlayerJumpEvent> { event ->
        event.motion = 0.33f
    }

}
