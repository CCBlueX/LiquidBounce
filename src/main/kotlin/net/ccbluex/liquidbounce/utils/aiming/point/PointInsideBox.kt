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

package net.ccbluex.liquidbounce.utils.aiming.point

import net.ccbluex.liquidbounce.utils.math.minus
import net.ccbluex.liquidbounce.utils.math.plus
import net.minecraft.util.math.Box
import net.minecraft.util.math.Vec3d

data class PointInsideBox(val pos: Vec3d, val box: Box) {
    init {
        pos.x = pos.x.coerceIn(box.minX, box.maxX)
        pos.y = pos.y.coerceIn(box.minY, box.maxY)
        pos.z = pos.z.coerceIn(box.minZ, box.maxZ)
    }

    fun distanceTo(point: PointInsideBox) = pos.distanceTo(point.pos)

    fun distanceTo(point: Vec3d) = pos.distanceTo(point)

    fun squaredDistanceTo(point: PointInsideBox) = pos.squaredDistanceTo(point.pos)

    fun squaredDistanceTo(point: Vec3d) = pos.squaredDistanceTo(point)

    operator fun plus(other: Vec3d) = PointInsideBox(pos + other, box.offset(other))

    operator fun minus(other: Vec3d) = PointInsideBox(pos - other, box.offset(other.negate()))

    override fun equals(other: Any?) = other is PointInsideBox && pos == other.pos && box == other.box

    override fun hashCode(): Int {
        var result = pos.hashCode()
        result = 31 * result + box.hashCode()
        return result
    }

}
