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

package net.ccbluex.liquidbounce.utils.navigation

import it.unimi.dsi.fastutil.longs.LongOpenHashSet
import net.ccbluex.liquidbounce.test.MinecraftBootstrap
import net.ccbluex.liquidbounce.utils.navigation.TestNavigationWorld.Companion.AIR
import net.ccbluex.liquidbounce.utils.navigation.TestNavigationWorld.Companion.feet
import net.ccbluex.liquidbounce.utils.navigation.flight.FlightAbilities
import net.ccbluex.liquidbounce.utils.navigation.flight.FlightControl
import net.ccbluex.liquidbounce.utils.navigation.flight.FlightPath
import net.ccbluex.liquidbounce.utils.navigation.flight.FlightRouteSearch
import net.ccbluex.liquidbounce.utils.navigation.flight.isClearLine
import net.ccbluex.liquidbounce.utils.navigation.route.NearGoal
import net.ccbluex.liquidbounce.utils.navigation.route.RouteLimits
import net.ccbluex.liquidbounce.utils.navigation.route.RouteResult
import net.ccbluex.liquidbounce.utils.navigation.route.RouteSearch
import net.ccbluex.liquidbounce.utils.navigation.walk.WalkAbilities
import net.ccbluex.liquidbounce.utils.navigation.walk.WalkPath
import net.ccbluex.liquidbounce.utils.navigation.walk.WalkRouteSearch
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.phys.Vec3
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class FlightRoutingTest {

    companion object {
        init {
            MinecraftBootstrap.ensureInitialized()
        }

        private val LIMITS = RouteLimits(maxLength = 64.0, maxExpansions = 20_000, maxComputeNanos = Long.MAX_VALUE)

        private val CREATIVE = FlightAbilities(FlightControl.MOMENTUM, 1.089, 0.375, isFlying = false)

        private val WALKER = WalkAbilities(
            canBreak = false,
            canPlace = false,
            placeableBlocks = 0,
            canParkour = true,
            canSprint = true,
            safeFall = 3,
            waterFall = 20,
            breakTicks = { Double.POSITIVE_INFINITY },
            avoid = LongOpenHashSet(),
        )
    }

    private fun <T> run(search: RouteSearch<T>): RouteResult<T> {
        var result = search.advance()
        repeat(1000) {
            if (result != RouteResult.Searching) {
                return result
            }
            result = search.advance()
        }
        return result
    }

    private fun fly(world: TestNavigationWorld, start: Vec3, goal: Vec3): FlightPath {
        val result = run(FlightRouteSearch(world.terrain(), start, NearGoal(goal, 1.0), CREATIVE, LIMITS))
        val path = assertIs<RouteResult.Found<FlightPath>>(result).value
        assertTrue(path.complete, "the flight should reach the goal")
        assertTrue(path.end.distanceTo(goal) <= 1.0, "the flight ends at ${path.end}, away from $goal")

        val terrain = world.terrain()
        for ((from, to) in path.points.zipWithNext()) {
            assertTrue(isClearLine(terrain, from, to), "the line from $from to $to is blocked")
        }
        return path
    }

    private fun walk(world: TestNavigationWorld, start: Vec3, goal: Vec3): RouteResult<WalkPath> =
        run(WalkRouteSearch(world.terrain(), start, NearGoal(goal, 1.0), WALKER, LIMITS))

    private fun FlightPath.length() = points.zipWithNext().sumOf { (from, to) -> from.distanceTo(to) }

    @Test
    fun `canyons are flown straight across`() {
        val world = TestNavigationWorld.flat().fill(5, 0, -40, 14, 0, 40, AIR)
        val start = feet(0, 1, 0)
        val goal = feet(20, 1, 0)

        val path = fly(world, start, goal)
        assertEquals(2, path.points.size, "a clear line needs no corner: ${path.points}")

        val walked = walk(world, start, goal)
        assertFalse(walked is RouteResult.Found && walked.value.complete, "the canyon cannot be walked across")
    }

    @Test
    fun `tall walls are flown over along few straight lines`() {
        val world = TestNavigationWorld.flat().fill(10, 1, -64, 10, 12, 64)
        val start = feet(0, 1, 0)
        val goal = feet(20, 1, 0)

        val path = fly(world, start, goal)
        assertTrue(path.points.any { it.y >= 13.0 }, "the route stays below the top of the wall: ${path.points}")
        assertTrue(path.points.size <= 6, "the route turns too often: ${path.points}")
        // Up the wall, over it and down again, with little more than the detour the wall forces.
        assertTrue(path.length() <= 1.25 * (2.0 * 12.0 + 20.0), "the route is ${path.length()} blocks long")

        val walked = walk(world, start, goal)
        assertFalse(walked is RouteResult.Found && walked.value.complete, "the wall cannot be walked over")
    }

    @Test
    fun `hazards are flown over`() {
        val world = TestNavigationWorld.flat().fill(5, 1, -40, 15, 1, 40, Blocks.FIRE.defaultBlockState())

        // Every line of the route is checked to keep out of the fire.
        val path = fly(world, feet(0, 1, 0), feet(20, 1, 0))
        assertTrue(path.points.size > 2, "the straight line through the fire is taken")
    }

    @Test
    fun `walking routes stay as they were without flight`() {
        val world = TestNavigationWorld.flat().fill(5, 1, -5, 5, 3, 5)
        val walked = assertIs<RouteResult.Found<WalkPath>>(walk(world, feet(0, 1, 0), feet(10, 1, 0))).value

        assertTrue(walked.complete)
        assertTrue(walked.points.all { it.y == 1.0 }, "the walk leaves the ground: ${walked.points}")
        assertTrue(walked.steps.none { it.to.x == 5 && it.to.z in -5..5 })
        assertTrue(walked.end.distanceTo(feet(10, 1, 0)) <= 1.0, "the walk ends at ${walked.end}")
    }

}
