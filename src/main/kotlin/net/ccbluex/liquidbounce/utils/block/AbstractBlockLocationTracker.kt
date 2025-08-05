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
package net.ccbluex.liquidbounce.utils.block

import it.unimi.dsi.fastutil.longs.LongOpenHashSet
import it.unimi.dsi.fastutil.longs.LongSet
import net.minecraft.block.BlockState
import net.minecraft.util.math.BlockPos
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantReadWriteLock
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
     * Note: The elements of the [Sequence] is [BlockPos.Mutable]. Copy them if they will be maintained.
     */
    abstract fun allPositions(): Sequence<BlockPos>

    /**
     * Returns a [Sequence] providing all tracked [BlockPos] and its state [T].
     *
     * Note: The elements of the [Map.Entry.key] is [BlockPos.Mutable]. Copy them if they will be maintained.
     */
    abstract fun iterate(): Sequence<Map.Entry<BlockPos, T>>

    /**
     * Returns if there iss nothing tracked.
     */
    abstract fun isEmpty(): Boolean

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

    final override fun chunkUpdate(x: Int, z: Int) {
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
        private val stateAndPositions = hashMapOf<T, LongSet>()

        private val lock = ReentrantReadWriteLock()

        final override fun allPositions() = sequence<BlockPos> {
            val mutable = BlockPos.Mutable()
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
            val mutable = BlockPos.Mutable()
            var entry: FullMutableEntry<BlockPos, T>? = null
            lock.read {
                for ((state, positions) in stateAndPositions) {
                    val iterator = positions.longIterator()
                    while (iterator.hasNext()) {
                        mutable.set(iterator.nextLong())
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

        final override fun isEmpty() = lock.read {
            stateAndPositions.isEmpty() || stateAndPositions.values.all { it.isEmpty() }
        }

        final override fun track(pos: BlockPos, state: T) {
            lock.write {
                stateAndPositions.computeIfAbsent(state) { LongOpenHashSet() }.add(pos.asLong())
            }
        }

        final override fun untrack(pos: BlockPos): Boolean {
            lock.write {
                val longValue = pos.asLong()
                return stateAndPositions.values.any { it.remove(longValue) }
            }
        }

        final override fun clearAllChunks() {
            lock.write {
                stateAndPositions.clear()
            }
        }

        final override fun clearChunk(x: Int, z: Int) {
            val pos = BlockPos.Mutable()
            lock.write {
                stateAndPositions.values.forEach { set ->
                    set.removeIf {
                        pos.set(it)
                        pos.x shr 4 == x && pos.z shr 4 == z
                    }
                }
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
    abstract class BlockPos2State<T> : AbstractBlockLocationTracker<T>() {
        private val positionAndState = ConcurrentHashMap<BlockPos, T>()

        final override fun allPositions() = positionAndState.keys.asSequence()

        final override fun iterate() = positionAndState.entries.asSequence()

        final override fun isEmpty() = positionAndState.isEmpty()

        final override fun track(pos: BlockPos, state: T) {
            positionAndState[pos.immutable] = state
        }

        final override fun untrack(pos: BlockPos): Boolean {
            return positionAndState.remove(pos) != null
        }

        final override fun clearAllChunks() {
            positionAndState.clear()
        }

        final override fun clearChunk(x: Int, z: Int) {
            positionAndState.keys.removeIf { it.x shr 4 == x && it.z shr 4 == z }
        }
    }
}
