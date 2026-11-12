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

package net.ccbluex.liquidbounce.utils.navigation.route

import net.ccbluex.liquidbounce.utils.block.AStarSearch
import net.ccbluex.liquidbounce.utils.block.WeightedEdge

/** An A* route search over nodes [N] that runs in slices of [RouteLimits.expansionsPerSlice] within [limits]. */
abstract class AStarRouteSearch<N, T>(protected val limits: RouteLimits) : RouteSearch<T> {

    protected var search: AStarSearch<N>? = null
        private set
    private var computeNanos = 0L
    private var result: RouteResult<T> = RouteResult.Searching

    final override fun advance(): RouteResult<T> {
        if (result != RouteResult.Searching) {
            return result
        }

        val began = System.nanoTime()
        result = search?.let(::expand) ?: begin()
        computeNanos += System.nanoTime() - began

        if (result == RouteResult.Searching && computeNanos > limits.maxComputeNanos) {
            result = closestOr(RouteFailure.TIMED_OUT)
        }
        return result
    }

    private fun expand(search: AStarSearch<N>): RouteResult<T> {
        val path = search.advance(minOf(limits.expansionsPerSlice, limits.maxExpansions - search.iterations))
        return when {
            path != null -> route(path.nodes)
            search.isFinished -> closestOr(RouteFailure.NO_PATH)
            search.iterations >= limits.maxExpansions -> closestOr(RouteFailure.TOO_COMPLEX)
            else -> RouteResult.Searching
        }
    }

    /** The first slice: a result known without the A*, or [searchFrom] the start node. */
    protected abstract fun begin(): RouteResult<T>

    protected fun searchFrom(start: N, maxCost: Double = Double.POSITIVE_INFINITY): RouteResult<T> {
        search = AStarSearch(
            start = start,
            isGoal = ::isGoal,
            neighbors = ::neighbors,
            heuristic = ::heuristic,
            maxCost = maxCost,
        )
        return RouteResult.Searching
    }

    protected abstract fun isGoal(node: N): Boolean

    protected abstract fun neighbors(node: N): Iterable<WeightedEdge<N>>

    protected abstract fun heuristic(node: N): Double

    protected abstract fun route(nodes: List<N>): RouteResult<T>

    /** What a search returns that ends by [failure] short of the goal. */
    protected open fun closestOr(failure: RouteFailure): RouteResult<T> = RouteResult.Failed(failure)

    /** The path to the explored node closest to the goal, unless [distance] shrinks less than [MIN_PROGRESS]. */
    protected fun closestNodes(distance: (N) -> Double): List<N>? = search?.closestPath()?.nodes?.takeUnless {
        it.size < 2 || distance(it.first()) - distance(it.last()) < MIN_PROGRESS
    }

    private companion object {
        const val MIN_PROGRESS = 1.0
    }

}
