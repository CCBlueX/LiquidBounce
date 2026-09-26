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
package net.ccbluex.liquidbounce.utils.entity

import net.minecraft.core.BlockPos
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import net.minecraft.world.phys.shapes.Shapes
import kotlin.test.assertEquals
import kotlin.test.Test

class FallingPlayerCollisionTest {

    @Test
    fun `landing beside a slab steps onto it`() {
        val boundingBox = AABB(0.2, 1.2, 0.2, 0.8, 3.0, 0.8)
        val groundedBox = boundingBox.move(0.0, -0.2, 0.0)
        val movement = Vec3(0.6, -0.3, 0.0)
        val directMovement = Vec3(0.2, -0.2, 0.0)
        val colliders = listOf(
            Shapes.create(AABB(-10.0, 0.0, -10.0, 10.0, 1.0, 10.0)),
            Shapes.create(AABB(1.0, 0.0, 0.0, 2.0, 1.5, 1.0)),
        )

        val result = resolveStepUpMovement(
            movement,
            directMovement,
            boundingBox,
            groundedBox,
            maxUpStep = 0.6f,
            colliders,
        )

        assertEquals(0.6, result.x, 1.0E-9)
        assertEquals(0.3, result.y, 1.0E-9)
        assertEquals(0.0, result.z, 1.0E-9)
    }

    @Test
    fun `obstacle above step height keeps direct collision`() {
        val boundingBox = AABB(0.2, 1.2, 0.2, 0.8, 3.0, 0.8)
        val groundedBox = boundingBox.move(0.0, -0.2, 0.0)
        val directMovement = Vec3(0.2, -0.2, 0.0)
        val colliders = listOf(
            Shapes.create(AABB(-10.0, 0.0, -10.0, 10.0, 1.0, 10.0)),
            Shapes.create(AABB(1.0, 0.0, 0.0, 2.0, 2.0, 1.0)),
        )

        val result = resolveStepUpMovement(
            Vec3(0.6, -0.3, 0.0),
            directMovement,
            boundingBox,
            groundedBox,
            maxUpStep = 0.6f,
            colliders,
        )

        assertEquals(directMovement, result)
    }

    @Test
    fun `equidistant supports use vanilla block position tie break`() {
        val lower = BlockPos(0, 0, 0)
        val greater = BlockPos(1, 0, 0)
        val position = Vec3(1.0, 0.5, 0.5)

        assertEquals(greater, selectSupportingBlock(arrayOf(lower, greater).iterator(), position))
        assertEquals(greater, selectSupportingBlock(arrayOf(greater, lower).iterator(), position))
    }

    @Test
    fun `closer support wins before block position ordering`() {
        val closer = BlockPos(0, 0, 0)
        val fartherButGreater = BlockPos(2, 0, 0)

        val result = selectSupportingBlock(
            arrayOf(fartherButGreater, closer).iterator(),
            Vec3(0.5, 0.5, 0.5),
        )

        assertEquals(closer, result)
    }
}
