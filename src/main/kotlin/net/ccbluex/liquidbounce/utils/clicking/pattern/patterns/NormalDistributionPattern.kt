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
import net.ccbluex.liquidbounce.utils.clicking.Clicker.Companion.RNG
import net.ccbluex.liquidbounce.utils.clicking.pattern.ClickPattern

/**
 * Normal distribution clicking pattern.
 */
object NormalDistributionPattern : ClickPattern {

    override fun fill(
        clickArray: IntArray,
        cps: IntRange,
        clicker: Clicker<*>
    ) {
        data class Band(val top: Double, val mean: Double, val std: Double)

        val frequencyBands = arrayOf(
            Band(10.0 / 110.0, 179.5242718446602, 20.416937885616676),
            Band(0.0, 87.88, 13.420088130563776)
        )

        var t = 0.0

        while (true) {
            val v = RNG.nextDouble()

            val band = frequencyBands.first { v >= it.top }

            t += RNG.nextGaussian(band.mean, band.std) * 20.0 / 1000.0

            // Second is over
            if (t > 20.0) {
                break
            }

            clickArray[t.toInt()]++
        }
    }

}
