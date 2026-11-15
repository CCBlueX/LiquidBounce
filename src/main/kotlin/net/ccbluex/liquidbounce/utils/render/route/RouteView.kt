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

package net.ccbluex.liquidbounce.utils.render.route

import net.ccbluex.liquidbounce.utils.client.player
import net.ccbluex.liquidbounce.utils.entity.interpolateCurrentPosition
import net.minecraft.core.BlockPos
import net.minecraft.world.entity.Entity
import net.minecraft.world.phys.Vec3

enum class RouteState {
    NONE,

    PENDING,

    ACTIVE,
    DONE,

    /** No route, or the server or the world stopped it. */
    INVALID,
}

/** Where a mover is on its route and the corners still ahead. */
interface RouteProgress {

    val isMoving: Boolean

    fun head(partialTicks: Float): Vec3

    val ahead: List<Vec3>

}

/** The player heading straight for [target] while [isMoving]. */
internal class TargetProgress(
    private val moving: () -> Boolean,
    private val target: () -> Vec3?,
) : RouteProgress {

    override val isMoving: Boolean
        get() = moving()

    override val ahead: List<Vec3>
        get() = listOfNotNull(target())

    override fun head(partialTicks: Float): Vec3 = player.interpolateCurrentPosition(partialTicks)

}

/** What a navigating module shows, drawn by [RouteRenderer]. */
class RouteView {

    var state = RouteState.NONE
        private set

    var since = 0L
        private set

    var start: Vec3? = null
        private set

    var destination: Vec3? = null
        private set

    /** Corners of the route, feet positions. */
    var path: List<Vec3>? = null
        private set

    var progress: RouteProgress? = null
        private set

    var breaks: Collection<BlockPos> = emptyList()
    var places: Collection<BlockPos> = emptyList()

    /** Where the server has the player when that differs from the client. */
    var ghost: ((partialTicks: Float) -> Vec3?)? = null

    var target: Entity? = null
    var attackPosition: Vec3? = null

    var engaged = false

    /** A destination the player could pick right now, shown while nothing else is. */
    var preview: Vec3? = null
    var isPreviewValid = false

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
