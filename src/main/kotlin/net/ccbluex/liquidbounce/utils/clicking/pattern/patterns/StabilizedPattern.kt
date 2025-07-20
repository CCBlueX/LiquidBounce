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
package net.ccbluex.liquidbounce.utils.clicking.pattern.patterns

import net.ccbluex.liquidbounce.utils.clicking.Clicker
import net.ccbluex.liquidbounce.utils.clicking.pattern.ClickPattern
import kotlin.math.max

/**
 * Normal clicking but with a stabilized click cycle.
 */
object StabilizedPattern : ClickPattern {

    override fun fill(
        clickArray: IntArray,
        cps: IntRange,
        clicker: Clicker<*>
    ) {
        val clicks = cps.random()

        // Calculate the interval and distribute the remainder to spread evenly
        val interval = if (clicks > 0) clickArray.size / clicks else 0
        var remainder = if (clicks > 0) clickArray.size % clicks else 0

        var currentIndex = 0

        repeat(clicks) {
            clickArray[currentIndex % clickArray.size]++
            currentIndex += max(interval, 1)
            if (remainder > 0) {
                currentIndex++
                remainder--
            }
        }
    }

}
