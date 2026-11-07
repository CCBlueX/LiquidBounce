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

package net.ccbluex.liquidbounce.utils.navigation.flight

import net.ccbluex.liquidbounce.features.module.modules.movement.fly.modes.FlyVanilla
import net.ccbluex.liquidbounce.utils.client.player

/**
 * How the player's speed follows the movement keys while flying.
 */
enum class FlightControl {
    /**
     * Keys accelerate and the speed fades once they are released, as in creative flight.
     */
    MOMENTUM,

    /**
     * Keys set the speed right away and releasing them stops, as with the Vanilla mode of Fly.
     */
    DIRECT,
}

/**
 * How the player flies, captured on the render thread for one search.
 */
class FlightAbilities(
    val control: FlightControl,

    /**
     * Top speed in blocks per tick.
     */
    val horizontalSpeed: Double,
    val verticalSpeed: Double,

    /**
     * Whether the player flies already, so no take-off is needed.
     */
    val isFlying: Boolean,
) {

    /**
     * Ticks per block.
     */
    val horizontalCost: Double
        get() = 1.0 / horizontalSpeed

    val verticalCost: Double
        get() = 1.0 / verticalSpeed

    companion object {

        /**
         * Momentum flight keeps this much of its speed every tick, horizontally and vertically.
         */
        const val HORIZONTAL_DRAG = 0.91
        const val VERTICAL_DRAG = 0.6

        private const val INPUT_SCALE = 0.98
        private const val VERTICAL_THRUST = 3.0

        /**
         * How the player can fly right now: with the Vanilla mode of Fly, with flying allowed by the game mode or
         * the server, or not at all.
         */
        fun capture(): FlightAbilities? {
            if (FlyVanilla.running) {
                return FlightAbilities(
                    FlightControl.DIRECT,
                    FlyVanilla.BaseSpeed.horizontalSpeed.toDouble(),
                    FlyVanilla.BaseSpeed.verticalSpeed.toDouble(),
                    isFlying = true,
                )
            }

            val abilities = player.abilities
            if (!abilities.mayfly && !abilities.flying) {
                return null
            }

            // Sprinting doubles the flying speed, and the speed settles where drag takes what the keys add.
            val speed = abilities.flyingSpeed.toDouble()
            return FlightAbilities(
                FlightControl.MOMENTUM,
                horizontalSpeed = 2.0 * speed * INPUT_SCALE / (1.0 - HORIZONTAL_DRAG),
                verticalSpeed = speed * VERTICAL_THRUST / (1.0 - VERTICAL_DRAG),
                isFlying = abilities.flying,
            )
        }
    }

}
