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
package net.ccbluex.liquidbounce.utils.block.hole

import it.unimi.dsi.fastutil.longs.Long2ByteOpenHashMap
import net.ccbluex.fastutil.referenceHashSetOf
import net.ccbluex.liquidbounce.features.module.MinecraftShortcuts
import net.ccbluex.liquidbounce.utils.block.ChunkScanner
import net.ccbluex.liquidbounce.utils.block.DIRECTIONS_EXCLUDING_UP
import net.ccbluex.liquidbounce.utils.block.getState
import net.ccbluex.liquidbounce.utils.kotlin.getValue
import net.ccbluex.liquidbounce.utils.math.expendToBlockBox
import net.ccbluex.liquidbounce.utils.math.iterate
import net.ccbluex.liquidbounce.utils.math.iterator
import net.ccbluex.liquidbounce.utils.math.size
import net.ccbluex.liquidbounce.utils.math.toBlockBox
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.world.level.ChunkPos
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.chunk.LevelChunk
import net.minecraft.world.level.levelgen.structure.BoundingBox
import java.util.concurrent.ConcurrentSkipListSet

private const val INDESTRUCTIBLE = (-2).toByte()
private const val BLAST_RESISTANT = (-1).toByte()
private const val AIR = 0.toByte()
private const val BREAKABLE = 1.toByte()

// BlockState types
private typealias State = Byte
private typealias BlockStateBuffer = Long2ByteOpenHashMap

object HoleTracker : ChunkScanner.BlockChangeSubscriber, MinecraftShortcuts {

    val holes = ConcurrentSkipListSet<Hole>()
    private val mutable by ThreadLocal.withInitial(BlockPos::MutableBlockPos)
    private val BLAST_RESISTANT_BLOCKS: Set<Block> by lazy {
        BuiltInRegistries.BLOCK.filterTo(referenceHashSetOf()) {
            it.explosionResistance >= 600 && it.explosionResistance < 3_600_000
        }
    }

    private val INDESTRUCTIBLE_BLOCKS: Set<Block> by lazy {
        BuiltInRegistries.BLOCK.filterTo(referenceHashSetOf()) {
            it.explosionResistance >= 3_600_000
        }
    }

    override val shouldCallRecordBlockOnChunkUpdate: Boolean
        get() = false

    override fun recordBlock(pos: BlockPos, state: BlockState, cleared: Boolean) {
        // Invalidate old ones
        if (state.isAir) {
            // if one of the neighbor blocks becomes air, invalidate the hole
            holes.removeIf { it.positions.iterate().any { p -> p.distManhattan(pos) == 1 } }
        } else {
            holes.removeIf { it.blockInvalidators.isInside(pos) }
        }

        // Check new ones
        val region = pos.expendToBlockBox(2, 3, 2)
        invalidate(region)
        region.cachedUpdate()
    }

    private fun invalidate(region: BoundingBox) {
        holes.removeIf { it.positions.intersects(region) }
    }

    @Suppress("CognitiveComplexMethod", "LongMethod", "LoopWithTooManyJumpStatements")
    fun BoundingBox.cachedUpdate() {
        val buffer = BlockStateBuffer(size)

        val mutableLocal = BlockPos.MutableBlockPos()

        val topY = world.maxY - 2

        val holesInRegion = if (holes.size >= 32) {
            holes.subSet(
                Hole(Hole.Type.ONE_ONE, BoundingBox(mutableLocal.set(minX() - 2, minY() - 2, minZ() - 2))), true,
                Hole(Hole.Type.ONE_ONE, BoundingBox(mutableLocal.set(maxX() + 2, maxY() + 2, maxZ() + 2))), true
            )
        } else {
            holes
        }

        // Only check positions in this chunk (pos is BlockPos.Mutable)
        for (pos in this) {
            if (pos.y >= topY || holesInRegion.any { pos in it } || !buffer.checkSameXZ(pos)) {
                continue
            }

            val surroundings = Direction.BY_2D_DATA.filterTo(ArrayList(4)) { direction ->
                val cached = buffer.cache(mutable.setWithOffset(pos, direction))
                cached == BLAST_RESISTANT || cached == INDESTRUCTIBLE
            }

            when (surroundings.size) {
                // 1*1
                4 -> {
                    val bedrockOnly = DIRECTIONS_EXCLUDING_UP.all { direction ->
                        val cached = buffer.cache(mutable.setWithOffset(pos, direction))
                        cached == INDESTRUCTIBLE
                    }

                    holes += Hole(Hole.Type.ONE_ONE, BoundingBox(pos), bedrockOnly)
                }
                // 1*2
                3 -> {
                    val airDirection = Direction.BY_2D_DATA.first { it !in surroundings }
                    val another = pos.relative(airDirection)

                    if (!buffer.checkSameXZ(another)) {
                        continue
                    }

                    val airOpposite = airDirection.opposite
                    var idx = 0
                    val checkDirections = Array(3) {
                        val value = Direction.BY_2D_DATA[idx++]
                        if (value === airOpposite) Direction.BY_2D_DATA[idx++] else value
                    }

                    if (buffer.checkSurroundings(another, checkDirections)) {
                        holes += Hole(Hole.Type.ONE_TWO, BoundingBox.fromCorners(pos, another))
                    }
                }
                // 2*2
                2 -> {
                    val (direction1, direction2) = Direction.BY_2D_DATA.filterTo(ArrayList(2)) { it !in surroundings }

                    if (!buffer.checkState(
                            mutableLocal.setWithOffset(pos, direction1),
                            direction1, direction2.opposite
                        )
                    ) {
                        continue
                    }

                    if (!buffer.checkState(
                            mutableLocal.setWithOffset(pos, direction2),
                            direction2, direction1.opposite
                        )
                    ) {
                        continue
                    }

                    if (!buffer.checkState(mutableLocal.move(direction1), direction1, direction2)) {
                        continue
                    }

                    holes += Hole(Hole.Type.TWO_TWO, BoundingBox.fromCorners(pos, mutableLocal))
                }
            }
        }
    }

    private fun BlockStateBuffer.cache(blockPos: BlockPos): State {
        val longValue = blockPos.asLong()
        if (containsKey(longValue)) {
            return get(longValue)
        } else {
            val state = blockPos.getState() ?: return AIR
            val result = when {
                state.isAir -> AIR
                state.block in BLAST_RESISTANT_BLOCKS -> BLAST_RESISTANT
                state.block in INDESTRUCTIBLE_BLOCKS -> INDESTRUCTIBLE
                else -> BREAKABLE
            }
            put(longValue, result)
            return result
        }
    }

    private fun BlockStateBuffer.checkSameXZ(blockPos: BlockPos): Boolean {
        mutable.set(blockPos.x, blockPos.y - 1, blockPos.z)
        val cached = cache(mutable)
        if (cached != BLAST_RESISTANT && cached != INDESTRUCTIBLE) {
            return false
        }

        repeat(3) {
            mutable.y++
            if (cache(mutable) != AIR) {
                return false
            }
        }

        return true
    }

    private fun BlockStateBuffer.checkSurroundings(
        blockPos: BlockPos,
        directions: Array<out Direction>
    ): Boolean {
        return directions.all {
            val cached = cache(mutable.setWithOffset(blockPos, it))
            cached == BLAST_RESISTANT || cached == INDESTRUCTIBLE
        }
    }

    private fun BlockStateBuffer.checkState(
        blockPos: BlockPos,
        vararg directions: Direction
    ): Boolean {
        return checkSameXZ(blockPos) && checkSurroundings(blockPos, directions)
    }

    override fun chunkUpdate(chunk: LevelChunk) {
        val region = chunk.toBlockBox()
        if (region.intersects(HoleManager.movableRegionScanner.currentRegion)) {
            invalidate(region)
            region.cachedUpdate()
        }
    }

    override fun clearChunk(pos: ChunkPos) {
        invalidate(pos.toBlockBox())
    }

    override fun clearAllChunks() {
        holes.clear()
    }

}
