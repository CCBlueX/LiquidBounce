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

import net.ccbluex.liquidbounce.utils.block.ChunkScanner
import net.ccbluex.liquidbounce.utils.client.logger
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.math.dotProduct
import net.ccbluex.liquidbounce.utils.math.similarity
import net.minecraft.client.gui.render.TextureSetup
import net.minecraft.core.BlockPos
import net.minecraft.world.level.ChunkPos
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.chunk.LevelChunk
import net.minecraft.world.level.material.MapColor.Brightness
import org.joml.Vector2i
import org.joml.Vector2ic
import org.joml.component1
import org.joml.component2
import java.awt.Color
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin

object ChunkRenderer {
    private val textureAtlasManager = MinimapTextureAtlasManager()
    private val heightmapManager = MinimapHeightmapManager()

    @JvmField
    val SUN_DIRECTION: Vector2ic = Vector2i(2, 1)

    fun unloadEverything() {
        heightmapManager.unloadAllChunks()
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
            val heightmapUpdated = heightmapManager.updatePosition(pos, state)

            val positionsToUpdate =
                if (heightmapUpdated) {
                    arrayOf(
                        pos,
                        pos.offset(1, 0, 0),
                        pos.offset(-1, 0, 0),
                        pos.offset(0, 0, 1),
                        pos.offset(0, 0, -1),
                    )
                } else {
                    arrayOf(pos)
                }

            for (posToUpdate in positionsToUpdate) {
                val color = getColor(posToUpdate.x, posToUpdate.z)

                textureAtlasManager.editChunk(ChunkPos.asLong(posToUpdate)) { texture, atlasPosition ->
                    val (x, y) = atlasPosition.getPosOnAtlas(posToUpdate.x and 15, posToUpdate.z and 15)

                    texture.pixels!!.setPixel(x, y, color)
                }
            }
        }

        private val offsetsToCheck = arrayOf(
            Vector2i(-1, 0),
            Vector2i(1, 0),
            Vector2i(0, -1),
            Vector2i(0, 1),
            Vector2i(-1, 1),
            Vector2i(1, 1),
            Vector2i(-1, -1),
            Vector2i(1, -1),
        )

        private val AIR_COLOR = Color(179, 207, 255).rgb

        private fun getColor(x: Int, z: Int): Int {
            try {
                val chunk = mc.level?.getChunk(x shr 4, z shr 4) ?: return AIR_COLOR

                val height = heightmapManager.getHeight(x, z)

                val higherOffsets = offsetsToCheck.filter { offset ->
                    heightmapManager.getHeight(x + offset.x, z + offset.y) > height
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
                        val eee = higherOffsetVec.dotProduct(x, z).toDouble() / higherOffsetVec.length()
                        val sine = sin(eee * 0.5 * PI)

                        (190.0 + (similarityToSunDirection * 55.0) + sine * 10.0) / 255.0
                    }

                val surfaceBlockPos = BlockPos(x, height, z)
                val surfaceBlockState = chunk.getBlockState(surfaceBlockPos)

                if (surfaceBlockState.isAir) {
                    return AIR_COLOR
                }

                val baseColor = surfaceBlockState.getMapColor(chunk, surfaceBlockPos)
                    .calculateARGBColor(Brightness.HIGH)

                val color = Color(baseColor)

                return Color(
                    (color.red * brightness).roundToInt(),
                    (color.green * brightness).roundToInt(),
                    (color.blue * brightness).roundToInt(),
                ).rgb
            } catch (e: Exception) {
                logger.error("Failed to get color for chunk at $x, $z", e)
                return AIR_COLOR
            }
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

            heightmapManager.updateChunk(chunkPos)

            textureAtlasManager.editChunk(chunkPos.toLong()) { texture, atlasPosition ->
                for (offX in 0..15) {
                    for (offZ in 0..15) {
                        val (texX, texY) = atlasPosition.getPosOnAtlas(offX, offZ)

                        val color = getColor(offX or (x shl 4), offZ or (z shl 4))

                        texture.pixels!!.setPixel(texX, texY, color)
                    }
                }
            }

            for ((otherPos, from, to) in chunkBordersToUpdate) {
                textureAtlasManager.editChunk(otherPos.toLong()) { texture, atlasPosition ->
                    for (offX in from.x..to.x) {
                        for (offZ in from.y..to.y) {
                            val (texX, texY) = atlasPosition.getPosOnAtlas(offX, offZ)

                            val color = getColor(offX or otherPos.minBlockX, offZ or otherPos.minBlockZ)

                            texture.pixels!!.setPixel(texX, texY, color)
                        }
                    }
                }
            }
        }

        override fun clearChunk(pos: ChunkPos) {
            heightmapManager.unloadChunk(pos)
            textureAtlasManager.deallocate(pos.toLong())
        }

        override fun clearAllChunks() {
            unloadEverything()
        }
    }
}
