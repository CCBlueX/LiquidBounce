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
 * Double-clicking is NOT a method but a button on a few cheater mouses.
 * This button is called the FIRE button and will result in two clicks when pressed once.
 *
 * This is a method that is not allowed on most servers and is considered cheating.
 * Unlikely to bypass and will result in twice the CPS (!!!).
 *
 * @note In the past I had a mouse with this feature and I always used it. @1zuna
 */
object DoubleClickPattern : ClickPattern {
    override fun fill(
        clickArray: IntArray,
        cps: IntRange,
        clicker: Clicker<*>
    ) {
        val clicks = cps.random()

        repeat(clicks) {
            // Increase random index inside click array by 1
            clickArray.indices.random().let { index ->
                clickArray[index] += 2
            }
        }
    }
}
