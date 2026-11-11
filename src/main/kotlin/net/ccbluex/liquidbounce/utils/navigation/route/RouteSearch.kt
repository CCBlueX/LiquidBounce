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

import net.minecraft.world.phys.Vec3
import java.util.concurrent.TimeUnit

enum class RouteFailure {
    INVALID_DESTINATION,
    TOO_FAR,
    NO_PATH,
    TOO_COMPLEX,
    TIMED_OUT,
}

sealed interface RouteResult<out T> {

    data object Searching : RouteResult<Nothing>

    data object AlreadyThere : RouteResult<Nothing>

    class Found<T>(val value: T) : RouteResult<T>

    data class Failed(val failure: RouteFailure) : RouteResult<Nothing>

}

/** A search that is advanced in slices until it has a result. */
interface RouteSearch<out T> {

    fun advance(): RouteResult<T>

    /** Feet positions from the start to the explored spot closest to the goal; read between slices. */
    fun preview(): List<Vec3>? = null

}

class RouteLimits(
    /** In blocks along the path. */
    val maxLength: Double,
    val maxWaypoints: Int = 24,
    val maxExpansions: Int = 8_000,
    val expansionsPerSlice: Int = 400,
    val maxComputeNanos: Long = TimeUnit.MILLISECONDS.toNanos(150),
)
