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

import net.ccbluex.liquidbounce.test.MinecraftBootstrap
import net.ccbluex.liquidbounce.utils.navigation.TestNavigationWorld.Companion.AIR
import net.ccbluex.liquidbounce.utils.navigation.TestNavigationWorld.Companion.feet
import net.ccbluex.liquidbounce.utils.navigation.ride.RideBody
import net.ccbluex.liquidbounce.utils.navigation.ride.RideKind
import net.ccbluex.liquidbounce.utils.navigation.ride.RidePath
import net.ccbluex.liquidbounce.utils.navigation.ride.RideRouteSearch
import net.ccbluex.liquidbounce.utils.navigation.route.BlockGoal
import net.ccbluex.liquidbounce.utils.navigation.route.NavigationGoal
import net.ccbluex.liquidbounce.utils.navigation.route.NearGoal
import net.ccbluex.liquidbounce.utils.navigation.route.RouteLimits
import net.ccbluex.liquidbounce.utils.navigation.route.RouteResult
import net.ccbluex.liquidbounce.utils.navigation.walk.WalkRouteSearch
import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.Vec3
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RideRoutingTest {

    companion object {
        init {
            MinecraftBootstrap.ensureInitialized()
        }

        private val LIMITS = RouteLimits(maxLength = 64.0, maxExpansions = 20_000, maxComputeNanos = Long.MAX_VALUE)
        private const val MAX_SLICES = 1000

        private val WATER: BlockState get() = Blocks.WATER.defaultBlockState()
        private val ICE: BlockState get() = Blocks.PACKED_ICE.defaultBlockState()

        private val BOAT = RideBody(
            kind = RideKind.BOAT,
            width = 1.375,
            height = 1.9,
            stepHeight = 0.0,
            jumpHeight = 0.0,
            safeFall = 1,
            speed = RideBody.BOAT_WATER_SPEED,
            iceSpeed = RideBody.BOAT_ICE_SPEED,
        )

        private fun horse(jump: Double = 0.0) = RideBody(
            kind = RideKind.MOUNT,
            width = 1.3964844,
            height = 2.99,
            stepHeight = 1.0,
            jumpHeight = jump,
            safeFall = 5,
            speed = 0.48,
        )

        /**
         * A lake of water at y = 0 over x -15..15 and z -10..10 on a stone floor, with a stone shore two blocks
         * high around it.
         */
        private fun lake() = TestNavigationWorld()
            .fill(-17, -1, -12, 17, -1, 12)
            .fill(-17, 0, -12, 17, 1, 12)
            .fill(-15, 0, -10, 15, 0, 10, WATER)
            .fill(-15, 1, -10, 15, 1, 10, AIR)

        /** Where a boat floats in the water block [x] 0 [z]. */
        private fun afloat(x: Int, z: Int) = Vec3(x + 0.5, 0.52, z + 0.5)
    }

    private fun search(world: TestNavigationWorld, start: Vec3, goal: NavigationGoal, body: RideBody) =
        RideRouteSearch(world.terrain(), start, goal, body, LIMITS).advanceToEnd(MAX_SLICES)

    private fun completePath(result: RouteResult<RidePath>): RidePath =
        found(result).also { assertTrue(it.complete, "path should reach the goal") }

    @Test
    fun `boats go around an island through the water channel`() {
        // The island reaches the south shore, so only the channel north of it at z 7..10 is open.
        val world = lake().fill(-3, 0, -10, 3, 1, 6)
        val path = completePath(search(world, afloat(-10, 0), NearGoal(Vec3(10.5, 1.0, 0.5), 1.0), BOAT))

        assertTrue(path.points.all { it.node.y == 0 && world.getBlockState(it.node) == WATER })
        assertTrue(path.points.none { it.node.x in -3..3 && it.node.z <= 6 })
        assertTrue(path.points.any { it.node.z >= 7 })
    }

    @Test
    fun `boats pass channels two blocks wide`() {
        val world = lake().fill(-15, 0, -10, 15, 1, 10)
            .fill(-15, 0, 0, 15, 0, 1, WATER)
            .fill(-15, 1, 0, 15, 1, 1, AIR)
        val path = completePath(search(world, afloat(-10, 0), NearGoal(Vec3(10.5, 1.0, 0.5), 1.0), BOAT))

        assertTrue(path.points.all { it.node.z in 0..1 })
    }

    @Test
    fun `boats ride to the shore nearest a goal on land`() {
        val goal = Vec3(20.5, 2.0, 3.5)
        val path = found(search(lake(), afloat(-10, 0), NearGoal(goal, 1.0), BOAT))

        assertTrue(path.exhausted)
        assertEquals(BlockPos(15, 0, 3), path.points.last().node)
    }

    @Test
    fun `boats slide over ice but not over other ground`() {
        val world = TestNavigationWorld.flat().fill(-20, 0, -1, 20, 0, 1, ICE)
        val path = completePath(search(world, feet(-15, 1, 0), NearGoal(Vec3(15.5, 1.0, 0.5), 1.0), BOAT))
        assertTrue(path.points.all { it.node.y == 1 && it.node.z in -1..1 })

        world.fill(0, 0, -1, 0, 0, 1)
        assertFalse(
            (search(world, feet(-15, 1, 0), NearGoal(Vec3(15.5, 1.0, 0.5), 1.0), BOAT) as? RouteResult.Found)
                ?.value?.complete ?: false
        )
    }

    @Test
    fun `horses walk up one block steps and jump ledges only within their jump height`() {
        val step = TestNavigationWorld.flat().fill(3, 1, -40, 40, 1, 40)
        val walked = completePath(search(step, feet(0, 1, 0), BlockGoal(listOf(BlockPos(10, 2, 0))), horse()))
        assertTrue(walked.points.none { it.jump })
        assertEquals(2.0, walked.end.y)

        val ledge = TestNavigationWorld.flat().fill(3, 1, -40, 40, 2, 40)
        val goal = BlockGoal(listOf(BlockPos(10, 3, 0)))
        val jumped = completePath(search(ledge, feet(0, 1, 0), goal, horse(jump = 2.3)))
        assertTrue(jumped.points.any { it.jump })

        val grounded = search(ledge, feet(0, 1, 0), goal, horse())
        assertFalse(grounded is RouteResult.Found && grounded.value.complete)
    }

    @Test
    fun `a doorway two blocks high lets the player through but not a horse`() {
        // A wall across the whole floor with an opening two wide and two high.
        val world = TestNavigationWorld.flat()
            .fill(5, 1, -40, 5, 4, 40)
            .fill(5, 1, 0, 5, 2, 1, AIR)
        val goal = BlockGoal(listOf(BlockPos(10, 1, 0)))

        val ridden = search(world, feet(0, 1, 0), goal, horse())
        assertFalse(ridden is RouteResult.Found && ridden.value.complete)

        val walker = WalkRouteSearch(world.terrain(), feet(0, 1, 0), goal, walkAbilities(breakTicks = 20.0), LIMITS)
        assertTrue(found(walker.advanceToEnd()).complete)

        // Three blocks high, the horse fits, off the middle of the blocks.
        world.set(5, 3, 0, AIR).set(5, 3, 1, AIR)
        completePath(search(world, feet(0, 1, 0), goal, horse()))
    }

}
