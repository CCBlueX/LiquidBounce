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
package net.ccbluex.liquidbounce.render.engine.type

import net.ccbluex.liquidbounce.utils.math.high32
import net.ccbluex.liquidbounce.utils.math.longFrom32
import net.ccbluex.liquidbounce.utils.math.low32

@JvmInline
value class UV2f private constructor(private val bits: Long) {
    val u: Float get() = Float.fromBits(bits.high32())
    val v: Float get() = Float.fromBits(bits.low32())

    constructor(u: Float, v: Float) : this(longFrom32(u.toRawBits(), v.toRawBits()))
}
