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

import net.ccbluex.fastutil.objectHashSetOf
import net.minecraft.core.BlockPos
import net.ccbluex.liquidbounce.test.assertIn
import net.ccbluex.liquidbounce.test.assertNotIn
import net.minecraft.core.Direction
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import net.minecraft.world.phys.shapes.Shapes
import net.minecraft.world.phys.shapes.VoxelShape
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ShapeExtensionsTest {

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

    @Test
    fun `forAllFaces returns six faces for full cube`() {
        val faces = Shapes.block().collectFaces()

        assertEquals(6, faces.size)
        assertIn(faces, FaceRect(Direction.DOWN, 0.0, 0.0, 0.0, 1.0, 0.0, 1.0))
        assertIn(faces, FaceRect(Direction.UP, 0.0, 1.0, 0.0, 1.0, 1.0, 1.0))
        assertIn(faces, FaceRect(Direction.NORTH, 0.0, 0.0, 0.0, 1.0, 1.0, 0.0))
        assertIn(faces, FaceRect(Direction.SOUTH, 0.0, 0.0, 1.0, 1.0, 1.0, 1.0))
        assertIn(faces, FaceRect(Direction.WEST, 0.0, 0.0, 0.0, 0.0, 1.0, 1.0))
        assertIn(faces, FaceRect(Direction.EAST, 1.0, 0.0, 0.0, 1.0, 1.0, 1.0))
    }

    @Test
    fun `forAllFaces returns correct slab faces without internal surfaces`() {
        val slab = Shapes.box(0.0, 0.0, 0.0, 1.0, 0.5, 1.0)
        val faces = slab.collectFaces()

        assertEquals(6, faces.size)
        assertIn(faces, FaceRect(Direction.UP, 0.0, 0.5, 0.0, 1.0, 0.5, 1.0))
        assertIn(faces, FaceRect(Direction.DOWN, 0.0, 0.0, 0.0, 1.0, 0.0, 1.0))
        assertIn(faces, FaceRect(Direction.WEST, 0.0, 0.0, 0.0, 0.0, 0.5, 1.0))
        assertIn(faces, FaceRect(Direction.EAST, 1.0, 0.0, 0.0, 1.0, 0.5, 1.0))
    }

    @Test
    fun `forAllFaces merges coplanar stair faces and omits shared interface`() {
        val shape = Shapes.or(
            Shapes.box(0.0, 0.0, 0.0, 1.0, 0.5, 1.0),
            Shapes.box(0.0, 0.5, 0.0, 0.5, 1.0, 1.0),
        )

        val faces = shape.collectFaces()
        val westFaces = faces.filter { it.direction == Direction.WEST }
        val eastFaces = faces.filter { it.direction == Direction.EAST }

        assertEquals(1, westFaces.size)
        assertIn(westFaces, FaceRect(Direction.WEST, 0.0, 0.0, 0.0, 0.0, 1.0, 1.0))

        assertEquals(2, eastFaces.size)
        assertIn(eastFaces, FaceRect(Direction.EAST, 1.0, 0.0, 0.0, 1.0, 0.5, 1.0))
        assertIn(eastFaces, FaceRect(Direction.EAST, 0.5, 0.5, 0.0, 0.5, 1.0, 1.0))

        assertNotIn(faces, FaceRect(Direction.UP, 0.0, 0.5, 0.0, 0.5, 0.5, 1.0))
        assertNotIn(faces, FaceRect(Direction.DOWN, 0.0, 0.5, 0.0, 0.5, 0.5, 1.0))
    }

    @Test
    fun `forAllFaces keeps disconnected coplanar regions separate`() {
        val shape = Shapes.or(
            Shapes.box(0.0, 0.0, 0.0, 0.5, 1.0, 0.4),
            Shapes.box(0.0, 0.0, 0.6, 0.5, 1.0, 1.0),
        )

        val westFaces = shape.collectFaces().filter { it.direction == Direction.WEST }

        assertEquals(2, westFaces.size)
        assertIn(westFaces, FaceRect(Direction.WEST, 0.0, 0.0, 0.0, 0.0, 1.0, 0.4))
        assertIn(westFaces, FaceRect(Direction.WEST, 0.0, 0.0, 0.6, 0.0, 1.0, 1.0))
    }

    @Test
    fun `forAllFaces preserves concave notch faces`() {
        val shape = Shapes.or(
            Shapes.box(0.0, 0.0, 0.0, 1.0, 1.0, 0.5),
            Shapes.box(0.0, 0.0, 0.5, 0.5, 1.0, 1.0),
        )

        val eastFaces = shape.collectFaces().filter { it.direction == Direction.EAST }
        val southFaces = shape.collectFaces().filter { it.direction == Direction.SOUTH }

        assertEquals(2, eastFaces.size)
        assertIn(eastFaces, FaceRect(Direction.EAST, 1.0, 0.0, 0.0, 1.0, 1.0, 0.5))
        assertIn(eastFaces, FaceRect(Direction.EAST, 0.5, 0.0, 0.5, 0.5, 1.0, 1.0))
        assertEquals(2, southFaces.size)
        assertIn(southFaces, FaceRect(Direction.SOUTH, 0.5, 0.0, 0.5, 1.0, 1.0, 0.5))
        assertIn(southFaces, FaceRect(Direction.SOUTH, 0.0, 0.0, 1.0, 0.5, 1.0, 1.0))
    }

    @Test
    fun `forAllSideFaces returns only the hit connected component`() {
        val shape = Shapes.or(
            Shapes.box(0.0, 0.0, 0.0, 0.5, 1.0, 0.4),
            Shapes.box(0.0, 0.0, 0.6, 0.5, 1.0, 1.0),
        )

        val faces = mutableListOf<FaceRect>()
        shape.forAllSideFaces(Direction.WEST, Vec3(0.0, 0.5, 0.2)) { direction, minX, minY, minZ, maxX, maxY, maxZ ->
            faces += FaceRect(direction, minX, minY, minZ, maxX, maxY, maxZ)
        }

        assertEquals(listOf(FaceRect(Direction.WEST, 0.0, 0.0, 0.0, 0.0, 1.0, 0.4)), faces)
    }

    @Test
    fun `forAllSideOutlineEdges returns only component perimeter without internal lines`() {
        val shape = Shapes.or(
            Shapes.box(0.0, 0.0, 0.0, 0.5, 1.0, 0.4),
            Shapes.box(0.0, 0.0, 0.6, 0.5, 1.0, 1.0),
        )

        val lines = objectHashSetOf<Line3>()
        shape.forAllSideOutlineEdges(Direction.WEST, Vec3(0.0, 0.5, 0.2)) { startX, startY, startZ, endX, endY, endZ ->
            lines += Line3(startX, startY, startZ, endX, endY, endZ)
        }

        assertEquals(
            objectHashSetOf(
                Line3(0.0, 0.0, 0.0, 0.0, 0.0, 0.4),
                Line3(0.0, 1.0, 0.0, 0.0, 1.0, 0.4),
                Line3(0.0, 0.0, 0.0, 0.0, 1.0, 0.0),
                Line3(0.0, 0.0, 0.4, 0.0, 1.0, 0.4),
            ),
            lines,
        )
    }

    private fun VoxelShape.collectFaces(): List<FaceRect> {
        val faces = mutableListOf<FaceRect>()
        forAllFaces { direction, minX, minY, minZ, maxX, maxY, maxZ ->
            faces += FaceRect(direction, minX, minY, minZ, maxX, maxY, maxZ)
        }
        return faces
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

    private data class FaceRect(
        val direction: Direction,
        val minX: Double,
        val minY: Double,
        val minZ: Double,
        val maxX: Double,
        val maxY: Double,
        val maxZ: Double,
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is FaceRect) return false

            return direction == other.direction &&
                minX.closeTo(other.minX) &&
                minY.closeTo(other.minY) &&
                minZ.closeTo(other.minZ) &&
                maxX.closeTo(other.maxX) &&
                maxY.closeTo(other.maxY) &&
                maxZ.closeTo(other.maxZ)
        }

        override fun hashCode(): Int = listOf(direction.ordinal, minX, minY, minZ, maxX, maxY, maxZ)
            .joinToString("|") { value -> value.toString() }
            .hashCode()
    }

    private data class Line3(
        val startX: Double,
        val startY: Double,
        val startZ: Double,
        val endX: Double,
        val endY: Double,
        val endZ: Double,
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Line3) return false

            val a = normalized()
            val b = other.normalized()
            return a.first.closeTo(b.first) &&
                a.second.closeTo(b.second) &&
                a.third.closeTo(b.third) &&
                a.fourth.closeTo(b.fourth) &&
                a.fifth.closeTo(b.fifth) &&
                a.sixth.closeTo(b.sixth)
        }

        override fun hashCode(): Int = normalized().toString().hashCode()

        private fun normalized(): LineTuple {
            return if (comesBefore(startX, startY, startZ, endX, endY, endZ)) {
                LineTuple(startX, startY, startZ, endX, endY, endZ)
            } else {
                LineTuple(endX, endY, endZ, startX, startY, startZ)
            }
        }
    }

    private data class LineTuple(
        val first: Double,
        val second: Double,
        val third: Double,
        val fourth: Double,
        val fifth: Double,
        val sixth: Double,
    )

}

private fun comesBefore(
    ax: Double,
    ay: Double,
    az: Double,
    bx: Double,
    by: Double,
    bz: Double,
): Boolean {
    return when {
        !ax.closeTo(bx) -> ax < bx
        !ay.closeTo(by) -> ay < by
        else -> az <= bz
    }
}

private fun Double.closeTo(other: Double): Boolean = kotlin.math.abs(this - other) <= 1.0E-7
