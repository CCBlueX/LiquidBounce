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

package net.ccbluex.liquidbounce.utils.block.targetfinding

import net.ccbluex.liquidbounce.test.assertVec3Equals
import net.ccbluex.liquidbounce.utils.math.geometry.AlignedFace
import net.minecraft.world.phys.Vec3
import kotlin.test.Test
import kotlin.test.assertTrue

class FaceTargetPositionFactoryKtTest {

    private val eps = 1e-9

    @Test
    fun `default scale trims every side to 70 percent`() {
        val face = AlignedFace(Vec3.ZERO, Vec3(2.0, 3.0, 0.0))

        val trimmed = trimFace(face)

        assertVec3Equals(Vec3(0.3, 0.45, 0.0), trimmed.from, eps)
        assertVec3Equals(Vec3(1.7, 2.55, 0.0), trimmed.to, eps)
    }

    @Test
    fun `constant axis stays pinned at its coordinate`() {
        val face = AlignedFace(Vec3(0.0, 0.0, 2.0), Vec3(2.0, 3.0, 2.0))

        val trimmed = trimFace(face)

        assertVec3Equals(Vec3(0.3, 0.45, 2.0), trimmed.from, eps)
        assertVec3Equals(Vec3(1.7, 2.55, 2.0), trimmed.to, eps)
    }

    @Test
    fun `corner order does not matter`() {
        val trimmed = trimFace(AlignedFace(Vec3(2.0, 3.0, 0.0), Vec3.ZERO))

        assertVec3Equals(Vec3(0.3, 0.45, 0.0), trimmed.from, eps)
        assertVec3Equals(Vec3(1.7, 2.55, 0.0), trimmed.to, eps)
    }

    @Test
    fun `scale zero leaves the face untouched`() {
        val face = AlignedFace(Vec3(1.0, 2.0, 3.0), Vec3(3.0, 4.0, 3.0))

        val trimmed = trimFace(face, scale = 0.0)

        assertVec3Equals(face.from, trimmed.from, eps)
        assertVec3Equals(face.to, trimmed.to, eps)
    }

    @Test
    fun `scale above half collapses the face to its center`() {
        val trimmed = trimFace(AlignedFace(Vec3.ZERO, Vec3(2.0, 2.0, 0.0)), scale = 0.6)

        assertVec3Equals(Vec3(1.0, 1.0, 0.0), trimmed.from, eps)
        assertVec3Equals(Vec3(1.0, 1.0, 0.0), trimmed.to, eps)
    }

    @Test
    fun `degenerate point stays pinned`() {
        val point = AlignedFace(Vec3(1.0, 2.0, 3.0), Vec3(1.0, 2.0, 3.0))

        val trimmed = trimFace(point)

        assertVec3Equals(point.from, trimmed.from, eps)
        assertVec3Equals(point.to, trimmed.to, eps)
    }

    @Test
    fun `degenerate line shrinks the varying axis only`() {
        val line = AlignedFace(Vec3(0.0, 1.0, 2.0), Vec3(0.0, 1.0, 5.0))

        val trimmed = trimFace(line)

        assertVec3Equals(Vec3(0.0, 1.0, 2.45), trimmed.from, eps)
        assertVec3Equals(Vec3(0.0, 1.0, 4.55), trimmed.to, eps)
    }

    @Test
    fun `trimmed face stays inside the original face`() {
        val face = AlignedFace(Vec3(-1.0, -2.0, 3.0), Vec3(4.0, 1.0, 3.0))
        val trimmed = trimFace(face)

        assertTrue(trimmed.from.x >= face.from.x && trimmed.from.y >= face.from.y && trimmed.from.z >= face.from.z)
        assertTrue(trimmed.to.x <= face.to.x && trimmed.to.y <= face.to.y && trimmed.to.z <= face.to.z)
    }
}
