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
package net.ccbluex.liquidbounce.integration.theme.component.components.minimap

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap
import it.unimi.dsi.fastutil.objects.ObjectArrayList
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet
import net.ccbluex.liquidbounce.LiquidBounce.CLIENT_NAME
import net.ccbluex.liquidbounce.render.engine.font.BoundingBox2f
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.utils.render.textureSetup
import net.ccbluex.liquidbounce.utils.render.uploadRect
import net.minecraft.client.gui.render.TextureSetup
import net.minecraft.client.renderer.texture.DynamicTexture
import org.joml.Vector2i
import java.util.concurrent.locks.ReentrantReadWriteLock
import java.util.function.BiConsumer
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
    private val texture = DynamicTexture(
        { "$CLIENT_NAME MinimapTexture" },
        ATLAS_SIZE * 16, ATLAS_SIZE * 16, false
    )

    private val availableAtlasPositions = ObjectArrayList<AtlasPosition>(MAX_ATLAS_POSITIONS).apply {
        for (x in 0 until ATLAS_SIZE) {
            for (y in 0 until ATLAS_SIZE) {
                if (x == 0 && y == 0) {
                    continue
                }

                add(AtlasPosition(x, y))
            }
        }
    }
    private val dirtyAtlasPositions = ObjectOpenHashSet<AtlasPosition>()
    private val chunkPosAtlasPosMap = Long2ObjectOpenHashMap<AtlasPosition>() // key -> ChunkPos

    private val lock = ReentrantReadWriteLock()

    private var allocated = false

    init {
        for (x in 0..15) {
            for (y in 0..15) {
                val color = if ((x and 1) xor (y and 1) == 0) Color4b.BLACK.argb else Color4b.WHITE.argb

                this.texture.pixels!!.setPixel(x, y, color)
            }
        }

        this.dirtyAtlasPositions.add(NOT_LOADED_ATLAS_POSITION)
    }

    private fun allocate(chunkPos: Long): AtlasPosition {
        return lock.write {
            val atlasPosition =
                availableAtlasPositions.removeLastOrNull() ?: error("No more space in the texture atlas!")
            chunkPosAtlasPosMap.put(chunkPos, atlasPosition)
            atlasPosition
        }
    }

    fun deallocate(chunkPos: Long) {
        lock.write {
            chunkPosAtlasPosMap.remove(chunkPos)?.apply(availableAtlasPositions::push)
        }
    }

    fun deallocateAll() {
        lock.write {
            availableAtlasPositions.addAll(chunkPosAtlasPosMap.values)
            chunkPosAtlasPosMap.clear()
            dirtyAtlasPositions.clear()
        }
    }

    fun getOrNotLoadedTexture(chunkPos: Long): AtlasPosition {
        return get(chunkPos) ?: NOT_LOADED_ATLAS_POSITION
    }

    fun get(chunkPos: Long): AtlasPosition? {
        return lock.read { chunkPosAtlasPosMap[chunkPos] }
    }

    private fun getOrAllocate(chunkPos: Long): AtlasPosition {
        return chunkPosAtlasPosMap[chunkPos] ?: allocate(chunkPos)
    }

    fun editChunk(
        chunkPos: Long,
        editor: BiConsumer<DynamicTexture, AtlasPosition>,
    ) {
        val atlasPosition = getOrAllocate(chunkPos)

        lock.write {
            dirtyAtlasPositions.add(atlasPosition)
        }

        editor.accept(texture, atlasPosition)
    }

    /**
     * Uploads texture changes to the GPU
     *
     * @return the [TextureSetup] of the texture
     */
    fun prepareRendering(): TextureSetup {
        lock.read {
            if (this.dirtyAtlasPositions.isEmpty()) {
                return this.texture.textureSetup
            }

            val dirtyChunks = this.dirtyAtlasPositions.size

            when {
                !this.allocated || dirtyChunks >= FULL_UPLOAD_THRESHOLD -> uploadFullTexture()
                else -> uploadOnlyDirtyPositions()
            }
        }

        lock.write {
            this.dirtyAtlasPositions.clear()
        }

        return this.texture.textureSetup
    }

    private fun uploadFullTexture() {
        this.texture.upload()

        this.allocated = true
    }

    private fun uploadOnlyDirtyPositions() {
        for (dirtyAtlasPosition in this.dirtyAtlasPositions) {
            this.texture.uploadRect(
                mipLevel = 0,
                x = dirtyAtlasPosition.baseXOnAtlas,
                y = dirtyAtlasPosition.baseYOnAtlas,
                width = 16, height = 16,
            )
        }
    }

    @JvmRecord
    data class AtlasPosition(private val x: Int, private val y: Int) {
        val baseXOnAtlas: Int get() = x shl 4
        val baseYOnAtlas: Int get() = y shl 4

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
        fun getPosOnAtlas(chunkX: Int, chunkY: Int): Vector2i {
            return Vector2i(baseXOnAtlas or chunkX, baseYOnAtlas or chunkY)
        }
    }
}
