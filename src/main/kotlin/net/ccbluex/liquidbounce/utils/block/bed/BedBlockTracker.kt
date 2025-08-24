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
 */
package net.ccbluex.liquidbounce.utils.block.bed

import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet
import net.ccbluex.liquidbounce.utils.block.AbstractBlockLocationTracker
import net.ccbluex.liquidbounce.utils.block.ChunkScanner
import net.ccbluex.liquidbounce.utils.block.getState
import net.ccbluex.liquidbounce.utils.block.isBed
import net.ccbluex.liquidbounce.utils.block.searchBedLayer
import net.ccbluex.liquidbounce.utils.client.world
import net.ccbluex.liquidbounce.utils.kotlin.component1
import net.ccbluex.liquidbounce.utils.kotlin.component2
import net.minecraft.block.BedBlock
import net.minecraft.block.Block
import net.minecraft.block.BlockState
import net.minecraft.block.Blocks
import net.minecraft.block.DoubleBlockProperties
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3d

object BedBlockTracker : AbstractBlockLocationTracker.BlockPos2State<BedState>() {
    private var maxLayers: Int = 0

    private val subscribers = ReferenceOpenHashSet<Subscriber>()

    internal fun triggerRescan() {
        val newMaxLayers = if (subscribers.isEmpty()) 0 else subscribers.maxOf { it.maxLayers }
        if (newMaxLayers == maxLayers) {
            return
        }
        maxLayers = newMaxLayers
        ChunkScanner.unsubscribe(this)
        if (newMaxLayers > 0) {
            ChunkScanner.subscribe(this)
        }
    }

    fun subscribe(subscriber: Subscriber) {
        subscribers += subscriber
        triggerRescan()
    }

    fun unsubscribe(subscriber: Subscriber) {
        subscribers -= subscriber
        triggerRescan()
    }

    private val WHITELIST_NON_SOLID = setOf(
        Blocks.LADDER,

        Blocks.WATER,

        Blocks.GLASS,
        Blocks.WHITE_STAINED_GLASS,
        Blocks.ORANGE_STAINED_GLASS,
        Blocks.MAGENTA_STAINED_GLASS,
        Blocks.LIGHT_BLUE_STAINED_GLASS,
        Blocks.YELLOW_STAINED_GLASS,
        Blocks.LIME_STAINED_GLASS,
        Blocks.PINK_STAINED_GLASS,
        Blocks.GRAY_STAINED_GLASS,
        Blocks.LIGHT_GRAY_STAINED_GLASS,
        Blocks.CYAN_STAINED_GLASS,
        Blocks.PURPLE_STAINED_GLASS,
        Blocks.BLUE_STAINED_GLASS,
        Blocks.BROWN_STAINED_GLASS,
        Blocks.GREEN_STAINED_GLASS,
        Blocks.RED_STAINED_GLASS,
        Blocks.BLACK_STAINED_GLASS,
    )

    private fun BlockPos.getBedSurroundingBlocks(blockState: BlockState): Set<SurroundingBlock> {
        val layers = Array<Reference2IntOpenHashMap<Block>>(maxLayers, ::Reference2IntOpenHashMap)

        searchBedLayer(blockState, maxLayers)
            .forEach { (layer, pos) ->
                val state = pos.getState()

                // Ignore empty positions
                if (state == null || state.isAir) {
                    return@forEach
                }

                val block = state.block
                if (state.isSolidBlock(world, pos) || block in WHITELIST_NON_SOLID) {
                    // Count blocks (getInt default = 0)
                    with(layers[layer - 1]) {
                        put(block, getInt(block) + 1)
                    }
                }
            }

        val result = sortedSetOf<SurroundingBlock>()

        layers.forEachIndexed { i, map ->
            map.reference2IntEntrySet().forEach { (block, count) ->
                result += SurroundingBlock(block, count, i + 1)
            }
        }

        return result
    }

    private fun BlockPos.getBedPlates(headState: BlockState): BedState {
        val bedDirection = headState.get(BedBlock.FACING)

        val bedBlock = headState.block
        val renderPos = Vec3d(
            x - (bedDirection.offsetX * 0.5) + 0.5,
            y + 1.0,
            z - (bedDirection.offsetZ * 0.5) + 0.5,
        )

        return BedState(bedBlock, renderPos, getBedSurroundingBlocks(headState))
    }

    @Suppress("detekt:CognitiveComplexMethod")
    override fun getStateFor(pos: BlockPos, state: BlockState): BedState? {
        return if (state.isBed) {
            val part = BedBlock.getBedPart(state)
            // Only track the first part (head) of the bed
            if (part == DoubleBlockProperties.Type.FIRST) {
                pos.getBedPlates(state)
            } else {
                null
            }
        } else {
            // A non-bed block was updated, we need to update the bed blocks around it
            val distance = maxLayers

            allPositions().forEach { bedPos ->
                // Update if the block is close to a bed
                if (bedPos.getManhattanDistance(pos) > distance) {
                    return@forEach
                }

                val bedState = bedPos.getState()
                if (bedState == null || !bedState.isBed) {
                    // The tracked block is not a bed anymore, remove it
                    untrack(bedPos)
                } else {
                    track(bedPos, bedPos.getBedPlates(bedState))
                }
            }

            null
        }
    }

    interface Subscriber {
        val maxLayers: Int
    }
}
