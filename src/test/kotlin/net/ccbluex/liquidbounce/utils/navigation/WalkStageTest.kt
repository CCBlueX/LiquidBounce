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
import net.ccbluex.liquidbounce.utils.navigation.route.NearGoal
import net.ccbluex.liquidbounce.utils.navigation.route.RouteLimits
import net.ccbluex.liquidbounce.utils.navigation.route.RouteResult
import net.ccbluex.liquidbounce.utils.navigation.route.StageGoal
import net.ccbluex.liquidbounce.utils.navigation.walk.WalkAbilities
import net.ccbluex.liquidbounce.utils.navigation.walk.WalkPath
import net.ccbluex.liquidbounce.utils.navigation.walk.WalkRouteSearch
import net.minecraft.core.BlockPos
import net.minecraft.world.phys.Vec3
import kotlin.math.abs
import kotlin.math.sqrt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Goals beyond the path range or in unknown blocks are walked to in stages.
 */
class WalkStageTest {

    companion object {
        init {
            MinecraftBootstrap.ensureInitialized()
        }

        private const val RANGE = 32.0
        private val LIMITS = RouteLimits(maxLength = RANGE, maxExpansions = 20_000, maxComputeNanos = Long.MAX_VALUE)
        private val START = feet(0, 1, 0)

        /**
         * A stone floor at y = 0 from x -10 to 200 and z -40 to 40.
         */
        private fun field(known: (x: Int, z: Int) -> Boolean = { _, _ -> true }) =
            TestNavigationWorld(known).fill(-10, 0, -40, 200, 0, 40)
    }

    private val abilities = WalkAbilities(
        canBreak = false,
        canPlace = false,
        placeableBlocks = 0,
        canParkour = false,
        canSprint = true,
        safeFall = 3,
        waterFall = 20,
        breakTicks = { Double.POSITIVE_INFINITY },
        avoid = LongOpenHashSet(),
    )

    private fun stage(world: TestNavigationWorld, goal: BlockPos): RouteResult<WalkPath> {
        val terrain = world.terrain()
        val stageGoal = StageGoal(NearGoal(Vec3.atBottomCenterOf(goal), 1.0), START, RANGE, terrain)
        val search = WalkRouteSearch(terrain, START, stageGoal, abilities, LIMITS)
        var result = search.advance()
        repeat(1000) {
            if (result != RouteResult.Searching) {
                return result
            }
            result = search.advance()
        }
        return result
    }

    private fun stageEnd(world: TestNavigationWorld, goal: BlockPos): BlockPos =
        assertIs<RouteResult.Found<WalkPath>>(stage(world, goal)).value.steps.last().to

    private fun horizontalDistance(pos: BlockPos, to: Vec3): Double {
        val dx = pos.x + 0.5 - to.x
        val dz = pos.z + 0.5 - to.z
        return sqrt(dx * dx + dz * dz)
    }

    @Test
    fun `goals beyond the range or not known are walked in stages`() {
        val world = field { x, _ -> x < 100 }

        assertFalse(StageGoal.isNeeded(world, START, feet(20, 1, 0), RANGE))
        assertTrue(StageGoal.isNeeded(world, START, feet(40, 1, 0), RANGE))
        assertTrue(StageGoal.isNeeded(field { x, _ -> x < 10 }, START, feet(20, 1, 0), RANGE))
    }

    @Test
    fun `a far goal gets a stage at the range in its direction`() {
        val end = stageEnd(field(), BlockPos(150, 1, 0))

        assertTrue(horizontalDistance(end, START) >= RANGE)
        assertTrue(horizontalDistance(end, START) < RANGE + 2.0)
        assertTrue(abs(end.z) <= 1, "stage at $end is off the way to the goal")
    }

    @Test
    fun `a stage lies past a wall on the way`() {
        val world = field().fill(10, 1, -30, 10, 3, 10)
        val end = stageEnd(world, BlockPos(150, 1, 0))

        assertTrue(end.x > 10, "stage at $end is still before the wall")
        assertTrue(horizontalDistance(end, START) >= RANGE)
    }

    @Test
    fun `a stage ends where the known blocks end`() {
        val end = stageEnd(field { x, _ -> x < 20 }, BlockPos(25, 1, 0))

        assertEquals(19, end.x)
        assertTrue(abs(end.z) <= 1, "stage at $end is off the way to the goal")
    }

    @Test
    fun `a goal within the stage range is reached`() {
        val path = assertIs<RouteResult.Found<WalkPath>>(stage(field(), BlockPos(20, 1, 0))).value

        assertTrue(NearGoal(feet(20, 1, 0), 1.0).isGoal(path.steps.last().to))
    }

    @Test
    fun `no stage is found when closed in`() {
        val world = field()
            .fill(-3, 1, -3, 1, 3, 3)
            .fill(-2, 1, -2, 0, 3, 2, AIR)

        assertIs<RouteResult.Failed>(stage(world, BlockPos(150, 1, 0)))
    }

    @Test
    fun `stages never lead away from the goal`() {
        // A closed tube from x -3 to 60: its far end is past the range but farther from the goal than the start.
        val world = field()
            .fill(-3, 1, -2, 60, 3, 2)
            .fill(-2, 1, -1, 59, 3, 1, AIR)

        val path = assertIs<RouteResult.Found<WalkPath>>(stage(world, BlockPos(-150, 1, 0))).value
        assertFalse(path.complete)
        assertTrue(path.steps.last().to.x < 0, "walked away from the goal to ${path.steps.last().to}")
    }

}
