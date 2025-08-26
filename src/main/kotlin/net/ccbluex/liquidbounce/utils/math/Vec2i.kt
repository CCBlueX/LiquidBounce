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
package net.ccbluex.liquidbounce.utils.math

import kotlin.math.sqrt

open class Vec2i(val x: Int, val y: Int) {
    operator fun component1() = x
    operator fun component2() = y

    fun add(vec: Vec2i): Vec2i {
        return Vec2i(this.x + vec.x, this.y + vec.y)
    }

    fun dotProduct(otherVec: Vec2i): Int {
        return this.x * otherVec.x + this.y * otherVec.y
    }

    fun similarity(otherVec: Vec2i): Double {
        return this.dotProduct(otherVec) / sqrt(this.lengthSquared().toDouble() * otherVec.lengthSquared())
    }

    fun length(): Double {
        return sqrt(lengthSquared().toDouble())
    }

    private fun lengthSquared(): Int {
        return this.x * this.x + this.y * this.y
    }

    companion object {
        @JvmStatic
        val ZERO = Vec2i(0, 0)
    }
}
