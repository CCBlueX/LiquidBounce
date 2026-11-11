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

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.events.BlockChangeEvent
import net.ccbluex.liquidbounce.event.events.ChunkDeltaUpdateEvent
import net.ccbluex.liquidbounce.event.events.WorldChangeEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.utils.client.logger
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.navigation.terrain.NavigationRegion
import net.ccbluex.liquidbounce.utils.navigation.terrain.NavigationWorld
import net.ccbluex.liquidbounce.utils.navigation.terrain.SectionCache
import net.ccbluex.liquidbounce.utils.navigation.terrain.Terrain
import net.minecraft.core.BlockPos
import net.minecraft.world.phys.Vec3
import java.util.concurrent.CompletableFuture

/**
 * Runs one route search at a time off the game thread, a slice per [poll]. The search reads a copy of its region
 * through the terrain [newTerrain] makes, and starts over when a block it looked at changes.
 */
class BackgroundSearch<T>(
    private val owner: EventListener,
    private val newTerrain: (NavigationWorld) -> Terrain = ::Terrain,
) : EventListener {

    private var job: Job<T>? = null
    private val changes = ArrayList<BlockPos>()
    private var changesOverflowed = false

    val isIdle: Boolean
        get() = job == null

    /** The best route of the running search so far, see [RouteSearch.preview]. */
    var preview: List<Vec3>? = null
        private set

    /** Searches with [newSearch] over [region], unless a search for an equal [key] is running or done. */
    fun request(key: Any, region: NavigationRegion, newSearch: (Terrain) -> RouteSearch<T>) {
        if (job?.key == key) {
            return
        }

        cancel()
        job = start(key, region, newSearch)
    }

    /** Collects the last slice and starts the next one. Call once per tick from the game thread. */
    fun poll(): RouteResult<T> {
        var job = job ?: return RouteResult.Searching
        job.polls++

        job.slice?.let { slice ->
            if (!slice.isDone) {
                return RouteResult.Searching
            }
            job.result = slice.join()
            job.slice = null
            if (job.result == RouteResult.Searching) {
                job.search.preview()?.let { preview = it }
            }
        }

        // Blocks changing all the time, like a command block running every tick, would restart the search forever.
        if (job.polls >= MIN_SEARCH_POLLS && applyChanges(job.terrain)) {
            job = start(job.key, job.region, job.newSearch) ?: run {
                cancel()
                return RouteResult.Searching
            }
            this.job = job
        }

        if (job.result == RouteResult.Searching) {
            job.slice = CompletableFuture.supplyAsync(job.search::advance, Dispatchers.Default.asExecutor())
                .exceptionally { throwable ->
                    logger.error("Route search failed", throwable)
                    RouteResult.Failed(RouteFailure.NO_PATH)
                }
        }
        return job.result
    }

    fun cancel() {
        job?.slice?.cancel(false)
        job = null
        preview = null
        changes.clear()
        changesOverflowed = false
    }

    private fun start(key: Any, region: NavigationRegion, newSearch: (Terrain) -> RouteSearch<T>): Job<T>? {
        val level = mc.level ?: return null
        val terrain = newTerrain(SectionCache.capture(level, region))
        return Job(key, region, newSearch, terrain, newSearch(terrain))
    }

    /** Whether a recorded change alters how a block the search looked at is moved through. */
    private fun applyChanges(terrain: Terrain): Boolean {
        val level = mc.level ?: return false

        var affected = changesOverflowed
        for (pos in changes) {
            affected = terrain.invalidate(pos, level.getBlockState(pos)) || affected
        }
        changes.clear()
        changesOverflowed = false
        return affected
    }

    private fun recordChange(pos: BlockPos) {
        val region = job?.region ?: return
        if (changesOverflowed || pos !in region) {
            return
        }

        if (changes.size >= MAX_PENDING_CHANGES) {
            changesOverflowed = true
            changes.clear()
        } else {
            changes += pos
        }
    }

    @Suppress("unused")
    private val blockChangeHandler = handler<BlockChangeEvent> { event ->
        recordChange(event.blockPos)
    }

    @Suppress("unused")
    private val chunkDeltaHandler = handler<ChunkDeltaUpdateEvent> { event ->
        event.packet.runUpdates { pos, _ -> recordChange(pos.immutable()) }
    }

    @Suppress("unused")
    private val worldChangeHandler = handler<WorldChangeEvent> {
        cancel()
    }

    override fun parent() = owner

    private class Job<T>(
        val key: Any,
        val region: NavigationRegion,
        val newSearch: (Terrain) -> RouteSearch<T>,
        val terrain: Terrain,
        val search: RouteSearch<T>,
    ) {
        var slice: CompletableFuture<RouteResult<T>>? = null
        var result: RouteResult<T> = RouteResult.Searching
        var polls = 0
    }

    private companion object {
        const val MAX_PENDING_CHANGES = 1024
        const val MIN_SEARCH_POLLS = 20
    }

}
