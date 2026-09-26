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

import net.minecraft.world.phys.Vec2
import java.awt.geom.Rectangle2D

@JvmRecord
data class BoundingBox2f(val xMin: Float, val yMin: Float, val xMax: Float, val yMax: Float) {
    constructor(rect: Rectangle2D) : this(
        rect.minX.toFloat(),
        rect.minY.toFloat(),
        rect.maxX.toFloat(),
        rect.maxY.toFloat()
    )

    init {
        require(xMin <= xMax) {
            "xMin must be less than or equal to xMax (xMin=$xMin, xMax=$xMax, yMin=$yMin, yMax=$yMax)"
        }
        require(yMin <= yMax) {
            "yMin must be less than or equal to yMax (xMin=$xMin, xMax=$xMax, yMin=$yMin, yMax=$yMax)"
        }
    }

    fun contains(x: Float, y: Float): Boolean {
        return x in xMin..xMax && y in yMin..yMax
    }

    val width: Float
        get() = xMax - xMin

    val height: Float
        get() = yMax - yMin

    val xCenter: Float
        get() = xMin + width * 0.5F

    val yCenter: Float
        get() = yMin + height * 0.5F

    val centerVec: Vec2
        get() = Vec2(xCenter, yCenter)

    infix fun intersects(other: BoundingBox2f): Boolean {
        return xMin < other.xMax && xMax > other.xMin && yMin < other.yMax && yMax > other.yMin
    }

    fun offset(xOffset: Float, yOffset: Float): BoundingBox2f {
        return BoundingBox2f(xMin + xOffset, yMin + yOffset, xMax + xOffset, yMax + yOffset)
    }

    companion object {
        @JvmField
        val EMPTY = BoundingBox2f(0f, 0f, 0f, 0f)
    }
}
