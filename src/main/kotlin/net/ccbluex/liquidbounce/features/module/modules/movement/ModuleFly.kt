/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2016 - 2021 CCBlueX
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
package net.ccbluex.liquidbounce.features.module.modules.movement

import net.ccbluex.liquidbounce.config.Choice
import net.ccbluex.liquidbounce.config.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.PlayerStrideEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.repeatable
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.entity.strafe

/**
 * Fly module
 *
 * Allows you to fly.
 */
object ModuleFly : Module("Fly", Category.MOVEMENT) {

    private val modes = choices("Mode", "Vanilla") {
        Vanilla
        Jetpack
    }

    private object Visuals : ToggleableConfigurable(this, "Visuals", true) {

        private val stride by boolean("Stride", true)

        val strideHandler = handler<PlayerStrideEvent> { event ->
            event.strideOnAir = stride
        }

    }

    private object Vanilla : Choice("Vanilla", modes) {

        val repeatable = repeatable {
            player.strafe(speed = 0.44)
            player.velocity.y = when {
                player.input.jumping -> 0.31
                player.input.sneaking -> -0.31
                else -> 0.0
            }
        }

    }

    private object Jetpack : Choice("Jetpack", modes) {

        val repeatable = repeatable {
            if (player.input.jumping) {
                player.velocity.x *= 1.1
                player.velocity.y += 0.15
                player.velocity.z *= 1.1
            }
        }

    }

    init {
        tree(Visuals)
    }

}
