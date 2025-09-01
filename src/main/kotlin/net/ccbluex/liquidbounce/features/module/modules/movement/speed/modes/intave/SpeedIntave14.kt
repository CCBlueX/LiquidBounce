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
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.speed.modes.intave

import net.ccbluex.liquidbounce.config.types.nesting.ChoiceConfigurable
import net.ccbluex.liquidbounce.config.types.nesting.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.features.module.modules.movement.speed.modes.SpeedBHopBase
import net.ccbluex.liquidbounce.utils.entity.airTicks
import net.ccbluex.liquidbounce.utils.entity.withStrafe

/**
 * Intave 14 speed
 *
 * @author larryngton
 */
class SpeedIntave14(override val parent: ChoiceConfigurable<*>) : SpeedBHopBase("Intave14", parent) {
    companion object {
        private const val BOOST_CONSTANT = 0.003
    }

    private inner class Strafe(parent: EventListener) : ToggleableConfigurable(parent, "Strafe", true) {
        private val strength by float("Strength", 0.27f, 0.01f..0.27f)

        @Suppress("unused")
        private val tickHandler = tickHandler {
            if (player.isSprinting && (player.isOnGround || player.airTicks == 11)) {
                player.velocity = player.velocity.withStrafe(strength = strength.toDouble())
            }
        }
    }

    private inner class AirBoost(parent: EventListener) : ToggleableConfigurable(parent, "AirBoost", true) {

        @Suppress("unused")
        private val tickHandler = tickHandler {
            if (player.velocity.y > 0.003 && player.isSprinting) {
                player.velocity.x *= 1f + (BOOST_CONSTANT * 0.25)
                player.velocity.z *= 1f + (BOOST_CONSTANT * 0.25)
            }
        }
    }

    init {
        tree(Strafe(this))
        tree(AirBoost(this))
    }
}
