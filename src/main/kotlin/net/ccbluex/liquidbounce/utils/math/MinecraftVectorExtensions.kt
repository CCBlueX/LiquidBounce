/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2024 CCBlueX
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
@file:Suppress("NOTHING_TO_INLINE")

package net.ccbluex.liquidbounce.utils.math

import net.ccbluex.liquidbounce.render.engine.Vec3
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3d
import net.minecraft.util.math.Vec3i
import kotlin.math.floor

inline operator fun Vec3d.plus(other: Vec3d): Vec3d {
    return this.add(other)
}

inline operator fun Vec3d.minus(other: Vec3d): Vec3d {
    return this.subtract(other)
}

inline operator fun Vec3d.times(scalar: Double): Vec3d {
    return this.multiply(scalar)
}
inline operator fun Vec3d.component1(): Double = this.x
inline operator fun Vec3d.component2(): Double = this.y
inline operator fun Vec3d.component3(): Double = this.z

fun Vec3i.toVec3d(): Vec3d = Vec3d.of(this)
fun Vec3d.toVec3() = Vec3(this.x, this.y, this.z)
fun Vec3d.toVec3i() = Vec3i(this.x.toInt(), this.y.toInt(), this.z.toInt())

fun Vec3d.toBlockPos(): BlockPos {
    val d = floor(this.x).toInt()
    val e = floor(this.y).toInt()
    val f = floor(this.z).toInt()
    return BlockPos(d, e, f)
}
