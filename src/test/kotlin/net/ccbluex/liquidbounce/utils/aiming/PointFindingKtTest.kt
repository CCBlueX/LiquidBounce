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

package net.ccbluex.liquidbounce.utils.aiming

import net.minecraft.world.phys.Vec3
import org.joml.Matrix3f
import org.junit.jupiter.api.Test
import kotlin.math.atan2
import kotlin.math.hypot

class PointFindingKtTest {

    @Test
    fun testPlanePointConstruction() {
        val normalVec = Vec3(-1.0, 1.0, -1.0).normalize()

        val hypotenuse = hypot(normalVec.x, normalVec.z)

        val yawAtan = atan2(normalVec.z, normalVec.x).toFloat()
        val pitchAtan = atan2(normalVec.y, hypotenuse).toFloat()

        val initVec = Vec3(1.0, 0.0, 0.0)
        val rotZ = initVec.zRot(-pitchAtan)
        val rotY = rotZ.yRot(-yawAtan)

        val rotMatrix1 = Matrix3f().rotateZ(-pitchAtan)
        val rotMatrix2 = Matrix3f().rotateY(yawAtan)

        val totalMatrix = rotMatrix1.mul(rotMatrix2)

        println(rotY.dot(normalVec))
        println(rotY)
        println(normalVec.toVector3f().mul(totalMatrix))
    }

}
