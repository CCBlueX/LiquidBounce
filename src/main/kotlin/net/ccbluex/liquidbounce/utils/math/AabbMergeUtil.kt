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
@file:Suppress("MatchingDeclarationName")
package net.ccbluex.liquidbounce.utils.math

import it.unimi.dsi.fastutil.ints.IntArrayList
import it.unimi.dsi.fastutil.ints.IntArrays
import it.unimi.dsi.fastutil.ints.IntList
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap
import net.minecraft.world.phys.AABB

/**
 * A world-space AABB tagged with a merge key.
 * Only boxes with equal keys are allowed to be merged.
 */
@JvmRecord
data class KeyedAabb<K>(
    val box: AABB,
    val key: K
)

/**
 * Merge transitive intersecting AABBs using sweep-and-prune broadphase + union-find.
 *
 * Intersection uses vanilla [AABB.intersects] semantics (strict overlap, no touching-only merge).
 */
fun <K> mergeIntersectingAabbsSweep(items: List<KeyedAabb<K>>): List<KeyedAabb<K>> {
    if (items.isEmpty()) {
        return emptyList()
    }

    val buckets = Object2ObjectLinkedOpenHashMap<K, IntArrayList>()
    for (index in items.indices) {
        val key = items[index].key
        buckets.computeIfAbsent(key) { IntArrayList() }.add(index)
    }

    val dsu = DisjointSet(items.size)
    for (bucket in buckets.values) {
        unionIntersectingInBucket(items, bucket, dsu)
    }

    val rootToMerged = IntArray(items.size) { -1 }
    val mergedBoxes = ArrayList<AABB>(items.size)
    val mergedKeys = ArrayList<K>(items.size)

    for (index in items.indices) {
        val root = dsu.find(index)
        val mergedIndex = rootToMerged[root]
        if (mergedIndex == -1) {
            rootToMerged[root] = mergedBoxes.size
            mergedBoxes += items[index].box
            mergedKeys += items[index].key
            continue
        }

        mergedBoxes[mergedIndex] = mergedBoxes[mergedIndex].minmax(items[index].box)
    }

    return Array(mergedBoxes.size) { i ->
        KeyedAabb(mergedBoxes[i], mergedKeys[i])
    }.asList()
}

private class DisjointSet(size: Int) {
    private val parent = IntArray(size) { it }
    private val rank = IntArray(size)

    fun find(node: Int): Int {
        var current = node
        while (current != parent[current]) {
            parent[current] = parent[parent[current]]
            current = parent[current]
        }
        return current
    }

    fun union(left: Int, right: Int) {
        var rootLeft = find(left)
        var rootRight = find(right)

        if (rootLeft == rootRight) {
            return
        }

        if (rank[rootLeft] < rank[rootRight]) {
            val tmp = rootLeft
            rootLeft = rootRight
            rootRight = tmp
        }

        parent[rootRight] = rootLeft
        if (rank[rootLeft] == rank[rootRight]) {
            rank[rootLeft]++
        }
    }
}

private fun <K> unionIntersectingInBucket(
    items: List<KeyedAabb<K>>,
    bucket: IntList,
    dsu: DisjointSet
) {
    if (bucket.size <= 1) {
        return
    }

    val order = bucket.toIntArray()
    IntArrays.quickSort(order) { left, right ->
        val cmp = items[left].box.minX.compareTo(items[right].box.minX)
        if (cmp != 0) cmp else left - right
    }

    val active = IntArrayList(order.size)
    for (index in order) {
        val currentBox = items[index].box

        var write = 0
        for (read in 0 until active.size) {
            val activeIndex = active.getInt(read)
            if (items[activeIndex].box.maxX > currentBox.minX) {
                active.set(write++, activeIndex)
            }
        }
        active.size(write)

        for (activeIdx in 0 until active.size) {
            val candidateIndex = active.getInt(activeIdx)
            if (!items[candidateIndex].box.intersects(currentBox)) continue
            dsu.union(index, candidateIndex)
        }

        active.add(index)
    }
}
