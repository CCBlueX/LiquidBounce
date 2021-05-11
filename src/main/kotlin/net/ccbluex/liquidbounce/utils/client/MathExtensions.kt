/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2016 - 2021 CCBlueX
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

package net.ccbluex.liquidbounce.utils.extensions

import net.ccbluex.liquidbounce.features.module.modules.world.ModuleScaffold
import net.minecraft.util.math.Box
import net.minecraft.util.math.Direction
import net.minecraft.util.math.Vec3d

fun Float.toRadians() = this / 180.0F * Math.PI.toFloat()

fun Box.getFace(direction: Direction): ModuleScaffold.Face {
    return when (direction) {
        Direction.DOWN -> ModuleScaffold.Face(
            Vec3d(this.minX, this.minY, this.minZ),
            Vec3d(this.maxX, this.minY, this.maxZ)
        )
        Direction.UP -> ModuleScaffold.Face(
            Vec3d(this.minX, this.maxY, this.minZ),
            Vec3d(this.maxX, this.maxY, this.maxZ)
        )
        Direction.SOUTH -> ModuleScaffold.Face(
            Vec3d(this.minX, this.minY, this.maxZ),
            Vec3d(this.maxX, this.maxY, this.maxZ)
        )
        Direction.NORTH -> ModuleScaffold.Face(
            Vec3d(this.minX, this.minY, this.minZ),
            Vec3d(this.maxX, this.maxY, this.minZ)
        )
        Direction.EAST -> ModuleScaffold.Face(
            Vec3d(this.maxX, this.minY, this.minZ),
            Vec3d(this.maxX, this.maxY, this.maxZ)
        )
        Direction.WEST -> ModuleScaffold.Face(
            Vec3d(this.minX, this.minY, this.minZ),
            Vec3d(this.minX, this.maxY, this.maxZ)
        )
    }
}
