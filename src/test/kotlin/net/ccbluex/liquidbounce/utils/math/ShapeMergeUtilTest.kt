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
import net.minecraft.core.Direction
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.shapes.BooleanOp
import net.minecraft.world.phys.shapes.Shapes
import net.minecraft.world.phys.shapes.VoxelShape
import org.junit.jupiter.api.Test
import java.util.BitSet
import kotlin.random.Random

class ShapeMergeUtilTest {

    @Test
    fun `mergeAdjacentVoxelShapes returns empty list for empty input`() {
        assertSamePositionedShapes(
            expected = emptyList(),
            actual = emptyList<PositionedVoxelShape<Int>>().mergeAdjacentVoxelShapes().toShapeSpecs(),
        )
    }

    @Test
    fun `mergeAdjacentVoxelShapes merges adjacent same key regardless of input order`() {
        val merged = listOf(
            positionedShape(1, 0, 0, 1),
            positionedShape(0, 0, 0, 1),
        ).mergeAdjacentVoxelShapes()

        assertSamePositionedShapes(
            expected = listOf(
                PositionedShapeSpec(
                    blockPos = BlockPos.asLong(0, 0, 0),
                    key = 1,
                    boxes = listOf(AABB(0.0, 0.0, 0.0, 2.0, 1.0, 1.0)),
                )
            ),
            actual = merged.toShapeSpecs(),
        )
    }

    @Test
    fun `mergeAdjacentVoxelShapes keeps disconnected components separate`() {
        val merged = listOf(
            positionedShape(0, 0, 0, 1),
            positionedShape(2, 0, 0, 1),
        ).mergeAdjacentVoxelShapes()

        assertSamePositionedShapes(
            expected = listOf(
                PositionedShapeSpec(
                    blockPos = BlockPos.asLong(0, 0, 0),
                    key = 1,
                    boxes = listOf(AABB(0.0, 0.0, 0.0, 1.0, 1.0, 1.0)),
                ),
                PositionedShapeSpec(
                    blockPos = BlockPos.asLong(2, 0, 0),
                    key = 1,
                    boxes = listOf(AABB(0.0, 0.0, 0.0, 1.0, 1.0, 1.0)),
                ),
            ),
            actual = merged.toShapeSpecs(),
        )
    }

    @Test
    fun `mergeAdjacentVoxelShapes does not merge adjacent shapes with different keys`() {
        val merged = listOf(
            positionedShape(0, 0, 0, 1),
            positionedShape(1, 0, 0, 2),
        ).mergeAdjacentVoxelShapes()

        assertSamePositionedShapes(
            expected = listOf(
                PositionedShapeSpec(
                    blockPos = BlockPos.asLong(0, 0, 0),
                    key = 1,
                    boxes = listOf(AABB(0.0, 0.0, 0.0, 1.0, 1.0, 1.0)),
                ),
                PositionedShapeSpec(
                    blockPos = BlockPos.asLong(1, 0, 0),
                    key = 2,
                    boxes = listOf(AABB(0.0, 0.0, 0.0, 1.0, 1.0, 1.0)),
                ),
            ),
            actual = merged.toShapeSpecs(),
        )
    }

    @Test
    fun `mergeAdjacentVoxelShapes preserves partial shape geometry when merging`() {
        val slab = Shapes.box(0.0, 0.0, 0.0, 1.0, 0.5, 1.0)

        val merged = listOf(
            positionedShape(0, 0, 0, 1, slab),
            positionedShape(1, 0, 0, 1, slab),
        ).mergeAdjacentVoxelShapes()

        assertSamePositionedShapes(
            expected = listOf(
                PositionedShapeSpec(
                    blockPos = BlockPos.asLong(0, 0, 0),
                    key = 1,
                    boxes = listOf(AABB(0.0, 0.0, 0.0, 2.0, 0.5, 1.0)),
                )
            ),
            actual = merged.toShapeSpecs(),
        )
    }

    @Test
    fun `mergeAdjacentVoxelShapes merges transitive chains across all axes`() {
        val merged = listOf(
            positionedShape(0, 2, 0, 1),
            positionedShape(0, 1, 0, 1),
            positionedShape(0, 1, 1, 1),
            positionedShape(1, 1, 1, 1),
        ).mergeAdjacentVoxelShapes()

        assertSamePositionedShapes(
            expected = listOf(
                PositionedShapeSpec(
                    blockPos = BlockPos.asLong(0, 1, 0),
                    key = 1,
                    boxes = listOf(
                        AABB(0.0, 0.0, 0.0, 1.0, 1.0, 2.0),
                        AABB(0.0, 1.0, 0.0, 1.0, 2.0, 1.0),
                        AABB(1.0, 0.0, 1.0, 2.0, 1.0, 2.0),
                    ),
                )
            ),
            actual = merged.toShapeSpecs(),
        )
    }

    @Test
    fun `mergeAdjacentVoxelShapes does not merge diagonally touching shapes`() {
        val merged = listOf(
            positionedShape(0, 0, 0, 1),
            positionedShape(1, 1, 0, 1),
        ).mergeAdjacentVoxelShapes()

        assertSamePositionedShapes(
            expected = listOf(
                PositionedShapeSpec(
                    blockPos = BlockPos.asLong(0, 0, 0),
                    key = 1,
                    boxes = listOf(AABB(0.0, 0.0, 0.0, 1.0, 1.0, 1.0)),
                ),
                PositionedShapeSpec(
                    blockPos = BlockPos.asLong(1, 1, 0),
                    key = 1,
                    boxes = listOf(AABB(0.0, 0.0, 0.0, 1.0, 1.0, 1.0)),
                ),
            ),
            actual = merged.toShapeSpecs(),
        )
    }

    @Test
    fun `mergeAdjacentVoxelShapes keeps smallest block position as component origin`() {
        val merged = listOf(
            positionedShape(4, 0, 2, 1),
            positionedShape(4, 0, 1, 1),
            positionedShape(4, 1, 1, 1),
        ).mergeAdjacentVoxelShapes()

        assertSamePositionedShapes(
            expected = listOf(
                PositionedShapeSpec(
                    blockPos = BlockPos.asLong(4, 0, 1),
                    key = 1,
                    boxes = listOf(
                        AABB(0.0, 0.0, 0.0, 1.0, 1.0, 2.0),
                        AABB(0.0, 1.0, 0.0, 1.0, 2.0, 1.0),
                    ),
                )
            ),
            actual = merged.toShapeSpecs(),
        )
    }

    @Test
    fun `mergeAdjacentVoxelShapes ignores empty shapes in input`() {
        val merged = listOf(
            positionedShape(0, 0, 0, 1),
            PositionedVoxelShape(BlockPos.asLong(1, 0, 0), 1, Shapes.empty()),
            positionedShape(2, 0, 0, 1),
        ).mergeAdjacentVoxelShapes()

        assertSamePositionedShapes(
            expected = listOf(
                PositionedShapeSpec(
                    blockPos = BlockPos.asLong(0, 0, 0),
                    key = 1,
                    boxes = listOf(AABB(0.0, 0.0, 0.0, 1.0, 1.0, 1.0)),
                ),
                PositionedShapeSpec(
                    blockPos = BlockPos.asLong(2, 0, 0),
                    key = 1,
                    boxes = listOf(AABB(0.0, 0.0, 0.0, 1.0, 1.0, 1.0)),
                ),
            ),
            actual = merged.toShapeSpecs(),
        )
    }

    @Test
    fun `mergeAdjacentVoxelShapes does not merge shapes that only touch at a corner`() {
        val merged = listOf(
            positionedShape(0, 0, 0, 1),
            positionedShape(1, 0, 1, 1),
        ).mergeAdjacentVoxelShapes()

        assertSamePositionedShapes(
            expected = listOf(
                PositionedShapeSpec(
                    blockPos = BlockPos.asLong(0, 0, 0),
                    key = 1,
                    boxes = listOf(AABB(0.0, 0.0, 0.0, 1.0, 1.0, 1.0)),
                ),
                PositionedShapeSpec(
                    blockPos = BlockPos.asLong(1, 0, 1),
                    key = 1,
                    boxes = listOf(AABB(0.0, 0.0, 0.0, 1.0, 1.0, 1.0)),
                ),
            ),
            actual = merged.toShapeSpecs(),
        )
    }

    @Test
    fun `mergeAdjacentVoxelShapes matches naive merge on random data`() {
        val seeds = intArrayOf(1001, 2002, 3003)

        for (seed in seeds) {
            val input = randomDataset(size = 48, random = Random(seed))
            val fast = input.mergeAdjacentVoxelShapes().toShapeSpecs()
            val naive = naiveMergeAdjacentVoxelShapes(input).toShapeSpecs()
            assertSamePositionedShapes(expected = naive, actual = fast)
        }
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

    private fun randomDataset(size: Int, random: Random): List<PositionedVoxelShape<Int>> {
        val templates = listOf(
            Shapes.block(),
            Shapes.box(0.0, 0.0, 0.0, 1.0, 0.5, 1.0),
            Shapes.box(0.25, 0.0, 0.25, 0.75, 1.0, 0.75),
            Shapes.or(
                Shapes.box(0.0, 0.0, 0.0, 1.0, 0.5, 1.0),
                Shapes.box(0.0, 0.5, 0.0, 0.5, 1.0, 1.0),
            ),
        )

        val entries = ArrayList<PositionedVoxelShape<Int>>(size)
        val occupiedKeys = HashSet<String>(size)
        while (entries.size < size) {
            val x = random.nextInt(-2, 4)
            val y = random.nextInt(0, 3)
            val z = random.nextInt(-2, 4)
            val key = random.nextInt(0, 4)
            val occupancyKey = "$x|$y|$z|$key"
            if (!occupiedKeys.add(occupancyKey)) {
                continue
            }

            entries += PositionedVoxelShape(
                blockPos = BlockPos.asLong(x, y, z),
                key = key,
                shape = templates[random.nextInt(templates.size)],
            )
        }
        return entries
    }

    @Suppress("CognitiveComplexMethod")
    private fun naiveMergeAdjacentVoxelShapes(items: List<PositionedVoxelShape<Int>>): List<PositionedVoxelShape<Int>> {
        if (items.isEmpty()) {
            return emptyList()
        }

        val result = ArrayList<PositionedVoxelShape<Int>>()
        val visited = BitSet(items.size)

        for (startIndex in items.indices) {
            if (visited[startIndex] || items[startIndex].shape.isEmpty) {
                continue
            }

            visited[startIndex] = true
            val key = items[startIndex].key
            val componentIndices = mutableListOf(startIndex)
            val queue = ArrayDeque<Int>()
            queue += startIndex

            while (queue.isNotEmpty()) {
                val currentIndex = queue.removeFirst()
                val current = items[currentIndex]

                for (candidateIndex in items.indices) {
                    if (visited[candidateIndex] || items[candidateIndex].shape.isEmpty) {
                        continue
                    }

                    val candidate = items[candidateIndex]
                    if (candidate.key != key || !areAdjacent(current.blockPos, candidate.blockPos)) {
                        continue
                    }

                    visited[candidateIndex] = true
                    componentIndices += candidateIndex
                    queue += candidateIndex
                }
            }

            val component = componentIndices.map(items::get)
            val originLong = component.minWithOrNull(compareBy(::blockPosY, ::blockPosZ, ::blockPosX))!!.blockPos
            val originX = BlockPos.getX(originLong)
            val originY = BlockPos.getY(originLong)
            val originZ = BlockPos.getZ(originLong)

            var mergedShape = Shapes.empty()
            for (entry in component) {
                mergedShape = Shapes.joinUnoptimized(
                    mergedShape,
                    entry.shape.move(
                        (BlockPos.getX(entry.blockPos) - originX).toDouble(),
                        (BlockPos.getY(entry.blockPos) - originY).toDouble(),
                        (BlockPos.getZ(entry.blockPos) - originZ).toDouble(),
                    ),
                    BooleanOp.OR,
                )
            }

            result += PositionedVoxelShape(originLong, key, mergedShape.optimize())
        }

        return result
    }

    private fun areAdjacent(first: Long, second: Long): Boolean {
        for (direction in Direction.entries) {
            if (BlockPos.offset(first, direction) == second) {
                return true
            }
        }
        return false
    }

    private fun blockPosX(positionedShape: PositionedVoxelShape<Int>) = BlockPos.getX(positionedShape.blockPos)

    private fun blockPosY(positionedShape: PositionedVoxelShape<Int>) = BlockPos.getY(positionedShape.blockPos)

    private fun blockPosZ(positionedShape: PositionedVoxelShape<Int>) = BlockPos.getZ(positionedShape.blockPos)
}
