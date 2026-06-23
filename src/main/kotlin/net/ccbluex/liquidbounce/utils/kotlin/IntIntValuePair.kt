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

package net.ccbluex.liquidbounce.utils.kotlin

import net.ccbluex.liquidbounce.utils.math.high32
import net.ccbluex.liquidbounce.utils.math.longFrom32
import net.ccbluex.liquidbounce.utils.math.low32

@JvmInline
value class IntIntValuePair private constructor(private val bits: Long) {
    constructor(left: Int, right: Int): this(longFrom32(left, right))
    inline val left get() = component1()
    inline val right get() = component2()

    operator fun component1(): Int = bits.high32()
    operator fun component2(): Int = bits.low32()
}
