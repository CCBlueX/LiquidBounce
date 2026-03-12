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

import net.ccbluex.fastutil.objectDoubleMapOf
import java.util.function.Predicate

/**
 * Returns the shortest path from [start] to the first node where [isGoal] returns true.
 *
 * All edge costs must be non-negative.
 */
fun <T> dijkstraShortestPath(
    start: T,
    isGoal: Predicate<T>,
    neighbors: (T) -> Iterable<WeightedEdge<T>>,
    maxIterations: Int = Int.MAX_VALUE,
    maxCost: Double = Double.POSITIVE_INFINITY,
): ShortestPath<T>? = aStarShortestPath(
    start = start,
    isGoal = isGoal,
    neighbors = neighbors,
    heuristic = objectDoubleMapOf(),
    maxIterations = maxIterations,
    maxCost = maxCost,
)
