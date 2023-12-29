package net.ccbluex.liquidbounce.features.module.modules.render.minimap

import net.ccbluex.liquidbounce.utils.client.mc
import net.minecraft.block.BlockState
import net.minecraft.block.MapColor
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.ChunkPos

class MinimapHeightmapManager {
    private val heightmaps = hashMapOf<ChunkPos, HeightmapForChunk>()

    fun getHeight(
        x: Int,
        z: Int,
    ): Int {
        val chunkPos = ChunkPos(BlockPos(x, 0, z))
        val heightmap = getHeightmap(chunkPos)

        return heightmap.getHeight(x - chunkPos.startX, z - chunkPos.startZ)
    }

    private fun getHeightmap(chunkPos: ChunkPos): HeightmapForChunk {
        return heightmaps.getOrPut(chunkPos) { HeightmapForChunk() }
    }

    fun updateChunk(chunkPos: ChunkPos) {
        if (mc.world == null) {
            return
        }

        val heightmap = HeightmapForChunk()

        heightmaps[chunkPos] = heightmap

        for (x in 0..15) {
            for (z in 0..15) {
                heightmap.setHeight(x, z, calculateHeight(chunkPos.startX + x, chunkPos.startZ + z))
            }
        }
    }

    /**
     * @return true if the heightmap was changed
     */
    fun updatePosition(
        pos: BlockPos,
        newState: BlockState,
    ): Boolean {
        val chunkPos = ChunkPos(pos)
        val heightmap = getHeightmap(chunkPos)

        val currentHeight = heightmap.getHeight(pos.x - chunkPos.startX, pos.z - chunkPos.startZ)

        val newHeight =
            when {
                currentHeight > pos.y -> {
                    // Do nothing, the change is under the current height
                    return false
                }

                currentHeight == pos.y -> {
                    // The changed block is the world surface. If it is not a surface block anymore,
                    // we need to find a new surface block under it
                    if (isSurface(pos, newState)) {
                        return false
                    }

                    calculateHeight(pos.x, pos.z, maxY = currentHeight)
                }

                currentHeight < pos.y -> {
                    if (!isSurface(pos, newState)) {
                        return false
                    }

                    // If the block is a surface block, and it is above the current height, we know that it must be
                    // the new surface
                    pos.y
                }
                else -> error("Unreachable")
            }

        heightmap.setHeight(pos.x - chunkPos.startX, pos.z - chunkPos.startZ, newHeight)

        return true
    }

    private fun calculateHeight(
        x: Int,
        z: Int,
        maxY: Int? = null,
    ): Int {
        val world = mc.world!!

        val maxHeight = (maxY ?: world.height) - 1

        for (y in maxHeight downTo world.bottomY + 1) {
            val pos = BlockPos(x, y, z)
            val state = world.getBlockState(pos)

            if (isSurface(pos, state)) {
                return y
            }
        }

        return world.bottomY
    }

    private fun isSurface(
        pos: BlockPos,
        blockState: BlockState,
    ): Boolean {
        return !blockState.isAir && blockState.getMapColor(mc.world!!, pos) != MapColor.CLEAR
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

    private fun getPosition(
        x: Int,
        z: Int,
    ): Int {
        return z * 16 + x
    }

    fun getHeight(
        x: Int,
        z: Int,
    ): Int {
        return heightmap[getPosition(x, z)]
    }

    fun setHeight(
        x: Int,
        z: Int,
        height: Int,
    ) {
        heightmap[getPosition(x, z)] = height
    }
}
