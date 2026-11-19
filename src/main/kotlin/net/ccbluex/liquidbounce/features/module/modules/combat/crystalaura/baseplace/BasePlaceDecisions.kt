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
package net.ccbluex.liquidbounce.features.module.modules.combat.crystalaura.baseplace

import net.ccbluex.liquidbounce.utils.block.WeightedEdge
import net.ccbluex.liquidbounce.utils.block.dijkstraShortestPath

/** Keeps upstream BasePlace behavior: obsidian is preferred and bedrock remains the fallback. */
internal fun <T> selectBaseSlot(obsidianSlot: T?, bedrockSlot: T?): T? = obsidianSlot ?: bedrockSlot

/**
 * Finds a connected target-to-anchor placement path whose anchor can be click-placed immediately.
 *
 * A single-element path proves that the target itself can be placed immediately. Longer paths start at the target and
 * end at a currently reachable anchor with an existing solid placement neighbor; every intermediate position is
 * connected to that anchor, so the path can be built in reverse. The caller supplies world access and placement
 * verification, keeping this decision independent of the live client. [neighbors] must only return positions that are
 * legal support candidates and within the caller's depth and range limits.
 *
 * At most [maxIterations] queue entries are expanded. Exhausting that budget returns null rather than treating an
 * unexplored nearby block as a foundation.
 */
internal fun <T> findBasePlaceSupportPath(
    target: T,
    hasSolidNeighbor: (T) -> Boolean,
    canClickPlace: (T) -> Boolean,
    neighbors: (T) -> Iterable<T>,
    maxIterations: Int,
): List<T>? {
    require(maxIterations > 0) { "maxIterations must be positive." }

    if (canClickPlace(target)) {
        return listOf(target)
    }

    return dijkstraShortestPath(
        start = target,
        isGoal = { current ->
            current != target && hasSolidNeighbor(current) && canClickPlace(current)
        },
        neighbors = { current ->
            neighbors(current).map { neighbor -> WeightedEdge(neighbor, 1.0) }
        },
        maxIterations = maxIterations,
    )?.nodes
}
