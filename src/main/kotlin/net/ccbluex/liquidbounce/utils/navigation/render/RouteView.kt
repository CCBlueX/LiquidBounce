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

package net.ccbluex.liquidbounce.utils.navigation.render

import net.minecraft.core.BlockPos
import net.minecraft.world.entity.Entity
import net.minecraft.world.phys.Vec3

enum class RouteState {
    NONE,

    /**
     * Searching for a route.
     */
    PENDING,

    /**
     * Route found or being travelled.
     */
    ACTIVE,
    DONE,

    /**
     * No route, or the server or the world stopped it.
     */
    INVALID,
}

/**
 * How far along its route a mover is.
 */
interface RouteProgress {

    val isMoving: Boolean

    /**
     * Where the mover is, between the last tick and this one.
     */
    fun head(partialTicks: Float): Vec3

    /**
     * The corners still ahead of [head].
     */
    val ahead: List<Vec3>

}

/**
 * What a navigating module shows: its route, the blocks the route changes, the destination and, for combat,
 * the target it goes for.
 */
class RouteView {

    var state = RouteState.NONE
        private set

    var since = 0L
        private set

    var start: Vec3? = null
        private set

    var destination: Vec3? = null
        private set

    /**
     * Corners of the route, feet positions.
     */
    var path: List<Vec3>? = null
        private set

    var progress: RouteProgress? = null
        private set

    var breaks: Collection<BlockPos> = emptyList()
    var places: Collection<BlockPos> = emptyList()

    /**
     * Where the server has the player when that differs from the client.
     */
    var ghost: ((partialTicks: Float) -> Vec3?)? = null

    var target: Entity? = null
    var attackPosition: Vec3? = null

    /**
     * Whether the module is attacking [target] right now.
     */
    var engaged = false

    /**
     * A destination the player could pick right now, shown while nothing else is.
     */
    var preview: Vec3? = null
    var isPreviewValid = false

    /**
     * Searching from [start] to [destination]; [searched] is the best route so far, see
     * [net.ccbluex.liquidbounce.utils.navigation.route.RoutePlanner.preview].
     */
    fun pending(start: Vec3, destination: Vec3, searched: List<Vec3>? = null) {
        this.start = start
        this.destination = destination
        this.path = searched
        this.progress = null
        change(RouteState.PENDING)
    }

    fun active(path: List<Vec3>, progress: RouteProgress? = null) {
        this.start = path.first()
        this.destination = path.last()
        this.path = path
        this.progress = progress
        change(RouteState.ACTIVE)
    }

    fun done() = change(RouteState.DONE)

    fun invalid() = change(RouteState.INVALID)

    fun clear() {
        start = null
        destination = null
        path = null
        progress = null
        breaks = emptyList()
        places = emptyList()
        target = null
        attackPosition = null
        engaged = false
        change(RouteState.NONE)
    }

    private fun change(state: RouteState) {
        if (this.state != state) {
            this.state = state
            since = System.currentTimeMillis()
        }
    }

}
