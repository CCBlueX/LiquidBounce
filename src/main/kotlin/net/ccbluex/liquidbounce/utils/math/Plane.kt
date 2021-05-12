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

package net.ccbluex.liquidbounce.utils.math

import net.ccbluex.liquidbounce.features.module.modules.world.ModuleScaffold
import net.minecraft.util.math.Vec3d

class Plane(val base: Vec3d, val v1: Vec3d, val v2: Vec3d) {

    companion object {

        fun fromFaceAndNormal(face: ModuleScaffold.Face, normal: Vec3d): Plane {
            val center = face.center

            val v1 = face.to.subtract(center)

            val v2 = v1.crossProduct(normal).normalize()

            val distVec = v2.multiply(v1.length())

            val b = center.add(distVec)
            val c = center.subtract(distVec)

            return Plane(face.from, b - face.from, c - face.from)
        }

    }

    fun getPoint(phi: Double, lambda: Double): Vec3d {
        return this.base + this.v1 * phi + this.v2 * lambda
    }

}
