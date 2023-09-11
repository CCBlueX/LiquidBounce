/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2023 CCBlueX
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

import net.minecraft.util.math.MathHelper
import net.minecraft.util.math.Vec3d

class Plane(val pos: Vec3d, val normalVec: Vec3d) {
    fun intersection(line: Line): Vec3d? {
        val d = this.pos.dotProduct(this.normalVec)
        val e = line.direction.dotProduct(this.normalVec)

        // If the line is in the plane or parallel to it, there is no intersection point
        if (MathHelper.approximatelyEquals(e, 0.0))
            return null

        val phi = (d - line.position.dotProduct(this.normalVec)) / e

        return line.getPosition(phi)
    }


}
