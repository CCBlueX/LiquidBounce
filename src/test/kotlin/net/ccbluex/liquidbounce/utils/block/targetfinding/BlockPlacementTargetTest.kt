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

import net.ccbluex.liquidbounce.utils.aiming.data.Rotation
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.Vec3
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BlockPlacementTargetTest {

    private val interactedBlockPos = BlockPos(4, 5, 6)
    private val placementTarget = BlockPlacementTarget(
        interactedBlockPos,
        BlockPos(4, 4, 6),
        Direction.UP,
        Vec3(4.5, 6.0, 6.5),
        minPlacementY = 6.0,
        Rotation(0f, 0f),
    )

    private fun hit(location: Vec3, direction: Direction = Direction.UP, pos: BlockPos = interactedBlockPos) =
        BlockHitResult(location, direction, pos, false)

    @Test
    fun `accepts a hit on the targeted face`() {
        assertTrue(placementTarget.doesCrosshairTargetMatchRequirements(hit(Vec3(4.5, 6.0, 6.5))))
    }

    @Test
    fun `accepts a hit rounding below the box boundary`() {
        val roundedDown = placementTarget.minPlacementY - 1.0E-9

        assertTrue(placementTarget.doesCrosshairTargetMatchRequirements(hit(Vec3(4.5, roundedDown, 6.5))))
    }

    @Test
    fun `rejects a hit below the sampled shape box`() {
        assertFalse(placementTarget.doesCrosshairTargetMatchRequirements(hit(Vec3(4.5, 5.4, 6.5))))
    }

    @Test
    fun `rejects a hit on another block or face`() {
        assertFalse(
            placementTarget.doesCrosshairTargetMatchRequirements(
                hit(Vec3(4.5, 6.0, 6.5), pos = BlockPos(4, 6, 6))
            )
        )
        assertFalse(
            placementTarget.doesCrosshairTargetMatchRequirements(
                hit(Vec3(4.5, 6.0, 6.5), direction = Direction.NORTH)
            )
        )
    }
}
