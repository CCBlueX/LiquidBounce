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

import net.ccbluex.agent.navigation.Listener
import net.ccbluex.agent.navigation.Progress
import net.minecraft.core.BlockPos
import net.minecraft.world.phys.Vec3

/** Fills [view] from what a navigator or a trip reports, for [RouteRenderer]. */
class RouteEvents(private val view: RouteView) : Listener {

    override fun routeSearching(start: Vec3, goal: Vec3, radius: Double, preview: List<Vec3>?) =
        view.pending(start, goal, preview)

    override fun routeFollowing(points: List<Vec3>, progress: Progress) =
        view.active(points, object : RouteProgress {
            override val isMoving get() = progress.isMoving
            override fun head(partialTicks: Float) = progress.head(partialTicks)
            override val ahead get() = progress.ahead
        })

    override fun blocksAhead(breaks: Collection<BlockPos>, places: Collection<BlockPos>) {
        view.breaks = breaks
        view.places = places
    }

    override fun routeDone() = view.done()

    override fun routeInvalid() = view.invalid()

    override fun routeCleared() = view.clear()

}
