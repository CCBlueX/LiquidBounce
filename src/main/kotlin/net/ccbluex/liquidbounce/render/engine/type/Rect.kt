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

import org.joml.Vector2f

@JvmRecord
data class Rect(val x1: Float, val y1: Float, val x2: Float, val y2: Float) {

    val cx: Float get() = (x1 + x2) * 0.5F
    val cy: Float get() = (y1 + y2) * 0.5F
    val w: Float get() = x2 - x1
    val h: Float get() = y2 - y1

    val center: Vector2f
        get() = Vector2f(cx, cy)

    init {
        require(x1 <= x2)
        require(y1 <= y2)
    }

    fun contains(px: Float, py: Float): Boolean =
        px in x1..x2 && py in y1..y2

    fun intersects(other: Rect): Boolean =
        !(other.x1 > x2 || other.x2 < x1 || other.y1 > y2 || other.y2 < y1)

    companion object {
        @JvmStatic
        fun of(cx: Float, cy: Float, w: Float, h: Float): Rect {
            return Rect(cx - w * 0.5F, cy - h * 0.5F, cx + w * 0.5F, cy + h * 0.5F)
        }
    }

}
