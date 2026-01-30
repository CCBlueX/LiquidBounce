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
package net.ccbluex.liquidbounce.config.types

import it.unimi.dsi.fastutil.objects.ObjectArrayList
import net.ccbluex.liquidbounce.config.gson.stategies.Exclude
import net.ccbluex.liquidbounce.config.types.list.ListValue
import net.ccbluex.liquidbounce.utils.math.CurveUtil
import org.joml.Vector2f

class CurveValue(
    name: String,
    value: MutableList<Vector2f>,
    @Exclude val xAxis: Axis,
    @Exclude val yAxis: Axis,
    @Exclude val tension: Float = DEFAULT_TENSION,
) : ListValue<MutableList<Vector2f>, Vector2f>(
    name,
    value,
    ValueType.CURVE,
    ValueType.VECTOR2_F,
    Vector2f::class.java
) {

    @JvmRecord
    data class Axis(val label: String, val range: ClosedFloatingPointRange<Float>) {
        companion object {
            @JvmStatic
            infix fun String.axis(range: ClosedFloatingPointRange<Float>) = Axis(this, range)
        }
    }

    init {
        require(tension in 0.0..1.0) { "Tension must be in range [0.0, 1.0]" }
        require(value.size >= 2) { "Curve must have at least 2 points" }
        require(value.all { point -> point.x in xAxis.range && point.y in yAxis.range }) {
            "Curve points must be within the given bounds"
        }
    }

    fun transform(x: Float): Float = CurveUtil.transform(get(), x, tension)

    companion object {
        const val DEFAULT_TENSION = 0.4f
    }

    class Builder {
        @JvmField var name: String? = null
        @JvmField var xAxis: Axis? = null
        @JvmField var yAxis: Axis? = null
        @JvmField var tension: Float = DEFAULT_TENSION
        @JvmField var points: MutableList<Vector2f>? = null

        infix fun String.x(range: ClosedFloatingPointRange<Float>): Builder {
            xAxis = Axis(this, range)
            return this@Builder
        }

        infix fun String.y(range: ClosedFloatingPointRange<Float>): Builder {
            yAxis = Axis(this, range)
            return this@Builder
        }

        fun points(vararg values: Vector2f): Builder = apply {
            points = ObjectArrayList.wrap(values)
        }

        fun build(): CurveValue {
            return CurveValue(
                requireNotNull(name) { "Missing name" },
                requireNotNull(points) { "Missing default value" },
                requireNotNull(xAxis) { "Missing xAxis" },
                requireNotNull(yAxis) { "Missing yAxis" },
                tension,
            )
        }
    }

}
