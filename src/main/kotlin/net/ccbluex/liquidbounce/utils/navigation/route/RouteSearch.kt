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

import net.ccbluex.liquidbounce.utils.navigation.terrain.NavigationRegion
import net.ccbluex.liquidbounce.utils.navigation.terrain.NavigationTerrain
import net.minecraft.core.BlockPos
import net.minecraft.util.Mth
import net.minecraft.world.phys.Vec3
import java.util.concurrent.TimeUnit
import kotlin.math.sqrt

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

/**
 * A search that is advanced in slices until it has a result.
 */
interface RouteSearch<out T> {

    /**
     * Does one slice of work and returns the result so far.
     */
    fun advance(): RouteResult<T>

    /**
     * Feet positions from the start to the explored spot closest to the goal, while still searching; called
     * between slices.
     */
    fun preview(): List<Vec3>? = null

}

class RouteLimits(
    /**
     * Longest route, in blocks along the path.
     */
    val maxLength: Double,
    val maxWaypoints: Int = 24,
    val maxExpansions: Int = 8_000,
    val expansionsPerSlice: Int = 400,
    val maxComputeNanos: Long = TimeUnit.MILLISECONDS.toNanos(150),
)

/**
 * Where a route should end. Positions are the blocks the feet are in.
 */
interface NavigationGoal {

    fun isGoal(x: Int, y: Int, z: Int): Boolean

    /**
     * Distance in blocks from [x] [y] [z] to the goal; never more than the real distance.
     */
    fun distance(x: Int, y: Int, z: Int): Double

    /**
     * Points the route ends near, used to size the searched region.
     */
    val ends: Collection<Vec3>

    fun isGoal(pos: BlockPos) = isGoal(pos.x, pos.y, pos.z)

    fun distance(pos: BlockPos) = distance(pos.x, pos.y, pos.z)

    fun region(start: Vec3, maxLength: Double, verticalMargin: Int) =
        NavigationRegion.around(start, ends, maxLength, verticalMargin)

}

/**
 * Standing in one of [positions].
 */
class BlockGoal(val positions: Collection<BlockPos>) : NavigationGoal {

    private val set = positions.toHashSet()

    override val ends: Collection<Vec3> = positions.map(Vec3::atBottomCenterOf)

    override fun isGoal(x: Int, y: Int, z: Int) = BlockPos(x, y, z) in set

    override fun distance(x: Int, y: Int, z: Int) = positions.minOf { goal ->
        val dx = (goal.x - x).toDouble()
        val dy = (goal.y - y).toDouble()
        val dz = (goal.z - z).toDouble()
        sqrt(dx * dx + dy * dy + dz * dz)
    }

    override fun equals(other: Any?) = other is BlockGoal && other.set == set

    override fun hashCode() = set.hashCode()

}

/**
 * Feet within [radius] of [center], measured from the center of the feet block and, vertically, from the block
 * holding [center], as feet on a partial block such as farmland rest below its top.
 */
data class NearGoal(val center: Vec3, val radius: Double) : NavigationGoal {

    override val ends: Collection<Vec3> = listOf(center)

    private val centerY = Mth.floor(center.y)

    override fun isGoal(x: Int, y: Int, z: Int) = feetDistance(x, y, z) <= radius

    override fun distance(x: Int, y: Int, z: Int) = (feetDistance(x, y, z) - radius).coerceAtLeast(0.0)

    private fun feetDistance(x: Int, y: Int, z: Int): Double {
        val dx = x + 0.5 - center.x
        val dy = (y - centerY).toDouble()
        val dz = z + 0.5 - center.z
        return sqrt(dx * dx + dy * dy + dz * dz)
    }

}

/**
 * Creates the search for one request against a terrain of the request's region.
 */
fun interface RouteSearchFactory<out T> {
    fun create(terrain: NavigationTerrain): RouteSearch<T>
}
