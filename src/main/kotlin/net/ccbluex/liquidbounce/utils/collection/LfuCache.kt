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
import net.ccbluex.fastutil.fastIterator

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

    /**
     * [Map] backend.
     */
    private val cache = Object2ObjectOpenHashMap<K, V>()

    /**
     * Access count of each key.
     *
     * The key set of [counts] is same as [cache]:
     * `counts.keys == cache.keys`
     */
    private val counts = Object2IntOpenHashMap<K>()

    /**
     * The access count and keys with this count. Sorted ascending for discarding the least used ones.
     *
     * This matches:
     * `countTable.values.flatten() == counts.keys`
     * `countTable.keys == counts.values`
     */
    private val countTable = Int2ObjectRBTreeMap<MutableSet<K>>()

    private val setPool = ArrayDeque<MutableSet<K>>(8)

    /**
     * Creates a [MutableSet] (or get from the pool) for [countTable].
     */
    private fun newSet() = if (setPool.isEmpty()) ObjectOpenHashSet() else setPool.removeFirst()

    @get:JvmName("size")
    val size: Int get() = cache.size

    /**
     * Increases the access count of [key].
     */
    private fun incr(key: K) {
        val oldCount = counts.addTo(key, 1)
        val setOfOldCount = countTable.get(oldCount)
        if (setOfOldCount.size == 1) {
            countTable.remove(oldCount)
            setOfOldCount.clear()
            setPool.add(setOfOldCount)
        } else {
            setOfOldCount.remove(key)
        }
        countTable.computeIfAbsent(oldCount + 1) { newSet() }.add(key)
    }

    /**
     * Discards one of the least-used keys.
     */
    private fun discard() {
        val entryIter = countTable.fastIterator()
        while (entryIter.hasNext()) {
            val entry = entryIter.next()
            val set = entry.value
            val iter = set.iterator()
            if (iter.hasNext()) {
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

    /**
     * Gets the key and corresponding value (if exists), and increases its access count.
     */
    operator fun get(key: K): V? {
        return cache[key]?.also { incr(key) }
    }

    /**
     * Sets the key and corresponding value, and discards one of the least-used keys if full.
     */
    operator fun set(key: K, value: V): V {
        cache.computeIfPresent(key) { k, _ ->
            incr(k)
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

    inline fun getOrPut(key: K, value: () -> V): V {
        return get(key) ?: set(key, value())
    }

}
