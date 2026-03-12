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

import net.ccbluex.fastutil.objectDoubleHashMapOf
import net.ccbluex.fastutil.objectDoubleMapOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class GraphSearchTest {

    @Test
    fun `finds optimal path in weighted graph`() {
        val graph = mapOf(
            "S" to listOf(WeightedEdge("A", 2.0), WeightedEdge("B", 1.0)),
            "A" to listOf(WeightedEdge("G", 2.0)),
            "B" to listOf(WeightedEdge("C", 1.0)),
            "C" to listOf(WeightedEdge("G", 10.0)),
        )

        val result = aStarShortestPath(
            start = "S",
            isGoal = { it == "G" },
            neighbors = { graph[it].orEmpty() },
            heuristic = objectDoubleMapOf(),
        )

        val path = assertNotNull(result)
        assertEquals(listOf("S", "A", "G"), path.nodes)
        assertEquals(4.0, path.totalCost)
    }

    @Test
    fun `returns null when goal cannot be reached`() {
        val graph = mapOf(
            "S" to listOf(WeightedEdge("A", 1.0)),
            "A" to listOf(WeightedEdge("B", 1.0)),
            "B" to emptyList(),
        )

        val result = aStarShortestPath(
            start = "S",
            isGoal = { it == "G" },
            neighbors = { graph[it].orEmpty() },
            heuristic = objectDoubleMapOf(),
        )

        assertNull(result)
    }

    @Test
    fun `returns start-only path when start already satisfies goal`() {
        val result = aStarShortestPath(
            start = 42,
            isGoal = { it == 42 },
            neighbors = { emptyList() },
            heuristic = objectDoubleMapOf(),
        )

        val path = assertNotNull(result)
        assertEquals(listOf(42), path.nodes)
        assertEquals(0.0, path.totalCost)
    }

    @Test
    fun `respects maxIterations and can stop early`() {
        val graph = mapOf(
            "S" to listOf(WeightedEdge("A", 1.0)),
            "A" to listOf(WeightedEdge("B", 1.0)),
            "B" to listOf(WeightedEdge("C", 1.0)),
            "C" to listOf(WeightedEdge("G", 1.0)),
        )

        val limited = aStarShortestPath(
            start = "S",
            isGoal = { it == "G" },
            neighbors = { graph[it].orEmpty() },
            heuristic = objectDoubleMapOf(),
            maxIterations = 3,
        )
        assertNull(limited)

        val full = aStarShortestPath(
            start = "S",
            isGoal = { it == "G" },
            neighbors = { graph[it].orEmpty() },
            heuristic = objectDoubleMapOf(),
            maxIterations = 10,
        )
        assertEquals(listOf("S", "A", "B", "C", "G"), assertNotNull(full).nodes)
    }

    @Test
    fun `respects maxCost and prunes expensive routes`() {
        val graph = mapOf(
            "S" to listOf(WeightedEdge("A", 2.0)),
            "A" to listOf(WeightedEdge("G", 2.0)),
        )

        val tooLow = aStarShortestPath(
            start = "S",
            isGoal = { it == "G" },
            neighbors = { graph[it].orEmpty() },
            heuristic = objectDoubleMapOf(),
            maxCost = 3.0,
        )
        assertNull(tooLow)

        val exact = aStarShortestPath(
            start = "S",
            isGoal = { it == "G" },
            neighbors = { graph[it].orEmpty() },
            heuristic = objectDoubleMapOf(),
            maxCost = 4.0,
        )
        val path = assertNotNull(exact)
        assertEquals(listOf("S", "A", "G"), path.nodes)
        assertEquals(4.0, path.totalCost)
    }

    @Test
    fun `finds improved route when better path to known node appears later`() {
        val graph = mapOf(
            "S" to listOf(WeightedEdge("A", 10.0), WeightedEdge("B", 1.0)),
            "B" to listOf(WeightedEdge("A", 1.0)),
            "A" to listOf(WeightedEdge("G", 1.0)),
        )

        val result = aStarShortestPath(
            start = "S",
            isGoal = { it == "G" },
            neighbors = { graph[it].orEmpty() },
            heuristic = objectDoubleMapOf(),
        )

        val path = assertNotNull(result)
        assertEquals(listOf("S", "B", "A", "G"), path.nodes)
        assertEquals(3.0, path.totalCost)
    }

    @Test
    fun `works with admissible heuristic`() {
        val graph = mapOf(
            "S" to listOf(WeightedEdge("A", 1.0), WeightedEdge("B", 4.0)),
            "A" to listOf(WeightedEdge("C", 1.0)),
            "B" to listOf(WeightedEdge("C", 1.0)),
            "C" to listOf(WeightedEdge("G", 1.0)),
        )
        val heuristic = objectDoubleHashMapOf(
            "S", 3.0,
            "A", 2.0,
            "B", 1.0,
            "C", 1.0,
            "G", 0.0,
        )

        val result = aStarShortestPath(
            start = "S",
            isGoal = { it == "G" },
            neighbors = { graph[it].orEmpty() },
            heuristic = heuristic,
        )

        val path = assertNotNull(result)
        assertEquals(listOf("S", "A", "C", "G"), path.nodes)
        assertEquals(3.0, path.totalCost)
    }

    @Test
    fun `handles cycles without infinite looping`() {
        val graph = mapOf(
            "S" to listOf(WeightedEdge("A", 1.0)),
            "A" to listOf(WeightedEdge("B", 1.0)),
            "B" to listOf(WeightedEdge("A", 1.0), WeightedEdge("G", 1.0)),
        )

        val result = aStarShortestPath(
            start = "S",
            isGoal = { it == "G" },
            neighbors = { graph[it].orEmpty() },
            heuristic = objectDoubleMapOf(),
            maxIterations = 50,
        )

        val path = assertNotNull(result)
        assertEquals(listOf("S", "A", "B", "G"), path.nodes)
        assertEquals(3.0, path.totalCost)
    }

    @Test
    fun `selects cheapest among duplicate neighbor edges`() {
        val graph = mapOf(
            "S" to listOf(WeightedEdge("A", 5.0), WeightedEdge("A", 1.0)),
            "A" to listOf(WeightedEdge("G", 1.0)),
        )

        val result = aStarShortestPath(
            start = "S",
            isGoal = { it == "G" },
            neighbors = { graph[it].orEmpty() },
            heuristic = objectDoubleMapOf(),
        )

        val path = assertNotNull(result)
        assertEquals(listOf("S", "A", "G"), path.nodes)
        assertEquals(2.0, path.totalCost)
    }

    @Test
    fun `supports zero-cost edges and exact zero maxCost`() {
        val graph = mapOf(
            "S" to listOf(WeightedEdge("A", 0.0)),
            "A" to listOf(WeightedEdge("G", 0.0)),
        )

        val result = aStarShortestPath(
            start = "S",
            isGoal = { it == "G" },
            neighbors = { graph[it].orEmpty() },
            heuristic = objectDoubleMapOf(),
            maxCost = 0.0,
        )

        val path = assertNotNull(result)
        assertEquals(listOf("S", "A", "G"), path.nodes)
        assertEquals(0.0, path.totalCost)
    }

    @Test
    fun `dijkstra wrapper remains equivalent to zero-heuristic a-star`() {
        val graph = mapOf(
            "S" to listOf(WeightedEdge("A", 2.0), WeightedEdge("B", 1.0)),
            "A" to listOf(WeightedEdge("G", 2.0)),
            "B" to listOf(WeightedEdge("G", 10.0)),
        )

        val viaDijkstra = dijkstraShortestPath(
            start = "S",
            isGoal = { it == "G" },
            neighbors = { graph[it].orEmpty() },
            maxIterations = 100,
            maxCost = 10.0,
        )

        val viaAStar = aStarShortestPath(
            start = "S",
            isGoal = { it == "G" },
            neighbors = { graph[it].orEmpty() },
            heuristic = objectDoubleMapOf(),
            maxIterations = 100,
            maxCost = 10.0,
        )

        assertEquals(assertNotNull(viaAStar).nodes, assertNotNull(viaDijkstra).nodes)
        assertEquals(viaAStar.totalCost, viaDijkstra.totalCost)
    }

    @Test
    fun `negative maxCost yields no path when goal is not start`() {
        val graph = mapOf(
            "S" to listOf(WeightedEdge("G", 0.0)),
        )

        val result = aStarShortestPath(
            start = "S",
            isGoal = { it == "G" },
            neighbors = { graph[it].orEmpty() },
            heuristic = objectDoubleMapOf(),
            maxCost = -0.1,
        )

        assertNull(result)
    }

    @Test
    fun `negative maxCost rejects even immediate start-goal match`() {
        val result = aStarShortestPath(
            start = "S",
            isGoal = { it == "S" },
            neighbors = { emptyList() },
            heuristic = objectDoubleMapOf(),
            maxCost = -0.1,
        )

        assertNull(result)
    }
}
