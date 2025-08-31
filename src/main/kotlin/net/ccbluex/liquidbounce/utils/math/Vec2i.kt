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

import org.joml.Vector2i
import kotlin.math.sqrt

fun Vector2i.dotProduct(other: Vector2i): Long = dotProduct(other.x, other.y)

fun Vector2i.dotProduct(x: Int, y: Int): Long = this.x.toLong() * x + this.y.toLong() * y

fun Vector2i.similarity(other: Vector2i): Double {
    return this.dotProduct(other) / sqrt((this.lengthSquared() * other.lengthSquared()).toDouble())
}
