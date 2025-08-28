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
package net.ccbluex.liquidbounce.config.types

import net.ccbluex.liquidbounce.config.gson.stategies.Exclude

open class CurveValue(
    name: String,
    value: Array<Pair<Float, Float>>,
    @Exclude var xAxis: Axis,
    @Exclude var yAxis: Axis,
    @Exclude var tension: Float = 0.4f,
) : ListValue<MutableList<FloatArray>, FloatArray>(
    name,
    value.map { pair -> floatArrayOf(pair.first, pair.second) }.toMutableList(),
    ValueType.CURVE,
    ValueType.FLOAT_ARRAY,
    FloatArray::class.java
) {

    data class Axis(val label: String, val range: ClosedFloatingPointRange<Float>)

    init {
        require(tension in 0.0..1.0) { "Tension must be in range [0.0, 1.0]" }
        require(value.size >= 2) { "Curve must have at least 2 points" }
        require(value.all { point -> point.first in xAxis.range && point.second in yAxis.range }) {
            "Curve points must be within the given bounds"
        }
    }

}
