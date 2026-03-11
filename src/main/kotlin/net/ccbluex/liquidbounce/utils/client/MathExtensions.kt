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

@file:Suppress("NOTHING_TO_INLINE", "TooManyFunctions")
package net.ccbluex.liquidbounce.utils.client

import net.ccbluex.liquidbounce.utils.math.geometry.AlignedFace
import net.minecraft.core.Direction
import net.minecraft.util.Mth
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import org.joml.Vector2f
import java.math.RoundingMode

inline fun Float.toRadians() = this * Mth.DEG_TO_RAD
inline fun Double.toRadians() = this * Mth.DEG_TO_RAD
inline fun Float.toDegrees() = this * Mth.RAD_TO_DEG
inline fun Double.toDegrees() = this * Mth.RAD_TO_DEG

inline fun Float.floorToInt() = Mth.floor(this)
inline fun Double.floorToInt() = Mth.floor(this)
inline fun Float.ceilToInt() = Mth.ceil(this)
inline fun Double.ceilToInt() = Mth.ceil(this)

inline fun Float.fastSin() = toDouble().fastSin()
inline fun Double.fastSin() = Mth.sin(this)
inline fun Float.fastCos() = toDouble().fastCos()
inline fun Double.fastCos() = Mth.cos(this)

fun AABB.getFace(direction: Direction): AlignedFace {
    return when (direction) {
        Direction.DOWN -> AlignedFace(
            Vec3(this.minX, this.minY, this.minZ),
            Vec3(this.maxX, this.minY, this.maxZ)
        )

        Direction.UP -> AlignedFace(
            Vec3(this.minX, this.maxY, this.minZ),
            Vec3(this.maxX, this.maxY, this.maxZ)
        )

        Direction.SOUTH -> AlignedFace(
            Vec3(this.minX, this.minY, this.maxZ),
            Vec3(this.maxX, this.maxY, this.maxZ)
        )

        Direction.NORTH -> AlignedFace(
            Vec3(this.minX, this.minY, this.minZ),
            Vec3(this.maxX, this.maxY, this.minZ)
        )

        Direction.EAST -> AlignedFace(
            Vec3(this.maxX, this.minY, this.minZ),
            Vec3(this.maxX, this.maxY, this.maxZ)
        )

        Direction.WEST -> AlignedFace(
            Vec3(this.minX, this.minY, this.minZ),
            Vec3(this.minX, this.maxY, this.maxZ)
        )
    }
}

/**
 * Rounds the given number to the specified decimal place (the first by default).
 * For additional info see [RoundingMode#HALF_UP].
 *
 * For example ```roundToNDecimalPlaces(1234.567,decimalPlaces=1)``` will
 * return ```1234.6```.
 *
 * @see https://stackoverflow.com/a/2808648/9140494
 * @return The rounded value
 */
fun Double.roundToDecimalPlaces(decimalPlaces: Int = 1): Double =
    toBigDecimal().setScale(decimalPlaces, RoundingMode.HALF_UP).toDouble()

fun Float.roundToDecimalPlaces(decimalPlaces: Int = 1): Float =
    toBigDecimal().setScale(decimalPlaces, RoundingMode.HALF_UP).toFloat()

inline infix fun Float.vector2f(other: Float) = Vector2f(this, other)
