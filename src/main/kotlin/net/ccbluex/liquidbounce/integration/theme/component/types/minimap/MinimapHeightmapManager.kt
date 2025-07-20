/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2025 CCBlueX
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
 *
 *
 */
package net.ccbluex.liquidbounce.integration.theme.component.types.minimap

import net.ccbluex.liquidbounce.utils.client.logger
import net.ccbluex.liquidbounce.utils.client.mc
import net.minecraft.block.BlockState
import net.minecraft.block.MapColor
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.ChunkPos
import net.minecraft.world.Heightmap
import net.minecraft.world.chunk.Chunk
import java.util.concurrent.ConcurrentHashMap

class MinimapHeightmapManager {
    private val heightmaps = ConcurrentHashMap<ChunkPos, HeightmapForChunk>()

    fun getHeight(x: Int, z: Int): Int {
        val chunkPos = ChunkPos(x shr 4, z shr 4)
        val heightmap = getHeightmap(chunkPos)

        return heightmap.getHeight(x and 15, z and 15)
    }

    private fun getHeightmap(chunkPos: ChunkPos): HeightmapForChunk {
        return heightmaps.getOrPut(chunkPos, ::HeightmapForChunk)
    }

    fun updateChunk(chunkPos: ChunkPos) {
        val chunk = mc.world?.getChunk(chunkPos.x, chunkPos.z) ?: return

        val heightmap = HeightmapForChunk()

        heightmaps[chunkPos] = heightmap

        for (x in 0..15) {
            for (z in 0..15) {
                heightmap.setHeight(x, z, chunk.calculateHeight(x, z))
            }
        }
    }

    /**
     * @return true if the heightmap was changed
     */
    fun updatePosition(pos: BlockPos, newState: BlockState): Boolean {
        val chunkPos = ChunkPos(pos)
        val heightmap = getHeightmap(chunkPos)

        val currentHeight = heightmap.getHeight(pos.x and 15, pos.z and 15)

        val newHeight = mc.world!!.getChunk(chunkPos.x, chunkPos.z)
            .calculateHeightIfNeeded(currentHeight, pos, newState)

        return if (newHeight != null) {
            heightmap.setHeight(pos.x and 15, pos.z and 15, newHeight)

            true
        } else {
            false
        }
    }

    private fun Chunk.calculateHeightIfNeeded(currentHeight: Int, pos: BlockPos, newState: BlockState): Int? {
        return when {
            currentHeight > pos.y -> {
                // Do nothing, the change is under the current height
                null
            }
            currentHeight == pos.y -> {
                // The changed block is the world surface. If it is not a surface block anymore,
                // we need to find a new surface block under it
                if (!isSurface(pos, newState)) {
                    calculateHeight(pos.x, pos.z, maxY = currentHeight)
                } else {
                    null
                }

            }
            else -> {
                if (isSurface(pos, newState)) {
                    // If the block is a surface block, and it is above the current height, we know that it must be
                    // the new surface
                    pos.y
                } else {
                    null
                }
            }
        }
    }

    private fun Chunk.calculateHeight(x: Int, z: Int, maxY: Int? = null): Int {
        val maxHeight = (maxY ?: height) - 1

        val pos = BlockPos.Mutable(x, maxHeight, z)

        try {
            while (pos.y > bottomY) {
                val state = getBlockState(pos)
                if (isSurface(pos, state)) {
                    return pos.y
                }
                pos.y--
            }
        } catch (e: Exception) {
            logger.warn("Exception in height calculation", e)
        }

        return pos.y
    }

    private fun Chunk.isSurface(pos: BlockPos, blockState: BlockState): Boolean {
        return !blockState.isAir && blockState.getMapColor(this, pos) != MapColor.CLEAR
    }

    fun unloadChunk(chunkPos: ChunkPos) {
        this.heightmaps.remove(chunkPos)
    }

    fun unloadAllChunks() {
        heightmaps.clear()
    }
}

class HeightmapForChunk {
    private val heightmap = IntArray(16 * 16) { 255 }

    fun getHeight(
        x: Int,
        z: Int,
    ): Int {
        return heightmap[(z shl 4) or x]
    }

    fun setHeight(
        x: Int,
        z: Int,
        height: Int,
    ) {
        heightmap[(z shl 4) or x] = height
    }
}
