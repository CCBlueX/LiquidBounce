/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015-2024 CCBlueX
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
package net.ccbluex.liquidbounce.web.theme.type.native.components.minimap

import net.ccbluex.liquidbounce.utils.block.ChunkScanner
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.math.Vec2i
import net.minecraft.block.BlockState
import net.minecraft.block.MapColor.Brightness
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.ChunkPos
import net.minecraft.util.math.MathHelper
import java.awt.Color
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin

object ChunkRenderer {
    private val textureAtlasManager = MinimapTextureAtlasManager()
    private val heightmapManager = MinimapHeightmapManager()

    val SUN_DIRECTION = Vec2i(2, 1)

    fun unloadEverything() {
        heightmapManager.unloadAllChunks()
        textureAtlasManager.deallocateAll()
    }

    fun getAtlasPosition(chunkPos: ChunkPos): MinimapTextureAtlasManager.AtlasPosition {
        return textureAtlasManager.getOrNotLoadedTexture(chunkPos)
    }

    fun prepareRendering(): Int {
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
                        pos.add(1, 0, 0),
                        pos.add(-1, 0, 0),
                        pos.add(0, 0, 1),
                        pos.add(0, 0, -1),
                    )
                } else {
                    arrayOf(pos)
                }

            for (posToUpdate in positionsToUpdate) {
                val color = getColor(posToUpdate)

                textureAtlasManager.editChunk(ChunkPos(posToUpdate)) { texture, atlasPosition ->
                    val (x, y) = atlasPosition.getPosOnAtlas(posToUpdate.x and 15, posToUpdate.z and 15)

                    texture.image!!.setColor(x, y, color)
                }
            }
        }

        private fun getColor(pos: BlockPos): Int {
            val world = mc.world!!

            val height = heightmapManager.getHeight(pos.x, pos.z)
            val offsetsToCheck =
                arrayOf(
                    Vec2i(-1, 0),
                    Vec2i(1, 0),
                    Vec2i(0, -1),
                    Vec2i(0, 1),
                    Vec2i(-1, 1),
                    Vec2i(1, 1),
                    Vec2i(-1, -1),
                    Vec2i(1, -1),
                )

            val higherOffsets =
                offsetsToCheck.filter { offset ->
                    heightmapManager.getHeight(pos.x + offset.x, pos.z + offset.y) > height
                }

            val higherOffsetVec = higherOffsets.fold(Vec2i(0, 0)) { acc, vec -> acc.add(vec) }

            val brightness =
                if (higherOffsets.size < 2) {
                    220 / 255.0
                } else if (MathHelper.approximatelyEquals(higherOffsetVec.length(), 0.0)) {
                    130.0 / 255.0
                } else {
                    val similarityToSunDirection = higherOffsetVec.similarity(SUN_DIRECTION)
                    val eee = higherOffsetVec.dotProduct(Vec2i(pos.x, pos.z)).toDouble() / higherOffsetVec.length()
                    val sine = sin(eee * 0.5 * PI)

                    (190 + (similarityToSunDirection * 55.0) + sine * 10) / 255.0
                }

            val surfaceBlockPos = BlockPos(pos.x, height, pos.z)
            val surfaceBlockState = world.getBlockState(surfaceBlockPos)

            if (surfaceBlockState.isAir) {
                return Color(255, 207, 179).rgb
            }

            val baseColor = surfaceBlockState.getMapColor(world, surfaceBlockPos).getRenderColor(Brightness.HIGH)

            val color = Color(baseColor)

            return Color(
                (color.red * brightness).roundToInt(),
                (color.green * brightness).roundToInt(),
                (color.blue * brightness).roundToInt(),
            ).rgb
        }

        override fun chunkUpdate(
            x: Int,
            z: Int,
        ) {
            val chunkPos = ChunkPos(x, z)

            val chunkBordersToUpdate =
                arrayOf(
                    Triple(ChunkPos(x + 1, z), Vec2i(0, 0), Vec2i(0, 15)),
                    Triple(ChunkPos(x - 1, z), Vec2i(15, 0), Vec2i(15, 15)),
                    Triple(ChunkPos(x, z + 1), Vec2i(0, 0), Vec2i(15, 0)),
                    Triple(ChunkPos(x, z - 1), Vec2i(0, 15), Vec2i(15, 15)),
                )

            heightmapManager.updateChunk(chunkPos)

            textureAtlasManager.editChunk(chunkPos) { texture, atlasPosition ->
                for (offX in 0..15) {
                    for (offZ in 0..15) {
                        val (texX, texY) = atlasPosition.getPosOnAtlas(offX, offZ)

                        val color = getColor(BlockPos(offX + x * 16, 0, offZ + z * 16))

                        texture.image!!.setColor(texX, texY, color)
                    }
                }
            }

            for ((otherPos, from, to) in chunkBordersToUpdate) {
                textureAtlasManager.editChunk(otherPos) { texture, atlasPosition ->
                    for (offX in from.x..to.x) {
                        for (offZ in from.y..to.y) {
                            val (texX, texY) = atlasPosition.getPosOnAtlas(offX, offZ)

                            val color = getColor(BlockPos(offX + otherPos.startX, 0, offZ + otherPos.startZ))

                            texture.image!!.setColor(texX, texY, color)
                        }
                    }
                }
            }
        }

        override fun clearChunk(
            x: Int,
            z: Int,
        ) {
            val chunkPos = ChunkPos(x, z)

            heightmapManager.unloadChunk(chunkPos)
            textureAtlasManager.deallocate(chunkPos)
        }

        override fun clearAllChunks() {
            unloadEverything()
        }
    }
}
