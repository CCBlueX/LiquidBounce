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

package net.ccbluex.liquidbounce.utils.math

import net.minecraft.core.BlockPos
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.shapes.Shapes
import net.minecraft.world.phys.shapes.VoxelShape
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ShapeMergeUtilTest {

    @Test
    fun `mergeAdjacentVoxelShapes returns empty list for empty input`() {
        assertEquals(
            emptyList<PositionedShapeSpec>(),
            emptyList<PositionedVoxelShape<Int>>().mergeAdjacentVoxelShapes().toSpecs(),
        )
    }

    @Test
    fun `mergeAdjacentVoxelShapes merges adjacent same key regardless of input order`() {
        val merged = listOf(
            positionedShape(1, 0, 0, 1),
            positionedShape(0, 0, 0, 1),
        ).mergeAdjacentVoxelShapes()

        assertEquals(
            listOf(
                PositionedShapeSpec(
                    blockPos = BlockPos.asLong(0, 0, 0),
                    key = 1,
                    boxes = listOf(BoxSpec(0.0, 0.0, 0.0, 2.0, 1.0, 1.0)),
                )
            ),
            merged.toSpecs(),
        )
    }

    @Test
    fun `mergeAdjacentVoxelShapes keeps disconnected components separate`() {
        val merged = listOf(
            positionedShape(0, 0, 0, 1),
            positionedShape(2, 0, 0, 1),
        ).mergeAdjacentVoxelShapes()

        assertEquals(
            listOf(
                PositionedShapeSpec(
                    blockPos = BlockPos.asLong(0, 0, 0),
                    key = 1,
                    boxes = listOf(BoxSpec(0.0, 0.0, 0.0, 1.0, 1.0, 1.0)),
                ),
                PositionedShapeSpec(
                    blockPos = BlockPos.asLong(2, 0, 0),
                    key = 1,
                    boxes = listOf(BoxSpec(0.0, 0.0, 0.0, 1.0, 1.0, 1.0)),
                ),
            ),
            merged.toSpecs(),
        )
    }

    @Test
    fun `mergeAdjacentVoxelShapes does not merge adjacent shapes with different keys`() {
        val merged = listOf(
            positionedShape(0, 0, 0, 1),
            positionedShape(1, 0, 0, 2),
        ).mergeAdjacentVoxelShapes()

        assertEquals(
            listOf(
                PositionedShapeSpec(
                    blockPos = BlockPos.asLong(0, 0, 0),
                    key = 1,
                    boxes = listOf(BoxSpec(0.0, 0.0, 0.0, 1.0, 1.0, 1.0)),
                ),
                PositionedShapeSpec(
                    blockPos = BlockPos.asLong(1, 0, 0),
                    key = 2,
                    boxes = listOf(BoxSpec(0.0, 0.0, 0.0, 1.0, 1.0, 1.0)),
                ),
            ),
            merged.toSpecs(),
        )
    }

    @Test
    fun `mergeAdjacentVoxelShapes preserves partial shape geometry when merging`() {
        val slab = Shapes.box(0.0, 0.0, 0.0, 1.0, 0.5, 1.0)

        val merged = listOf(
            positionedShape(0, 0, 0, 1, slab),
            positionedShape(1, 0, 0, 1, slab),
        ).mergeAdjacentVoxelShapes()

        assertEquals(
            listOf(
                PositionedShapeSpec(
                    blockPos = BlockPos.asLong(0, 0, 0),
                    key = 1,
                    boxes = listOf(BoxSpec(0.0, 0.0, 0.0, 2.0, 0.5, 1.0)),
                )
            ),
            merged.toSpecs(),
        )
    }

    private fun positionedShape(
        x: Int,
        y: Int,
        z: Int,
        key: Int,
        shape: VoxelShape = Shapes.block(),
    ) = PositionedVoxelShape(
        blockPos = BlockPos.asLong(x, y, z),
        key = key,
        shape = shape,
    )

    private fun List<PositionedVoxelShape<Int>>.toSpecs(): List<PositionedShapeSpec> =
        map { shape ->
            PositionedShapeSpec(
                blockPos = shape.blockPos,
                key = shape.key,
                boxes = shape.shape.toAabbs().map(::BoxSpec).sortedBy(BoxSpec::sortKey),
            )
        }.sortedBy(PositionedShapeSpec::sortKey)

    private data class PositionedShapeSpec(
        val blockPos: Long,
        val key: Int,
        val boxes: List<BoxSpec>,
    ) {
        fun sortKey(): String = "$key|$blockPos|$boxes"
    }

    private data class BoxSpec(
        val minX: Double,
        val minY: Double,
        val minZ: Double,
        val maxX: Double,
        val maxY: Double,
        val maxZ: Double,
    ) {
        constructor(box: AABB) : this(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ)

        fun sortKey(): String = "$minX|$minY|$minZ|$maxX|$maxY|$maxZ"

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is BoxSpec) return false

            return minX.closeTo(other.minX) &&
                minY.closeTo(other.minY) &&
                minZ.closeTo(other.minZ) &&
                maxX.closeTo(other.maxX) &&
                maxY.closeTo(other.maxY) &&
                maxZ.closeTo(other.maxZ)
        }

        override fun hashCode(): Int = sortKey().hashCode()
    }

}

private fun Double.closeTo(other: Double): Boolean = kotlin.math.abs(this - other) <= 1.0E-7
