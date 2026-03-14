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
package net.ccbluex.liquidbounce.utils.math

import it.unimi.dsi.fastutil.ints.IntArrayList
import net.minecraft.world.phys.AABB
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.random.Random

class AabbMergeUtilTest {

    @Test
    fun `returns empty list for empty input`() {
        assertTrue(mergeIntersectingAabbsSweep<Int>(emptyList()).isEmpty())
    }

    @Test
    fun `returns same single element for singleton input`() {
        val input = listOf(KeyedAabb(box(1.0, 2.0, 3.0, 4.0, 5.0, 6.0), 7))
        val merged = mergeIntersectingAabbsSweep(input)

        assertEquals(1, merged.size)
        assertEquals(7, merged[0].key)
        assertBoxEquals(input[0].box, merged[0].box)
    }

    @Test
    fun `merges overlapping boxes with same key`() {
        val merged = mergeIntersectingAabbsSweep(
            listOf(
                KeyedAabb(box(0.0, 0.0, 0.0, 2.0, 2.0, 2.0), 1),
                KeyedAabb(box(1.0, 0.5, 0.5, 3.0, 2.5, 2.5), 1),
            )
        )

        assertEquals(1, merged.size)
        assertBoxEquals(box(0.0, 0.0, 0.0, 3.0, 2.5, 2.5), merged.single().box)
        assertEquals(1, merged.single().key)
    }

    @Test
    fun `does not merge overlapping boxes with different keys`() {
        val merged = mergeIntersectingAabbsSweep(
            listOf(
                KeyedAabb(box(0.0, 0.0, 0.0, 2.0, 2.0, 2.0), 1),
                KeyedAabb(box(1.0, 0.5, 0.5, 3.0, 2.5, 2.5), 2),
            )
        )

        assertEquals(2, merged.size)
    }

    @Test
    fun `merges same key boxes even when different keys are interleaved`() {
        val merged = mergeIntersectingAabbsSweep(
            listOf(
                KeyedAabb(box(0.0, 0.0, 0.0, 2.0, 2.0, 2.0), 1),
                KeyedAabb(box(0.5, 0.0, 0.0, 2.5, 2.0, 2.0), 2),
                KeyedAabb(box(1.0, 0.0, 0.0, 3.0, 2.0, 2.0), 1),
            )
        )

        assertEquals(2, merged.size)

        val mergedKeyOne = merged.first { it.key == 1 }
        assertBoxEquals(box(0.0, 0.0, 0.0, 3.0, 2.0, 2.0), mergedKeyOne.box)
    }

    @Test
    fun `merges transitively connected overlaps`() {
        val merged = mergeIntersectingAabbsSweep(
            listOf(
                KeyedAabb(box(0.0, 0.0, 0.0, 2.0, 2.0, 2.0), 1),
                KeyedAabb(box(1.5, 0.0, 0.0, 3.5, 2.0, 2.0), 1),
                KeyedAabb(box(3.0, 0.0, 0.0, 5.0, 2.0, 2.0), 1),
            )
        )

        assertEquals(1, merged.size)
        assertBoxEquals(box(0.0, 0.0, 0.0, 5.0, 2.0, 2.0), merged.single().box)
    }

    @Test
    fun `does not merge boxes that only touch`() {
        val merged = mergeIntersectingAabbsSweep(
            listOf(
                KeyedAabb(box(0.0, 0.0, 0.0, 1.0, 1.0, 1.0), 1),
                KeyedAabb(box(1.0, 0.0, 0.0, 2.0, 1.0, 1.0), 1),
            )
        )

        assertEquals(2, merged.size)
    }

    @Test
    fun `keeps all disjoint boxes unmerged`() {
        val merged = mergeIntersectingAabbsSweep(
            listOf(
                KeyedAabb(box(0.0, 0.0, 0.0, 1.0, 1.0, 1.0), 1),
                KeyedAabb(box(2.0, 0.0, 0.0, 3.0, 1.0, 1.0), 1),
                KeyedAabb(box(4.0, 0.0, 0.0, 5.0, 1.0, 1.0), 1),
            )
        )

        assertEquals(3, merged.size)
    }

    @Test
    fun `matches naive merge on small random data`() {
        assertMatchesNaiveForRandomDataset(size = 24, seed = 1001)
    }

    @Test
    fun `matches naive merge on medium random data`() {
        assertMatchesNaiveForRandomDataset(size = 96, seed = 2002)
    }

    @Test
    fun `matches naive merge on large random data`() {
        assertMatchesNaiveForRandomDataset(size = 320, seed = 3003)
    }

    private fun box(minX: Double, minY: Double, minZ: Double, maxX: Double, maxY: Double, maxZ: Double): AABB =
        AABB(minX, minY, minZ, maxX, maxY, maxZ)

    private fun assertBoxEquals(expected: AABB, actual: AABB) {
        assertEquals(expected.minX, actual.minX, 1e-9)
        assertEquals(expected.minY, actual.minY, 1e-9)
        assertEquals(expected.minZ, actual.minZ, 1e-9)
        assertEquals(expected.maxX, actual.maxX, 1e-9)
        assertEquals(expected.maxY, actual.maxY, 1e-9)
        assertEquals(expected.maxZ, actual.maxZ, 1e-9)
    }

    private fun assertMatchesNaiveForRandomDataset(size: Int, seed: Int) {
        val input = randomDataset(size, Random(seed))
        val fast = normalize(mergeIntersectingAabbsSweep(input))
        val naive = normalize(naiveMerge(input))
        assertEquals(naive, fast)
    }

    private fun randomDataset(size: Int, random: Random): List<KeyedAabb<Int>> {
        val list = ArrayList<KeyedAabb<Int>>(size)
        repeat(size) {
            val key = random.nextInt(0, 4)
            val cx = random.nextDouble(0.0, 100.0)
            val cy = random.nextDouble(0.0, 20.0)
            val cz = random.nextDouble(0.0, 100.0)
            val sx = random.nextDouble(0.3, 3.0)
            val sy = random.nextDouble(0.3, 3.0)
            val sz = random.nextDouble(0.3, 3.0)

            val aabb = box(
                cx - sx * 0.5,
                cy - sy * 0.5,
                cz - sz * 0.5,
                cx + sx * 0.5,
                cy + sy * 0.5,
                cz + sz * 0.5
            )
            list += KeyedAabb(aabb, key)
        }
        assertFalse(list.isEmpty())
        return list
    }

    @Suppress("CognitiveComplexMethod")
    private fun naiveMerge(items: List<KeyedAabb<Int>>): List<KeyedAabb<Int>> {
        if (items.isEmpty()) return emptyList()

        val visited = BooleanArray(items.size)
        val result = ArrayList<KeyedAabb<Int>>(items.size)
        val stack = IntArrayList()

        for (start in items.indices) {
            if (visited[start]) continue

            visited[start] = true
            stack.clear()
            stack.push(start)

            val key = items[start].key
            var merged = items[start].box

            while (stack.isNotEmpty()) {
                val current = stack.popInt()
                val currentBox = items[current].box

                for (candidate in items.indices) {
                    if (visited[candidate]) continue
                    if (items[candidate].key != key) continue
                    if (!currentBox.intersects(items[candidate].box)) continue

                    visited[candidate] = true
                    stack.push(candidate)
                    merged = merged.minmax(items[candidate].box)
                }
            }

            result += KeyedAabb(merged, key)
        }

        return result
    }

    private fun normalize(items: List<KeyedAabb<Int>>): List<String> {
        return items.map {
            "${it.key}|${it.box.minX}|${it.box.minY}|${it.box.minZ}|${it.box.maxX}|${it.box.maxY}|${it.box.maxZ}"
        }.sorted()
    }
}
