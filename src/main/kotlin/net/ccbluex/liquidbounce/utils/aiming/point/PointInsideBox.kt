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

package net.ccbluex.liquidbounce.utils.aiming.point

import net.ccbluex.liquidbounce.utils.entity.getNearestPoint
import net.ccbluex.liquidbounce.utils.math.minus
import net.ccbluex.liquidbounce.utils.math.plus
import net.minecraft.core.Position
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3

@ConsistentCopyVisibility
@JvmRecord
data class PointInsideBox private constructor(val pos: Vec3, val box: AABB) : Position {

    fun distanceTo(point: PointInsideBox) = pos.distanceTo(point.pos)

    fun distanceTo(point: Vec3) = pos.distanceTo(point)

    fun squaredDistanceTo(point: PointInsideBox) = pos.distanceToSqr(point.pos)

    fun squaredDistanceTo(point: Vec3) = pos.distanceToSqr(point)

    operator fun plus(other: Position) = Companion(pos + other, box + other)

    operator fun minus(other: Position) = Companion(pos - other, box - other)

    // Delegation
    override fun x(): Double = pos.x()
    override fun y(): Double = pos.y()
    override fun z(): Double = pos.z()

    companion object {
        @JvmStatic
        @JvmName("of")
        operator fun invoke(pos: Vec3, box: AABB) = PointInsideBox(box.getNearestPoint(pos), box)
    }

}
