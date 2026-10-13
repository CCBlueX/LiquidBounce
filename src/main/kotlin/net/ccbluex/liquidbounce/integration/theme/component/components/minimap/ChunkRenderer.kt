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

import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.utils.block.ChunkScanner
import net.ccbluex.liquidbounce.utils.client.logger
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.math.contains
import net.ccbluex.liquidbounce.utils.math.dotProduct
import net.ccbluex.liquidbounce.utils.math.similarity
import net.ccbluex.liquidbounce.utils.world.getChunk
import net.minecraft.client.gui.render.TextureSetup
import net.minecraft.core.BlockPos
import net.minecraft.util.ARGB
import net.minecraft.util.Mth
import net.minecraft.world.level.ChunkPos
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.chunk.LevelChunk
import net.minecraft.world.level.levelgen.Heightmap
import net.minecraft.world.level.material.MapColor
import net.minecraft.world.level.material.MapColor.Brightness
import org.joml.Vector2i
import org.joml.Vector2ic
import org.joml.component1
import org.joml.component2
import kotlin.math.roundToInt
import kotlin.math.sin

object ChunkRenderer {
    private val textureAtlasManager = MinimapTextureAtlasManager()

    @JvmField
    val SUN_DIRECTION: Vector2ic = Vector2i(2, 1)

    fun unloadEverything() {
        textureAtlasManager.deallocateAll()
    }

    fun getAtlasPosition(chunkPos: Long): MinimapTextureAtlasManager.AtlasPosition {
        return textureAtlasManager.getOrNotLoadedTexture(chunkPos)
    }

    fun prepareRendering(): TextureSetup {
        return textureAtlasManager.prepareRendering()
    }

    object MinimapChunkUpdateSubscriber : ChunkScanner.BlockChangeSubscriber {
        override val shouldCallRecordBlockOnChunkUpdate: Boolean
            get() = false

        override fun recordBlock(
            pos: BlockPos,
            state: BlockState,
            cleared: Boolean,
        ) {
            for (offset in surroundingOffsets) {
                val x = pos.x + offset.x
                val z = pos.z + offset.y
                val chunkPos = ChunkPos(x shr 4, z shr 4)
                val color = getColor(chunkPos, x and 15, z and 15)

                textureAtlasManager.editChunk(chunkPos.pack()) { texture, atlasPosition ->
                    val (texX, texY) = atlasPosition.getPosOnAtlas(x and 15, z and 15)

                    texture.pixels.setPixel(texX, texY, color)
                }
            }
        }

        /**
         * The eight neighbors of a column, excluding the column itself: only they take part in the
         * shading of the column.
         */
        private val neighborOffsets = arrayOf(
            Vector2i(-1, 0),
            Vector2i(1, 0),
            Vector2i(0, -1),
            Vector2i(0, 1),
            Vector2i(-1, 1),
            Vector2i(1, 1),
            Vector2i(-1, -1),
            Vector2i(1, -1),
        )

        /**
         * The column itself and its eight neighbors: a height change in any of them makes the pixel
         * of the column stale.
         */
        private val surroundingOffsets = arrayOf(Vector2i(0, 0)) + neighborOffsets

        private val AIR_COLOR = ARGB.color(179, 207, 255)

        private fun getColor(
            chunkPos: ChunkPos,
            localX: Int,
            localZ: Int,
            knownChunk: LevelChunk? = null,
        ): Int {
            val chunk = knownChunk ?: mc.level?.getChunk(chunkPos) ?: return AIR_COLOR
            val blockX = chunkPos.getBlockX(localX)
            val blockZ = chunkPos.getBlockZ(localZ)

            try {
                // The client keeps the vanilla WORLD_SURFACE heightmap up to date; it names the highest
                // non-air block. Blocks without a map color (glass, rails, ...) are not drawn on a map,
                // so the surface is searched downwards from there.
                val heightmap = chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.WORLD_SURFACE)
                val surfacePos = BlockPos.MutableBlockPos(
                    blockX,
                    heightmap.getHighestTaken(localX, localZ),
                    blockZ,
                )

                while (surfacePos.y >= chunk.minY) {
                    val surfaceBlockState = chunk.getBlockState(surfacePos)

                    if (!surfaceBlockState.isAir && surfaceBlockState.getMapColor(chunk, surfacePos) != MapColor.NONE) {
                        return surfaceColor(chunk, heightmap, surfacePos, surfaceBlockState)
                    }

                    surfacePos.y--
                }

                return AIR_COLOR
            } catch (e: Exception) {
                logger.error("Failed to get color for chunk at $blockX, $blockZ", e)
                return AIR_COLOR
            }
        }

        /**
         * Color of the surface block of a column, shaded by the height of the neighboring columns.
         * Their height is read straight from the vanilla heightmap, which differs from the surface
         * picked above only if a neighbor is topped by a block without a map color.
         */
        private fun surfaceColor(
            chunk: LevelChunk,
            heightmap: Heightmap,
            surfacePos: BlockPos,
            surfaceBlockState: BlockState,
        ): Int {
            val height = surfacePos.y

            val higherOffsets = neighborOffsets.filter { offset ->
                heightAt(chunk, heightmap, surfacePos.x + offset.x, surfacePos.z + offset.y) > height
            }

            val higherOffsetVec = Vector2i(0, 0)
            higherOffsets.forEach { higherOffsetVec.add(it) }

            val brightness =
                if (higherOffsets.size < 2) {
                    220.0 / 255.0
                } else if (higherOffsetVec.x == 0 && higherOffsetVec.y == 0) {
                    130.0 / 255.0
                } else {
                    val similarityToSunDirection = higherOffsetVec.similarity(SUN_DIRECTION)
                    val eee = higherOffsetVec.dotProduct(surfacePos.x, surfacePos.z).toDouble() /
                        higherOffsetVec.length()
                    val sine = sin(eee * Mth.HALF_PI)

                    (190.0 + (similarityToSunDirection * 55.0) + sine * 10.0) / 255.0
                }

            val baseColor = surfaceBlockState.getMapColor(chunk, surfacePos)
                .calculateARGBColor(Brightness.HIGH)

            val color = Color4b(baseColor)

            return Color4b(
                (color.r * brightness).roundToInt(),
                (color.g * brightness).roundToInt(),
                (color.b * brightness).roundToInt(),
            ).argb
        }

        /**
         * Height of a column as named by the vanilla heightmap. Columns within [chunk] reuse its
         * [heightmap]; only the columns across a chunk border are looked up.
         */
        private fun heightAt(chunk: LevelChunk, heightmap: Heightmap, x: Int, z: Int): Int {
            if (chunk.pos.contains(x, z)) {
                return heightmap.getHighestTaken(x and 15, z and 15)
            }

            val level = mc.level ?: return Int.MIN_VALUE

            return level.getChunk(x shr 4, z shr 4).getHeight(Heightmap.Types.WORLD_SURFACE, x and 15, z and 15)
        }

        /**
         * [0] -> (0, 0)
         * [1] -> (0, 15)
         * [2] -> (15, 0)
         * [3] -> (15, 15)
         */
        private val borderOffsets = arrayOf(
            Vector2i(0, 0),
            Vector2i(0, 15),
            Vector2i(15, 0),
            Vector2i(15, 15),
        )

        override fun chunkUpdate(chunk: LevelChunk) {
            val chunkPos = chunk.pos
            val x = chunkPos.x
            val z = chunkPos.z

            val chunkBordersToUpdate =
                arrayOf(
                    Triple(ChunkPos(x + 1, z), borderOffsets[0], borderOffsets[1]),
                    Triple(ChunkPos(x - 1, z), borderOffsets[2], borderOffsets[3]),
                    Triple(ChunkPos(x, z + 1), borderOffsets[0], borderOffsets[2]),
                    Triple(ChunkPos(x, z - 1), borderOffsets[1], borderOffsets[3]),
                )

            textureAtlasManager.editChunk(chunkPos.pack()) { texture, atlasPosition ->
                for (offX in 0..15) {
                    for (offZ in 0..15) {
                        val (texX, texY) = atlasPosition.getPosOnAtlas(offX, offZ)

                        val color = getColor(chunkPos, offX, offZ, chunk)

                        texture.pixels.setPixel(texX, texY, color)
                    }
                }
            }

            for ((otherPos, from, to) in chunkBordersToUpdate) {
                val otherChunk = mc.level?.getChunk(otherPos)

                textureAtlasManager.editChunk(otherPos.pack()) { texture, atlasPosition ->
                    for (offX in from.x..to.x) {
                        for (offZ in from.y..to.y) {
                            val (texX, texY) = atlasPosition.getPosOnAtlas(offX, offZ)

                            val color = getColor(otherPos, offX, offZ, otherChunk)

                            texture.pixels.setPixel(texX, texY, color)
                        }
                    }
                }
            }
        }

        override fun clearChunk(pos: ChunkPos) {
            textureAtlasManager.deallocate(pos.pack())
        }

        override fun clearAllChunks() {
            unloadEverything()
        }
    }
}
