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
package net.ccbluex.liquidbounce.utils.block

import it.unimi.dsi.fastutil.longs.LongOpenHashSet
import it.unimi.dsi.fastutil.longs.LongSet
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
import net.ccbluex.fastutil.forEachLong
import net.ccbluex.liquidbounce.utils.math.contains
import net.minecraft.core.BlockPos
import net.minecraft.world.level.ChunkPos
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.chunk.LevelChunk
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantReadWriteLock
import java.util.function.LongPredicate
import java.util.function.Predicate
import kotlin.concurrent.read
import kotlin.concurrent.write

/**
 * Tracks locations of specific states in the world.
 *
 * @param T state type
 */
sealed class AbstractBlockLocationTracker<T> : ChunkScanner.BlockChangeSubscriber {

    /**
     * Gets the [state] of specified [BlockPos] and its [BlockState].
     *
     * @return null if [pos] should be untracked
     */
    abstract fun getStateFor(pos: BlockPos, state: BlockState): T?

    /**
     * Untracks a [BlockPos].
     *
     * @return true if the [pos] was tracked and now removed, false if the [pos] is untracked.
     */
    abstract fun untrack(pos: BlockPos): Boolean

    /**
     * Tracks a [BlockPos] with [state].
     */
    abstract fun track(pos: BlockPos, state: T)

    /**
     * Returns a [Sequence] providing all tracked [BlockPos].
     *
     * Note: The elements of the [Sequence] is [BlockPos.MutableBlockPos]. Copy them if they will be maintained.
     */
    abstract fun allPositions(): Sequence<BlockPos>

    /**
     * Returns a [Sequence] providing all tracked [BlockPos] and its state [T].
     *
     * Note: The elements of the [Map.Entry.key] is [BlockPos.MutableBlockPos]. Copy them if they will be maintained.
     */
    abstract fun iterate(): Sequence<Map.Entry<BlockPos, T>>

    /**
     * Returns if there iss nothing tracked.
     */
    abstract fun isEmpty(): Boolean

    open fun onUpdated() {
        // NOP
    }

    final override fun recordBlock(pos: BlockPos, state: BlockState, cleared: Boolean) {
        val newState = this.getStateFor(pos, state)

        if (newState == null) {
            if (!cleared) {
                untrack(pos)
            }
        } else {
            track(pos, newState)
        }
    }

    final override fun chunkUpdate(chunk: LevelChunk) {
        // NOP
    }

    /**
     * This base implementation stores multiple [BlockPos] for each state [T].
     *
     * If one instance of [T] will be mapped from many [BlockPos],
     * for example, [T] is [Enum], this base will consume less memory.
     *
     * @param T the generic should be stable for hash.
     * @see BlockPos2State
     * @see AbstractBlockLocationTracker
     */
    abstract class State2BlockPos<T> : AbstractBlockLocationTracker<T>() {
        private val stateAndPositions = Object2ObjectOpenHashMap<T, LongSet>()

        private val lock = ReentrantReadWriteLock()

        final override fun allPositions() = sequence<BlockPos> {
            val mutable = BlockPos.MutableBlockPos()
            lock.read {
                for (positions in stateAndPositions.values) {
                    val iterator = positions.longIterator()
                    while (iterator.hasNext()) {
                        mutable.set(iterator.nextLong())
                        yield(mutable)
                    }
                }
            }
        }

        final override fun iterate() = sequence<Map.Entry<BlockPos, T>> {
            val mutable = BlockPos.MutableBlockPos()
            var entry: FullMutableEntry<BlockPos, T>? = null
            lock.read {
                for ((state, positions) in stateAndPositions) {
                    positions.forEachLong {
                        mutable.set(it)
                        if (entry == null) {
                            entry = FullMutableEntry(mutable, state)
                        } else {
                            entry.value = state
                        }
                        yield(entry)
                    }
                }
            }
        }

        fun iterate(type: T): Sequence<BlockPos> {
            val positions = stateAndPositions[type] ?: return emptySequence()

            return sequence {
                val mutable = BlockPos.MutableBlockPos()
                lock.read {
                    positions.forEachLong {
                        yield(mutable.set(it))
                    }
                }
            }
        }

        final override fun isEmpty() = lock.read {
            stateAndPositions.isEmpty() || stateAndPositions.values.all { it.isEmpty() }
        }

        final override fun track(pos: BlockPos, state: T) {
            val added = lock.write {
                stateAndPositions.computeIfAbsent(state) { LongOpenHashSet() }.add(pos.asLong())
            }
            if (added) {
                onUpdated()
            }
        }

        final override fun untrack(pos: BlockPos): Boolean {
            val removed = lock.write {
                val longValue = pos.asLong()
                stateAndPositions.values.any { it.remove(longValue) }
            }
            return if (removed) {
                onUpdated()
                true
            } else {
                false
            }
        }

        final override fun clearAllChunks() {
            lock.write {
                stateAndPositions.clear()
                onUpdated()
            }
        }

        final override fun clearChunk(pos: ChunkPos) {
            var removed = false
            lock.write {
                val predicate = LongPredicate(pos::contains)
                stateAndPositions.values.forEach { set ->
                    removed = removed || set.removeIf(predicate)
                }
            }
            if (removed) {
                onUpdated()
            }
        }

        private class FullMutableEntry<K, V>(override var key: K, override var value: V) : Map.Entry<K, V>
    }

    /**
     * This base implementation stores [BlockPos] and state [T] one by one.
     *
     * @see State2BlockPos
     * @see AbstractBlockLocationTracker
     */
    abstract class BlockPos2State<T : Any> : AbstractBlockLocationTracker<T>() {
        private val positionAndState = ConcurrentHashMap<BlockPos, T>()

        final override fun allPositions() = positionAndState.keys.asSequence()

        final override fun iterate() = positionAndState.entries.asSequence()

        final override fun isEmpty() = positionAndState.isEmpty()

        final override fun track(pos: BlockPos, state: T) {
            positionAndState[pos.immutable] = state
            onUpdated()
        }

        final override fun untrack(pos: BlockPos): Boolean =
            if (positionAndState.remove(pos) != null) {
                onUpdated()
                true
            } else {
                false
            }

        final override fun clearAllChunks() {
            positionAndState.clear()
            onUpdated()
        }

        final override fun clearChunk(pos: ChunkPos) {
            if (positionAndState.keys.removeIf(Predicate(pos::contains))) {
                onUpdated()
            }
        }
    }
}
