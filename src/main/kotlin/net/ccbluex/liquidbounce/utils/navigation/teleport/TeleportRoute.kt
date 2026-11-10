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

package net.ccbluex.liquidbounce.utils.navigation.teleport

import net.minecraft.world.phys.Vec3
import kotlin.math.max

/**
 * Feet positions from where the player is to the destination. Between two points the player first rises,
 * then moves horizontally at the higher level, then drops.
 */
class TeleportRoute(val points: List<Vec3>) {

    init {
        require(points.size >= 2) { "A route needs a start and a destination." }
    }

    val start: Vec3 get() = points.first()

    val destination: Vec3 get() = points.last()

    /**
     * Corners of the path the player actually takes.
     */
    val path: List<Vec3> by lazy {
        buildList {
            add(start)
            for (index in 0 until points.lastIndex) {
                val from = points[index]
                val to = points[index + 1]
                val top = max(from.y, to.y)

                if (top > from.y) add(Vec3(from.x, top, from.z))
                add(Vec3(to.x, top, to.z))
                if (top > to.y) add(to)
            }
        }
    }

    val length: Double by lazy { path.zipWithNext(Vec3::distanceTo).sum() }

    companion object {
        internal const val SAME_POSITION_SQ = 1.0E-6
    }

}

/**
 * A route and the position packets that travel it.
 */
class TeleportPlan(val route: TeleportRoute, val steps: List<TeleportStep>)
