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
package net.ccbluex.liquidbounce.features.module.modules.movement.speed.modes.intave

import net.ccbluex.liquidbounce.config.types.group.ModeValueGroup
import net.ccbluex.liquidbounce.config.types.group.ToggleableValueGroup
import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.features.module.modules.movement.speed.modes.SpeedBHopBase
import net.ccbluex.liquidbounce.utils.entity.airTicks
import net.ccbluex.liquidbounce.utils.entity.withStrafe
import net.ccbluex.liquidbounce.utils.math.multiply

/**
 * Intave 14 speed
 *
 * @author larryngton
 */
class SpeedIntave14(parent: ModeValueGroup<*>) : SpeedBHopBase("Intave14", parent) {
    companion object {
        private const val BOOST_CONSTANT = 0.003
    }

    private inner class Strafe(parent: EventListener) : ToggleableValueGroup(parent, "Strafe", true) {
        private val strength by float("Strength", 0.27f, 0.01f..0.27f)

        @Suppress("unused")
        private val tickHandler = tickHandler {
            if (player.isSprinting && (player.onGround() || player.airTicks == 11)) {
                player.deltaMovement = player.deltaMovement.withStrafe(strength = strength.toDouble())
            }
        }
    }

    private inner class AirBoost(parent: EventListener) : ToggleableValueGroup(parent, "AirBoost", true) {

        @Suppress("unused")
        private val tickHandler = tickHandler {
            if (player.deltaMovement.y > 0.003 && player.isSprinting) {
                player.deltaMovement = player.deltaMovement.multiply(
                    factorX = 1f + (BOOST_CONSTANT * 0.25),
                    factorZ = 1f + (BOOST_CONSTANT * 0.25),
                )
            }
        }
    }

    init {
        tree(Strafe(this))
        tree(AirBoost(this))
    }
}
