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

import net.ccbluex.liquidbounce.render.engine.font.BoundingBox2f
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.utils.math.Vec2i
import net.minecraft.client.texture.NativeImage
import net.minecraft.client.texture.NativeImageBackedTexture
import net.minecraft.util.math.ChunkPos
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

/**
 * Size of the texture atlas in chunks (size x size)
 */
private const val ATLAS_SIZE: Int = 64

/**
 * If we need to upload more than this amount of chunks, we upload the whole texture
 */
private const val FULL_UPLOAD_THRESHOLD: Int = 15

private const val MAX_ATLAS_POSITIONS: Int = ATLAS_SIZE * ATLAS_SIZE - 1

private val NOT_LOADED_ATLAS_POSITION = MinimapTextureAtlasManager.AtlasPosition(0, 0)

class MinimapTextureAtlasManager {
    private val texture = NativeImageBackedTexture(ATLAS_SIZE * 16, ATLAS_SIZE * 16, false)
    private val availableAtlasPositions: ArrayBlockingQueue<AtlasPosition>
    private val dirtyAtlasPositions = hashSetOf<AtlasPosition>()
    private val chunkPosAtlasPosMap = hashMapOf<ChunkPos, AtlasPosition>()

    private val lock = ReentrantReadWriteLock()

    private var allocated = false

    init {
        val atlasPositions = ArrayList<AtlasPosition>(MAX_ATLAS_POSITIONS)
        for (x in 0 until ATLAS_SIZE) {
            for (y in 0 until ATLAS_SIZE) {
                if (x == 0 && y == 0) {
                    continue
                }

                atlasPositions.add(AtlasPosition(x, y))
            }
        }
        availableAtlasPositions = ArrayBlockingQueue(MAX_ATLAS_POSITIONS, false, atlasPositions)

        for (x in 0..15) {
            for (y in 0..15) {
                val color = if ((x and 1) xor (y and 1) == 0) Color4b.BLACK.toARGB() else Color4b.WHITE.toARGB()

                this.texture.image!!.setColorArgb(x, y, color)
            }
        }

        this.dirtyAtlasPositions.add(NOT_LOADED_ATLAS_POSITION)
    }

    private fun allocate(chunkPos: ChunkPos): AtlasPosition {
        val atlasPosition = availableAtlasPositions.take() ?: error("No more space in the texture atlas!")

        lock.write {
            chunkPosAtlasPosMap[chunkPos] = atlasPosition
        }

        return atlasPosition
    }

    fun deallocate(chunkPos: ChunkPos) {
        lock.write {
            chunkPosAtlasPosMap.remove(chunkPos)?.apply(availableAtlasPositions::add)
        }
    }

    fun deallocateAll() {
        lock.write {
            availableAtlasPositions.addAll(chunkPosAtlasPosMap.values)
            chunkPosAtlasPosMap.clear()
            dirtyAtlasPositions.clear()
        }
    }

    fun getOrNotLoadedTexture(chunkPos: ChunkPos): AtlasPosition {
        return get(chunkPos) ?: NOT_LOADED_ATLAS_POSITION
    }

    fun get(chunkPos: ChunkPos): AtlasPosition? {
        return lock.read { chunkPosAtlasPosMap[chunkPos] }
    }

    private fun getOrAllocate(chunkPos: ChunkPos): AtlasPosition {
        return chunkPosAtlasPosMap[chunkPos] ?: allocate(chunkPos)
    }

    fun editChunk(
        chunkPos: ChunkPos,
        editor: (NativeImageBackedTexture, AtlasPosition) -> Unit,
    ) {
        val atlasPosition = getOrAllocate(chunkPos)

        lock.write {
            dirtyAtlasPositions.add(atlasPosition)
        }

        editor(texture, atlasPosition)
    }

    /**
     * Uploads texture changes to the GPU
     *
     * @return the GLid of the texture
     */
    fun prepareRendering(): Int {
        lock.read {
            if (this.dirtyAtlasPositions.isEmpty()) {
                return this.texture.glId
            }

            this.texture.bindTexture()

            val dirtyChunks = this.dirtyAtlasPositions.size

            when {
                !this.allocated || dirtyChunks >= FULL_UPLOAD_THRESHOLD -> uploadFullTexture()
                else -> uploadOnlyDirtyPositions()
            }
        }

        lock.write {
            this.dirtyAtlasPositions.clear()
        }

        return this.texture.glId
    }

    private fun uploadFullTexture() {
        this.texture.upload()

        this.allocated = true
    }

    private fun uploadOnlyDirtyPositions() {
        val image = this.texture.image!!

        for (dirtyAtlasPosition in this.dirtyAtlasPositions) {
            val chunkImage = NativeImage(16, 16, false)

            chunkImage.use {
                image.copyRect(
                    chunkImage,
                    dirtyAtlasPosition.baseXOnAtlas, dirtyAtlasPosition.baseYOnAtlas,
                    0, 0,
                    16, 16,
                    false, false
                )

                chunkImage.upload(
                    0,
                    dirtyAtlasPosition.baseXOnAtlas, dirtyAtlasPosition.baseYOnAtlas,
                    0, 0,
                    16, 16,
                    false
                )
            }
        }
    }

    data class AtlasPosition(private val x: Int, private val y: Int) {
        val baseXOnAtlas: Int = x shl 4
        val baseYOnAtlas: Int = y shl 4

        val uv: BoundingBox2f
            get() {
                val pixelSize = ATLAS_SIZE * 16.0F

                return BoundingBox2f(
                    baseXOnAtlas / pixelSize,
                    baseYOnAtlas / pixelSize,
                    (baseXOnAtlas + 16.0f) / pixelSize,
                    (baseYOnAtlas + 16.0f) / pixelSize,
                )
            }

        /**
         * @param chunkX x coordinate in the chunk (0-15)
         * @param chunkY y coordinate in the chunk (0-15)
         */
        fun getPosOnAtlas(chunkX: Int, chunkY: Int): Vec2i {
            return Vec2i(baseXOnAtlas or chunkX, baseYOnAtlas or chunkY)
        }
    }
}
