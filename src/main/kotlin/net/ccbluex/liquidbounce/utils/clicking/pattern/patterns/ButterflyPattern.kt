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
 * Butterfly clicking is a method that is used to bypass the CPS limit of 20.
 *
 * It will often result in double click (very similar to the double click technique - but randomized).
 */
object ButterflyPattern : ClickPattern {
    override fun fill(
        clickArray: IntArray,
        cps: IntRange,
        clicker: Clicker<*>
    ) {
        val clicks = cps.random()

        while (clickArray.sum() < clicks) {
            // Increase random index inside click array by 1
            val indices = clickArray.indices.filter { clickArray[it] == 0 }

            if (indices.isNotEmpty()) {
                // Increase a random index which is not yet clicked
                indices.random().let { index ->
                    clickArray[index] = Clicker.RNG.nextInt(1, 3)
                }
            } else {
                // Randomly increase an index
                clickArray.indices.random().let { index ->
                    clickArray[index]++
                }
            }
        }
    }
}
