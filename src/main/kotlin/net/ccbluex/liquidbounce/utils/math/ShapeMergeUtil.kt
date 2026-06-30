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

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap
import it.unimi.dsi.fastutil.longs.LongArrayList
import it.unimi.dsi.fastutil.longs.LongOpenHashSet
import net.ccbluex.fastutil.forEachLong
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.phys.shapes.BooleanOp
import net.minecraft.world.phys.shapes.Shapes
import net.minecraft.world.phys.shapes.VoxelShape

@JvmRecord
data class PositionedVoxelShape<K>(
    val blockPos: Long,
    val key: K,
    val shape: VoxelShape,
)

/**
 * Replacement of `ObjectRef<VoxelShape>`
 */
internal class ShapeJoiner {
    var value = Shapes.empty()
        private set

    fun add(shape: VoxelShape) {
        value = Shapes.joinUnoptimized(shape, value, BooleanOp.OR)
    }
}

@Suppress("CognitiveComplexMethod", "LongMethod")
fun <K> Collection<PositionedVoxelShape<K>>.mergeAdjacentVoxelShapes(): List<PositionedVoxelShape<K>> {
    if (this.isEmpty()) return emptyList()

    val groupedShapes = buildMap {
        for ((blockPos, key, shape) in this@mergeAdjacentVoxelShapes) {
            if (shape.isEmpty) continue
            val shapesByPos = this.getOrPut(key, ::Long2ObjectOpenHashMap)
            shapesByPos.put(blockPos, shape)
        }
    }

    if (groupedShapes.isEmpty()) {
        return emptyList()
    }

    val visited = LongOpenHashSet()
    val queue = LongArrayList()
    val componentEntries = LongArrayList()

    val result = ArrayList<PositionedVoxelShape<K>>()
    for ((key, shapesByPos) in groupedShapes) {
        visited.clear()
        queue.clear()

        shapesByPos.keys.forEachLong { startPos ->
            if (!visited.add(startPos)) {
                return@forEachLong
            }

            componentEntries.clear()
            queue.clear()
            queue.add(startPos)
            var queueIndex = 0

            var originLong = startPos

            while (queueIndex < queue.size) {
                val currentPos = queue.getLong(queueIndex++)
                if (!shapesByPos.containsKey(currentPos)) continue

                componentEntries.add(currentPos)

                if (BlockPosAsLongComparator.compare(currentPos, originLong) < 0) {
                    originLong = currentPos
                }

                for (direction in Direction.entries) {
                    val neighborPos = BlockPos.offset(currentPos, direction)
                    if (shapesByPos.containsKey(neighborPos) && visited.add(neighborPos)) {
                        queue.add(neighborPos)
                    }
                }
            }

            val originX = BlockPos.getX(originLong)
            val originY = BlockPos.getY(originLong)
            val originZ = BlockPos.getZ(originLong)

            var mergedShape = Shapes.empty()
            for (i in componentEntries.indices) {
                val componentPos = componentEntries.getLong(i)
                val componentShape = shapesByPos.get(componentPos) ?: continue

                mergedShape = Shapes.joinUnoptimized(
                    mergedShape,
                    componentShape.move(
                        (BlockPos.getX(componentPos) - originX).toDouble(),
                        (BlockPos.getY(componentPos) - originY).toDouble(),
                        (BlockPos.getZ(componentPos) - originZ).toDouble(),
                    ),
                    BooleanOp.OR,
                )
            }

            result += PositionedVoxelShape(
                blockPos = originLong,
                key = key,
                shape = mergedShape.optimize(),
            )
        }
    }

    return result
}
