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

package net.ccbluex.liquidbounce.utils.collection

import it.unimi.dsi.fastutil.ints.Int2ObjectRBTreeMap
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet

/**
 * A simple least frequency used cache. Non-thread-safe.
 */
class LfuCache<K : Any, V : Any>(
    @get:JvmName("capacity")
    val capacity: Int,
) {
    init {
        require(capacity > 0) { "capacity should be positive" }
    }

    private val cache = Object2ObjectOpenHashMap<K, V>()
    private val counts = Object2IntOpenHashMap<K>()
    private val countTable = Int2ObjectRBTreeMap<MutableSet<K>>()
    private val setPool = ArrayDeque<MutableSet<K>>(8)

    @PublishedApi
    internal val lock = Any()

    private fun newSet() = if (setPool.isEmpty()) ObjectOpenHashSet() else setPool.removeFirst()

    @get:JvmName("size")
    val size: Int get() = synchronized(lock) { cache.size }

    private fun incr(key: K) {

        synchronized(lock) {
            val oldCount = counts.addTo(key, 1)
            val setOfOldCount = countTable.get(oldCount)
            if (setOfOldCount != null) {
                if (setOfOldCount.size == 1) {
                    countTable.remove(oldCount)
                    setOfOldCount.clear()
                    setPool.add(setOfOldCount)
                } else {
                    setOfOldCount.remove(key)
                }
            }
            countTable.computeIfAbsent(oldCount + 1) { newSet() }.add(key)
        }
    }

    private fun discard() {
        synchronized(lock) {
            val entryIter = countTable.int2ObjectEntrySet().iterator()
            while (entryIter.hasNext()) {
                val entry = entryIter.next()
                val set = entry.value
                if (set.isNotEmpty()) {
                    val iter = set.iterator()
                    val toRemove = iter.next()
                    iter.remove()
                    cache.remove(toRemove)
                    counts.removeInt(toRemove)
                    if (!iter.hasNext()) {
                        setPool.add(set)
                        entryIter.remove()
                    }
                    break
                }
            }
        }
    }

    operator fun get(key: K): V? {
        synchronized(lock) {
            return cache[key]?.also { incr(key) }
        }
    }

    operator fun set(key: K, value: V): V {
        synchronized(lock) {
            cache.computeIfPresent(key) { k, oldV ->
                counts.addTo(k, 1)
                value
            }?.let { return value }

            if (cache.size >= capacity) {
                discard()
            }

            cache.put(key, value)
            counts.put(key, 1)
            countTable.computeIfAbsent(1) { newSet() }.add(key)

            return value
        }
    }

    inline fun getOrPut(key: K, value: () -> V): V {
        synchronized(lock) {
            return get(key) ?: set(key, value())
        }
    }
}
