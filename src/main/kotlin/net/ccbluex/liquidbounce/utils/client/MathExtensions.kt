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
package net.ccbluex.liquidbounce.utils.client

import net.ccbluex.liquidbounce.utils.math.geometry.AlignedFace
import net.minecraft.util.math.Box
import net.minecraft.util.math.Direction
import net.minecraft.util.math.Vec3d

fun Float.toRadians() = this / 180.0F * Math.PI.toFloat()
fun Float.toDegrees() = this / Math.PI.toFloat() * 180.0F

fun Box.getFace(direction: Direction): AlignedFace {
    return when (direction) {
        Direction.DOWN -> AlignedFace(
            Vec3d(this.minX, this.minY, this.minZ),
            Vec3d(this.maxX, this.minY, this.maxZ)
        )

        Direction.UP -> AlignedFace(
            Vec3d(this.minX, this.maxY, this.minZ),
            Vec3d(this.maxX, this.maxY, this.maxZ)
        )

        Direction.SOUTH -> AlignedFace(
            Vec3d(this.minX, this.minY, this.maxZ),
            Vec3d(this.maxX, this.maxY, this.maxZ)
        )

        Direction.NORTH -> AlignedFace(
            Vec3d(this.minX, this.minY, this.minZ),
            Vec3d(this.maxX, this.maxY, this.minZ)
        )

        Direction.EAST -> AlignedFace(
            Vec3d(this.maxX, this.minY, this.minZ),
            Vec3d(this.maxX, this.maxY, this.maxZ)
        )

        Direction.WEST -> AlignedFace(
            Vec3d(this.minX, this.minY, this.minZ),
            Vec3d(this.minX, this.maxY, this.maxZ)
        )
    }
}

