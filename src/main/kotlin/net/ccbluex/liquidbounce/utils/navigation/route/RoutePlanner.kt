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
import net.ccbluex.liquidbounce.utils.navigation.terrain.NavigationTerrain
import net.ccbluex.liquidbounce.utils.navigation.terrain.NavigationWorld
import net.ccbluex.liquidbounce.utils.navigation.terrain.SectionCache
import net.minecraft.core.BlockPos
import net.minecraft.world.phys.Vec3
import java.util.concurrent.CompletableFuture

/**
 * Runs route searches for one owner off the game thread, one slice per [poll]. Each search reads a copy of its
 * region, so the world can change while it runs; a change only restarts the search when the search had looked
 * at the changed block. The copy is read through the terrain [newTerrain] makes.
 */
class RoutePlanner<T>(
    private val owner: EventListener,
    private val newTerrain: (NavigationWorld) -> NavigationTerrain = ::NavigationTerrain,
) : EventListener {

    private var key: Any? = null
    private var region: NavigationRegion? = null
    private var factory: RouteSearchFactory<T>? = null
    private var terrain: NavigationTerrain? = null
    private var search: RouteSearch<T>? = null
    private var slice: CompletableFuture<RouteResult<T>>? = null
    private var result: RouteResult<T> = RouteResult.Searching

    private val changes = ArrayList<BlockPos>()
    private var changesOverflowed = false

    val isIdle: Boolean
        get() = search == null

    /**
     * The best route of the running search so far, see [RouteSearch.preview].
     */
    var preview: List<Vec3>? = null
        private set

    /**
     * Searches with [factory] over [region], unless a search for an equal [key] is running or done.
     */
    fun request(key: Any, region: NavigationRegion, factory: RouteSearchFactory<T>) {
        if (search != null && key == this.key) {
            return
        }

        cancel()
        this.key = key
        this.region = region
        this.factory = factory
        start()
    }

    /**
     * Collects the last slice and starts the next one. Call once per tick from the game thread.
     */
    fun poll(): RouteResult<T> {
        if (search == null) {
            return RouteResult.Searching
        }

        slice?.let { running ->
            if (!running.isDone) {
                return RouteResult.Searching
            }
            result = running.join()
            slice = null
            if (result == RouteResult.Searching) {
                search?.preview()?.let { preview = it }
            }
        }

        if (applyChanges()) {
            start()
        }

        val search = search ?: return RouteResult.Searching
        if (result == RouteResult.Searching) {
            slice = CompletableFuture.supplyAsync(search::advance, Dispatchers.Default.asExecutor())
                .exceptionally { throwable ->
                    logger.error("Route search failed", throwable)
                    RouteResult.Failed(RouteFailure.NO_PATH)
                }
        }
        return result
    }

    fun cancel() {
        slice?.cancel(false)
        slice = null
        search = null
        terrain = null
        key = null
        region = null
        factory = null
        result = RouteResult.Searching
        preview = null
        changes.clear()
        changesOverflowed = false
    }

    private fun start() {
        val level = mc.level
        val region = region
        val factory = factory
        if (level == null || region == null || factory == null) {
            cancel()
            return
        }

        val terrain = newTerrain(SectionCache.capture(level, region))
        this.terrain = terrain
        search = factory.create(terrain)
        result = RouteResult.Searching
        slice = null
        changes.clear()
        changesOverflowed = false
    }

    /**
     * True if a changed block was part of the search and changed how it is moved through.
     */
    private fun applyChanges(): Boolean {
        val terrain = terrain ?: return false
        val level = mc.level ?: return false
        if (changes.isEmpty() && !changesOverflowed) {
            return false
        }

        var affected = changesOverflowed
        for (pos in changes) {
            affected = terrain.invalidate(pos, level.getBlockState(pos)) || affected
        }
        changes.clear()
        changesOverflowed = false
        return affected
    }

    private fun recordChange(pos: BlockPos) {
        val region = region ?: return
        if (changesOverflowed || pos.x !in region.minX..region.maxX || pos.y !in region.minY..region.maxY ||
            pos.z !in region.minZ..region.maxZ) {
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

    private companion object {
        const val MAX_PENDING_CHANGES = 1024
    }

}
