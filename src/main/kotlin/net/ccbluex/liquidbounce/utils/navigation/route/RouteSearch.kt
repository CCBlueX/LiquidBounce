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
import net.ccbluex.liquidbounce.utils.navigation.terrain.NavigationWorld
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
 * [goal], or a stage on the way to it for goals beyond the searched area: feet at least [range] blocks from [start]
 * horizontally or next to blocks [terrain] does not know, at least [MIN_PROGRESS] blocks closer to [goal] than
 * [start]. As the search heads for [goal], the first stage it reaches has the cheapest estimated way on to [goal];
 * as every stage gets closer, consecutive stages never lead back.
 *
 * [distance] measures to [goal], so it can be more than the distance to the nearest stage.
 */
class StageGoal(
    val goal: NavigationGoal,
    private val start: Vec3,
    private val range: Double,
    private val terrain: NavigationTerrain,
) : NavigationGoal {

    private val maxStageDistance = goal.distance(BlockPos.containing(start)) - MIN_PROGRESS

    override val ends: Collection<Vec3>
        get() = goal.ends

    override fun isGoal(x: Int, y: Int, z: Int) =
        goal.isGoal(x, y, z) || isStage(x, y, z) && goal.distance(x, y, z) <= maxStageDistance

    override fun distance(x: Int, y: Int, z: Int) = goal.distance(x, y, z)

    private fun isStage(x: Int, y: Int, z: Int): Boolean {
        val dx = x + 0.5 - start.x
        val dz = z + 0.5 - start.z
        return dx * dx + dz * dz >= range * range ||
            !terrain.isKnown(x + 1, y, z) || !terrain.isKnown(x - 1, y, z) ||
            !terrain.isKnown(x, y, z + 1) || !terrain.isKnown(x, y, z - 1)
    }

    companion object {
        private const val MIN_PROGRESS = 2.0

        /**
         * Every block a stage search from [start] with [range] can pass, with at most [verticalMargin] blocks
         * above or below.
         */
        fun region(start: Vec3, range: Double, verticalMargin: Int) =
            NavigationRegion.around(start, emptyList(), range, verticalMargin)

        /**
         * Whether [goal] is farther than [range] from [start] or in blocks [world] does not know, so a walk there
         * goes in stages.
         */
        fun isNeeded(world: NavigationWorld, start: Vec3, goal: Vec3, range: Double): Boolean {
            val feet = BlockPos.containing(goal)
            return start.distanceTo(goal) > range || !world.isKnown(feet.x, feet.y, feet.z)
        }
    }

}

/**
 * Creates the search for one request against a terrain of the request's region.
 */
fun interface RouteSearchFactory<out T> {
    fun create(terrain: NavigationTerrain): RouteSearch<T>
}
