package net.ccbluex.liquidbounce.features.module.modules.render.minimap

import net.ccbluex.liquidbounce.render.engine.Color4b
import net.ccbluex.liquidbounce.render.engine.font.BoundingBox2f
import net.ccbluex.liquidbounce.utils.math.Vec2i
import net.minecraft.client.texture.NativeImage
import net.minecraft.client.texture.NativeImageBackedTexture
import net.minecraft.util.math.ChunkPos

/**
 * Size of the texture atlas in chunks (size x size)
 */
private const val ATLAS_SIZE: Int = 64

/**
 * If we need to upload more than this amount of chunks, we upload the whole texture
 */
private const val FULL_UPLOAD_THRESHOLD: Int = 15

private val NOT_LOADED_ATLAS_POSITION = MinimapTextureAtlasManager.AtlasPosition(0, 0)

class MinimapTextureAtlasManager {
    private val texture = NativeImageBackedTexture(ATLAS_SIZE * 16, ATLAS_SIZE * 16, false)
    private val availableAtlasPositions = mutableListOf<AtlasPosition>()
    private val dirtyAtlasPositions = hashSetOf<AtlasPosition>()
    private val chunkPosAtlasPosMap = hashMapOf<ChunkPos, AtlasPosition>()

    private var allocated = false

    init {
        for (x in 0 until ATLAS_SIZE) {
            for (y in 0 until ATLAS_SIZE) {
                if (x == 0 && y == 0) {
                    continue
                }

                availableAtlasPositions.add(AtlasPosition(x, y))
            }
        }

        for (x in 0..15) {
            for (y in 0..15) {
                val color = if ((x and 1) xor (y and 1) == 0) Color4b.BLACK.toRGBA() else Color4b.WHITE.toRGBA()

                this.texture.image!!.setColor(x, y, color)
            }
        }

        this.dirtyAtlasPositions.add(NOT_LOADED_ATLAS_POSITION)
    }

    private fun allocate(chunkPos: ChunkPos): AtlasPosition {
        val atlasPosition = availableAtlasPositions.removeLastOrNull() ?: error("No more space in the texture atlas!")

        chunkPosAtlasPosMap[chunkPos] = atlasPosition

        return atlasPosition
    }

    fun deallocate(chunkPos: ChunkPos) {
        val atlasPosition = chunkPosAtlasPosMap.remove(chunkPos) ?: return

        availableAtlasPositions.add(atlasPosition)
    }

    fun deallocateAll() {
        availableAtlasPositions.addAll(chunkPosAtlasPosMap.values)
        chunkPosAtlasPosMap.clear()
        dirtyAtlasPositions.clear()
    }

    fun getOrNotLoadedTexture(chunkPos: ChunkPos): AtlasPosition {
        return chunkPosAtlasPosMap[chunkPos] ?: NOT_LOADED_ATLAS_POSITION
    }

    fun get(chunkPos: ChunkPos): AtlasPosition? {
        return chunkPosAtlasPosMap[chunkPos]
    }

    private fun getOrAllocate(chunkPos: ChunkPos): AtlasPosition {
        return get(chunkPos) ?: allocate(chunkPos)
    }

    fun editChunk(
        chunkPos: ChunkPos,
        editor: (NativeImageBackedTexture, AtlasPosition) -> Unit,
    ) {
        val atlasPosition =
            synchronized(this) {
                val atlasPosition = getOrAllocate(chunkPos)

                dirtyAtlasPositions.add(atlasPosition)

                atlasPosition
            }

        editor(texture, atlasPosition)
    }

    /**
     * Uploads texture changes to the GPU
     *
     * @return the GLid of the texture
     */
    fun prepareRendering(): Int {
        this.texture.bindTexture()

        synchronized(this) {
            val dirtyChunks = this.dirtyAtlasPositions.size

            when {
                !this.allocated -> uploadFullTexture()
                dirtyChunks == 0 -> {}
                dirtyChunks < FULL_UPLOAD_THRESHOLD -> uploadOnlyDirtyPositions()
                else -> uploadFullTexture()
            }

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
                    false, false)
            }
        }
    }

    data class AtlasPosition(private val x: Int, private val y: Int) {
        val baseXOnAtlas: Int
            get() = x * 16
        val baseYOnAtlas: Int
            get() = y * 16

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
        fun getPosOnAtlas(
            chunkX: Int,
            chunkY: Int,
        ): Vec2i {
            return Vec2i(baseXOnAtlas + chunkX, baseYOnAtlas + chunkY)
        }
    }
}
