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

/**
 * Normal clicking is the most common clicking method and usually
 * results in a CPS of 5-8 and sometimes when aggressive 10-12.
 *
 *
 *
 * It is when clicking normally with your finger.
 *
 * @note I was not able to press faster than 8 CPS. @1zuna
 */
object SpammingPattern : ClickPattern {
    override fun fill(
        clickArray: IntArray,
        cps: IntRange,
        clicker: Clicker<*>
    ) {
        val clicks = cps.random()

        repeat(clicks) {
            // Increase random index inside click array by 1
            clickArray.indices.random().let { index ->
                clickArray[index]++
            }
        }
    }
}
