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

package net.ccbluex.liquidbounce.utils.aiming.utils

import net.ccbluex.liquidbounce.utils.aiming.data.Rotation
import net.ccbluex.liquidbounce.utils.aiming.preference.RotationPreference
import net.ccbluex.liquidbounce.utils.math.isHitByLine
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.math.abs

class RotationFindingKtTest {

    private companion object {
        const val TOLERANCE = 1e-6
    }

    private val preferYawClosestToForward = Comparator<Rotation> { first, second ->
        abs(first.yaw).compareTo(abs(second.yaw))
    }
    private val preferYawClosestToForwardPreference = object : RotationPreference {
        override fun getPreferredSpot(eyesPos: Vec3, range: Double): Vec3 = eyesPos

        override fun getPreferredSpotOnBox(box: AABB, eyesPos: Vec3, range: Double): Vec3 = box.center

        override fun compare(first: Rotation, second: Rotation): Int = preferYawClosestToForward.compare(first, second)
    }

    private val alwaysVisible = VisibilityPredicate { _, _ -> true }
    private val neverVisible = VisibilityPredicate { _, _ -> false }

    @Test
    fun `raytraceBoxes compares all shape boxes before returning`() {
        val result = raytraceBoxes(
            eyes = Vec3.ZERO,
            boxes = listOf(
                AABB(1.0, 0.0, 0.0, 3.0, 1.0, 0.3),
                AABB(0.0, 0.0, 1.0, 0.3, 1.0, 3.0),
            ),
            range = 8.0,
            wallsRange = 8.0,
            visibilityPredicate = alwaysVisible,
            rotationPreference = preferYawClosestToForwardPreference,
        )

        assertNotNull(result)
        assertTrue(abs(result!!.rotation.yaw) < 30f, "Expected later box to win, got yaw=${result.rotation.yaw}")
    }

    @Test
    fun `raytraceBox skips fast path when future target must be matched`() {
        val eyes = Vec3.ZERO
        val targetBox = AABB(0.0, 0.0, 2.0, 2.0, 1.0, 3.0)
        val futureTarget = AABB(0.0, 0.0, 4.0, 0.8, 1.0, 5.0)

        val result = raytraceBox(
            eyes = eyes,
            box = targetBox,
            range = 8.0,
            wallsRange = 8.0,
            visibilityPredicate = alwaysVisible,
            rotationPreference = preferYawClosestToForwardPreference,
            futureTarget = futureTarget,
        )

        assertNotNull(result)
        assertTrue(futureTarget.isHitByLine(eyes, result!!.vec), "Expected ray to intersect future target")
    }

    @Test
    fun `raytraceBox fast path returns hit point on box`() {
        val box = AABB(0.0, 0.0, 2.0, 1.0, 1.0, 3.0)
        val preference = object : RotationPreference {
            override fun getPreferredSpot(eyesPos: Vec3, range: Double): Vec3 = Vec3(1.0, 1.0, 4.0)

            override fun compare(first: Rotation, second: Rotation): Int = 0
        }

        val result = raytraceBox(
            eyes = Vec3.ZERO,
            box = box,
            range = 8.0,
            wallsRange = 8.0,
            visibilityPredicate = alwaysVisible,
            rotationPreference = preference,
        )

        assertNotNull(result)
        assertPointOnBoxSurface(result!!.vec, box)
    }

    @Test
    fun `raytraceBlockSideBoxes compares all shape boxes before returning`() {
        val result = raytraceBlockSideBoxes(
            side = Direction.UP,
            boxes = listOf(
                AABB(1.0, 0.0, 0.0, 3.0, 1.0, 0.3),
                AABB(0.0, 0.0, 1.0, 0.3, 1.0, 3.0),
            ),
            offset = BlockPos.ZERO,
            eyes = Vec3.ZERO,
            rangeSquared = 64.0,
            wallsRangeSquared = 64.0,
            rotationPreference = preferYawClosestToForward,
            visibilityPredicate = alwaysVisible,
        )

        assertNotNull(result)
        assertTrue(abs(result!!.rotation.yaw) < 30f, "Expected later box to win, got yaw=${result.rotation.yaw}")
    }

    @Test
    fun `raytraceBlockSideBoxes rejects visible points beyond normal range`() {
        val result = raytraceBlockSideBoxes(
            side = Direction.UP,
            boxes = listOf(AABB(0.0, 0.0, 3.0, 1.0, 1.0, 4.0)),
            offset = BlockPos.ZERO,
            eyes = Vec3.ZERO,
            rangeSquared = 4.0,
            wallsRangeSquared = 25.0,
            rotationPreference = preferYawClosestToForward,
            visibilityPredicate = alwaysVisible,
        )

        assertNull(result)
    }

    @Test
    fun `raytraceBlockSideBoxes still accepts invisible points within wall range`() {
        val result = raytraceBlockSideBoxes(
            side = Direction.UP,
            boxes = listOf(AABB(0.0, 0.0, 3.0, 1.0, 1.0, 4.0)),
            offset = BlockPos.ZERO,
            eyes = Vec3.ZERO,
            rangeSquared = 4.0,
            wallsRangeSquared = 25.0,
            rotationPreference = preferYawClosestToForward,
            visibilityPredicate = neverVisible,
        )

        assertNotNull(result)
    }

    private fun assertPointOnBoxSurface(point: Vec3, box: AABB, tolerance: Double = TOLERANCE) {
        assertTrue(point.x + tolerance >= box.minX && point.x - tolerance <= box.maxX, "x out of bounds: ${point.x}")
        assertTrue(point.y + tolerance >= box.minY && point.y - tolerance <= box.maxY, "y out of bounds: ${point.y}")
        assertTrue(point.z + tolerance >= box.minZ && point.z - tolerance <= box.maxZ, "z out of bounds: ${point.z}")

        val touchesFace = almostEquals(point.x, box.minX, tolerance) ||
            almostEquals(point.x, box.maxX, tolerance) ||
            almostEquals(point.y, box.minY, tolerance) ||
            almostEquals(point.y, box.maxY, tolerance) ||
            almostEquals(point.z, box.minZ, tolerance) ||
            almostEquals(point.z, box.maxZ, tolerance)

        assertTrue(touchesFace, "Expected point to lie on box surface, got $point")
    }

    private fun almostEquals(a: Double, b: Double, tolerance: Double): Boolean {
        return abs(a - b) <= tolerance
    }

}
