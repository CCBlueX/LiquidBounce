package net.ccbluex.liquidbounce.features.module.modules.render.minimap

import net.ccbluex.liquidbounce.utils.block.ChunkScanner
import net.ccbluex.liquidbounce.utils.client.mc
import net.minecraft.block.BlockState
import net.minecraft.block.Blocks
import net.minecraft.block.MapColor
import net.minecraft.client.texture.NativeImage
import net.minecraft.client.texture.NativeImageBackedTexture
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.ChunkPos
import net.minecraft.util.math.Direction
import net.minecraft.world.Heightmap
import net.minecraft.world.World

object ChunkRenderer {
    private val chunkTextures = HashMap<ChunkPos, ChunkTexture>()

    fun deleteAllTextures() {
        lockChunkTextures { textures ->
            textures.values.forEach { it.delete() }
            textures.clear()
        }
    }

    class ChunkTexture {
        var dirtyFlag: Boolean = false
        var wasImageUploaded: Boolean = false

        var front: NativeImage = createNativeImage()
        var back: NativeImage = createNativeImage()

        var renderData: RenderData? = null

        fun delete() {
            this.renderData?.texture?.close()
        }

        fun pushBackToFront() {
            val new = createNativeImage()

            new.copyFrom(this.back)

            this.front = new

            this.dirtyFlag = true
            this.wasImageUploaded = true
        }

        private fun createNativeImage() = NativeImage(NativeImage.Format.RGBA, 16, 16, false)
    }

    class RenderData(val texture: NativeImageBackedTexture)

    object MinimapChunkUpdateSubscriber : ChunkScanner.BlockChangeSubscriber {
        override val shouldCallRecordBlockOnChunkUpdate: Boolean
            get() = false

        override fun recordBlock(
            pos: BlockPos,
            state: BlockState,
            cleared: Boolean,
        ) {
            val color = getColor(pos)

            val texture = lockChunkTextures { textures -> getOrCreateTexture(ChunkPos(pos)) }

            texture.back.setColor(pos.x and 15, pos.z and 15, color)

            texture.pushBackToFront()
        }

        private fun getColor(pos: BlockPos): Int {
            val mutable = BlockPos.Mutable(pos.x, pos.y, pos.z)
            val mutable2 = BlockPos.Mutable(pos.x, pos.y, pos.z)

            val world = mc.world!!

            val chunk = world.getChunk(pos)

            var blockState: BlockState
            var w: Int = chunk.sampleHeightmap(Heightmap.Type.WORLD_SURFACE, mutable.x, mutable.z) + 1
            if (w > world.bottomY + 1) {
                do {
                    mutable.setY(--w)
                } while (chunk.getBlockState(mutable).also { blockState = it }
                        .getMapColor(world, mutable) === MapColor.CLEAR && w > world.bottomY
                )
                if (w > world.bottomY && !blockState.fluidState.isEmpty) {
                    var blockState2: BlockState
                    var y = w - 1
                    mutable2.set(mutable)
                    do {
                        mutable2.setY(y--)
                        blockState2 = chunk.getBlockState(mutable2)
                    } while (y > world.bottomY && !blockState2.fluidState.isEmpty)

                    blockState = getFluidStateIfVisible(world, blockState, mutable)
                }
            } else {
                blockState = Blocks.BEDROCK.defaultState
            }

            return blockState.getMapColor(world, mutable).getRenderColor(MapColor.Brightness.HIGH)
        }

        override fun chunkUpdate(
            x: Int,
            z: Int,
        ) {
            val texture = lockChunkTextures { _ -> getOrCreateTexture(ChunkPos(x, z)) }

            for (offX in 0..15) {
                for (offZ in 0..15) {
                    val color = getColor(BlockPos(offX + x * 16, 0, offZ + z * 16))

                    texture.back.setColor(offX, offZ, color)
                }
            }

            texture.pushBackToFront()
        }

        override fun clearChunk(
            x: Int,
            z: Int,
        ) {
            lockChunkTextures { textures ->
                textures.remove(ChunkPos(x, z))?.delete()
            }
        }

        override fun clearAllChunks() {
            deleteAllTextures()
        }
    }

    private fun getOrCreateTexture(chunkPos: ChunkPos) = chunkTextures.computeIfAbsent(chunkPos) { ChunkTexture() }

    private fun getFluidStateIfVisible(
        world: World,
        state: BlockState,
        pos: BlockPos,
    ): BlockState {
        val fluidState = state.fluidState
        return if (!fluidState.isEmpty && !state.isSideSolidFullSquare(world, pos, Direction.UP)) {
            fluidState.blockState
        } else {
            state
        }
    }

    fun <R> lockChunkTextures(fn: (HashMap<ChunkPos, ChunkTexture>) -> R): R {
        val textures = chunkTextures

        synchronized(textures) {
            return fn(textures)
        }
    }

    fun getOrUploadMinimapChunkTexture(chunkPos: ChunkPos): NativeImageBackedTexture? {
        val chunkTexture = lockChunkTextures { textures -> getOrCreateTexture(chunkPos) }

        if (!chunkTexture.wasImageUploaded) {
            return null
        }

        if (chunkTexture.renderData == null) {
            chunkTexture.renderData =
                RenderData(
                    NativeImageBackedTexture(chunkTexture.front),
                )

            chunkTexture.dirtyFlag = false
        }
        val renderData = chunkTexture.renderData!!

        val texture = renderData.texture

        if (chunkTexture.dirtyFlag) {
            texture.image = chunkTexture.front
            texture.upload()

            chunkTexture.dirtyFlag = false
        }

        return texture
    }
}
