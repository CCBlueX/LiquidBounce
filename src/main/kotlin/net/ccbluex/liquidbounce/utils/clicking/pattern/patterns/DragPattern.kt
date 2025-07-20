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
 * Drag clicking is a method that is used to bypass the CPS limit of 20.
 *
 * It can be done by gliding your finger over the mouse button and causing friction
 * to click very fast.
 *
 * Is not very easy to do as it requires a lot of practice and a good mouse,
 * as well as a good grip on the mouse. Sweaty hands are a big no-no.
 *
 * This is very hard to implement as I am not able to do this method myself,
 * so I will simply guess how it works.
 */
object DragPattern : ClickPattern {
    override fun fill(
        clickArray: IntArray,
        cps: IntRange,
        clicker: Clicker<*>
    ) {
        val clicks = cps.random()

        /**
         * The travel time is the time it takes to move the finger
         * from the top of the mouse to the bottom.
         *
         * After this travel time we need to move the finger back to the top and cannot click.
         * This is more consistent usually.
         */
        val travelTime = Clicker.RNG.nextInt(17, 19)

        // Fit the clicks into the travel time of the
        while (clickArray.sum() < clicks) {
            // Fill the travel time area in the click array with clicks

            // Get index with the lowest clicks on the click array
            val index = clickArray.copyOf(travelTime).indices.minByOrNull { clickArray[it] }!!

            // Increase the click count at the index
            clickArray[index]++
        }
    }
}
