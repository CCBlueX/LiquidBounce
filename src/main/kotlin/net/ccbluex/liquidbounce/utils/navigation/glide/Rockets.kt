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

package net.ccbluex.liquidbounce.utils.navigation.glide

/** The rockets [left] to fire on a glide, each pushing [burnTicks] ticks, the last one fired for [boostTicks] more. */
class Rockets(left: Int, private val burnTicks: Int) {

    var left = left
        private set

    var boostTicks = 0
        private set

    val canFire: Boolean
        get() = boostTicks == 0 && left > 0

    fun fire() {
        left--
        boostTicks = burnTicks
    }

    /** Whether a rocket pushes this tick. */
    fun tick(): Boolean {
        if (boostTicks <= 0) {
            return false
        }

        boostTicks--
        return true
    }

}
