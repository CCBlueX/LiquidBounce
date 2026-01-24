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
package net.ccbluex.liquidbounce.utils.block.bed

import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet
import net.ccbluex.fastutil.component1
import net.ccbluex.fastutil.component2
import net.ccbluex.fastutil.fastIterator
import net.ccbluex.liquidbounce.event.EventManager
import net.ccbluex.liquidbounce.event.events.BedStateChangeEvent
import net.ccbluex.liquidbounce.utils.block.AbstractBlockLocationTracker
import net.ccbluex.liquidbounce.utils.block.ChunkScanner
import net.ccbluex.liquidbounce.utils.block.getState
import net.ccbluex.liquidbounce.utils.block.isBed
import net.ccbluex.liquidbounce.utils.block.searchBedLayer
import net.ccbluex.liquidbounce.utils.kotlin.unmodifiable
import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.BedBlock
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.DoubleBlockCombiner
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.Vec3

object BedBlockTracker : AbstractBlockLocationTracker.BlockPos2State<BedState>() {
    private var maxLayers: Int = 0

    private val subscribers = ReferenceOpenHashSet<Subscriber>()

    private val CACHE = ThreadLocal.withInitial(BlockPos::MutableBlockPos)

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

    private fun BlockPos.getBedSurroundingBlocks(blockState: BlockState): List<SurroundingBlock> {
        val layers = Array<Reference2IntOpenHashMap<Block>>(maxLayers) { Reference2IntOpenHashMap() }

        val pos = CACHE.get()
        for ((layer, longValue) in searchBedLayer(blockState, maxLayers)) {
            val state = pos.set(longValue).getState()

            // Ignore empty positions
            if (state == null || state.isAir) {
                continue
            }

            // Count blocks (default = 0)
            layers[layer - 1].addTo(state.block, 1)
        }

        val result = arrayOfNulls<SurroundingBlock>(layers.sumOf { it.size })
        var idx = 0

        layers.forEachIndexed { i, map ->
            map.fastIterator().forEach {
                result[idx++] = SurroundingBlock(it.key, it.intValue, i + 1)
            }
        }
        result.sort()

        @Suppress("UNCHECKED_CAST")
        return result.unmodifiable() as List<SurroundingBlock>
    }

    private fun BlockPos.getBedPlates(headState: BlockState): BedState {
        val bedDirection = headState.getValue(BedBlock.FACING)

        val bedBlock = headState.block as BedBlock
        val renderPos = Vec3(
            x - (bedDirection.stepX * 0.5) + 0.5,
            y + 1.0,
            z - (bedDirection.stepZ * 0.5) + 0.5,
        )

        return BedState(bedBlock, this, renderPos, getBedSurroundingBlocks(headState))
    }

    @Suppress("detekt:CognitiveComplexMethod")
    override fun getStateFor(pos: BlockPos, state: BlockState): BedState? {
        return if (state.isBed) {
            val part = BedBlock.getBlockType(state)
            // Only track the first part (head) of the bed
            if (part == DoubleBlockCombiner.BlockType.FIRST) {
                pos.getBedPlates(state)
            } else {
                null
            }
        } else {
            // A non-bed block was updated, we need to update the bed blocks around it
            val distance = maxLayers

            allPositions().forEach { bedPos ->
                // Update if the block is close to a bed
                if (bedPos.distManhattan(pos) > distance) {
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

    override fun onUpdated() {
        val beds = iterate().mapTo(mutableListOf()) { it.value }
        EventManager.callEvent(BedStateChangeEvent(beds))
    }

    interface Subscriber {
        val maxLayers: Int
    }
}
