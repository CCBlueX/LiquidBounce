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

import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
import java.util.PriorityQueue
import java.util.function.Function
import java.util.function.Predicate
import java.util.function.ToDoubleFunction

@JvmRecord
data class WeightedEdge<T>(
    val node: T,
    val cost: Double,
)

@JvmRecord
data class ShortestPath<T>(
    val nodes: List<T>,
    val totalCost: Double,
)

@JvmRecord
private data class QueueEntry<T>(
    val node: T,
    val gScore: Double,
    val fScore: Double,
)

/**
 * Finds the shortest path using A* search.
 *
 * Set [heuristic] to zero to get Dijkstra behavior.
 */
fun <T> aStarShortestPath(
    start: T,
    isGoal: Predicate<T>,
    neighbors: Function<T, Iterable<WeightedEdge<T>>>,
    heuristic: ToDoubleFunction<T>,
    maxIterations: Int = Int.MAX_VALUE,
    maxCost: Double = Double.POSITIVE_INFINITY,
): ShortestPath<T>? {
    require(maxIterations > 0) { "maxIterations must be positive." }

    return AStarSearch(start, isGoal, neighbors, heuristic, maxCost).advance(maxIterations)
}

/**
 * A* search that can be advanced in slices, so one search can be spread over several calls.
 *
 * Not thread-safe: advance it from one thread at a time.
 */
class AStarSearch<T>(
    private val start: T,
    private val isGoal: Predicate<T>,
    private val neighbors: Function<T, Iterable<WeightedEdge<T>>>,
    private val heuristic: ToDoubleFunction<T>,
    private val maxCost: Double = Double.POSITIVE_INFINITY,
) {

    private val gScores = Object2DoubleOpenHashMap<T>().apply {
        defaultReturnValue(Double.POSITIVE_INFINITY)
        put(start, 0.0)
    }
    private val previous = Object2ObjectOpenHashMap<T, T>()
    private val queue = PriorityQueue(Comparator.comparingDouble<QueueEntry<T>> { it.fScore }).apply {
        add(QueueEntry(start, 0.0, heuristic.applyAsDouble(start)))
    }

    /**
     * Nodes polled so far.
     */
    var iterations = 0
        private set

    var result: ShortestPath<T>? = null
        private set

    private var closest: QueueEntry<T>? = null
    private var closestHeuristic = Double.POSITIVE_INFINITY

    /**
     * True once a path was found or no node is left to explore.
     */
    val isFinished: Boolean
        get() = result != null || queue.isEmpty()

    /**
     * Polls at most [maxIterations] nodes and returns the path once it is known.
     */
    fun advance(maxIterations: Int): ShortestPath<T>? {
        var budget = maxIterations
        while (result == null && budget > 0 && queue.isNotEmpty()) {
            budget--
            iterations++
            expand(queue.poll())
        }

        return result
    }

    /**
     * The node the best known path to [node] comes from; null for the start and nodes not reached yet.
     */
    fun previous(node: T): T? = previous[node]

    /**
     * Cost of the best known path to [node]; infinite for nodes not reached yet.
     */
    fun cost(node: T): Double = gScores.getDouble(node)

    /**
     * The path to the explored node with the smallest heuristic, or null if no node beats the start.
     */
    fun closestPath(): ShortestPath<T>? {
        val closest = closest ?: return null
        if (closest.node == start) {
            return null
        }

        return ShortestPath(reconstructPath(start, closest.node, previous), closest.gScore)
    }

    private fun expand(current: QueueEntry<T>) {
        val bestCurrentG = gScores.getDouble(current.node)
        if (current.gScore > bestCurrentG || current.gScore > maxCost) {
            return
        }

        val currentHeuristic = current.fScore - current.gScore
        if (currentHeuristic < closestHeuristic) {
            closest = current
            closestHeuristic = currentHeuristic
        }

        if (isGoal.test(current.node)) {
            result = ShortestPath(reconstructPath(start, current.node, previous), current.gScore)
            return
        }

        for ((node, cost) in neighbors.apply(current.node)) {
            require(cost >= 0.0) { "Path search edge costs must be non-negative." }

            val candidateG = current.gScore + cost
            if (candidateG > maxCost) {
                continue
            }

            val knownG = gScores.getDouble(node)
            if (candidateG >= knownG) {
                continue
            }

            gScores.put(node, candidateG)
            previous.put(node, current.node)

            val fScore = candidateG + heuristic.applyAsDouble(node)
            queue.add(QueueEntry(node, candidateG, fScore))
        }
    }

}

private fun <T> reconstructPath(
    start: T,
    goal: T,
    previous: Object2ObjectOpenHashMap<T, T>,
): List<T> {
    val reversedPath = mutableListOf<T>()

    var current = goal
    reversedPath.add(current)

    while (current != start) {
        val parent = previous[current]
            ?: return emptyList()
        current = parent
        reversedPath.add(current)
    }

    reversedPath.reverse()
    return reversedPath
}
