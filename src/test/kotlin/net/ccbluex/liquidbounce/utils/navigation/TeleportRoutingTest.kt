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
import net.ccbluex.liquidbounce.utils.navigation.TestNavigationWorld.Companion.STONE
import net.ccbluex.liquidbounce.utils.navigation.TestNavigationWorld.Companion.feet
import net.ccbluex.liquidbounce.utils.navigation.TestNavigationWorld.Companion.flat
import net.ccbluex.liquidbounce.utils.navigation.route.RouteFailure
import net.ccbluex.liquidbounce.utils.navigation.route.RouteLimits
import net.ccbluex.liquidbounce.utils.navigation.route.RouteResult
import net.ccbluex.liquidbounce.utils.navigation.teleport.TeleportPlan
import net.ccbluex.liquidbounce.utils.navigation.teleport.TeleportRoute
import net.ccbluex.liquidbounce.utils.navigation.teleport.TeleportRouteSearch
import net.ccbluex.liquidbounce.utils.navigation.teleport.TeleportStep
import net.ccbluex.liquidbounce.utils.navigation.teleport.TeleportSteps
import net.ccbluex.liquidbounce.utils.navigation.terrain.DestinationCheck
import net.ccbluex.liquidbounce.utils.navigation.terrain.Terrain
import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.CropBlock
import net.minecraft.world.level.block.SlabBlock
import net.minecraft.world.level.block.state.properties.SlabType
import net.minecraft.world.phys.Vec3
import kotlin.math.abs
import kotlin.math.sqrt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TeleportRoutingTest {

    companion object {
        init {
            MinecraftBootstrap.ensureInitialized()
        }

        private const val STEP_LENGTH = 5.0
        private const val MAX_SLICES = 1000
    }

    private fun search(
        terrain: Terrain,
        start: Vec3,
        goal: BlockPos,
        limits: RouteLimits = RouteLimits(maxLength = 80.0),
    ) = TeleportRouteSearch(terrain, start, listOf(goal), limits, STEP_LENGTH).advanceToEnd(MAX_SLICES)

    private fun failure(result: RouteResult<TeleportPlan>) = assertIs<RouteResult.Failed>(result).failure

    /**
     * Follows the fall distance a vanilla server keeps: every descending move adds to it, also one on ground,
     * and a move upwards or on ground clears it, the latter after dealing damage for what exceeds three blocks.
     */
    private fun assertNoFallDamage(start: Vec3, steps: List<TeleportStep>) {
        var fallDistance = 0.0
        var previous = start
        for (step in steps) {
            val dy = step.position.y - previous.y
            if (dy < 0.0) {
                fallDistance -= dy
            }
            assertTrue(fallDistance <= TeleportSteps.MAX_FALL + 1.0E-6, "Fell $fallDistance onto ${step.position}")
            if (step.onGround || dy > 0.0) {
                fallDistance = 0.0
            }
            previous = step.position
        }
    }

    @Test
    fun `standing needs support below and room for the body`() {
        val terrain = flat { x, _ -> x < 30 }.fill(3, 2, 0, 3, 2, 0).terrain()

        assertEquals(DestinationCheck.VALID, terrain.check(BlockPos(0, 1, 0)))
        assertEquals(DestinationCheck.NOT_STANDABLE, terrain.check(BlockPos(0, 2, 0)))
        assertEquals(DestinationCheck.NOT_STANDABLE, terrain.check(BlockPos(3, 1, 0)))
        assertEquals(DestinationCheck.NOT_LOADED, terrain.check(BlockPos(35, 1, 0)))
        assertEquals(DestinationCheck.OUTSIDE_WORLD, terrain.check(BlockPos(0, -70, 0)))
    }

    @Test
    fun `partial blocks carry the feet at their top`() {
        val bottomSlab = Blocks.STONE_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.BOTTOM)
        val terrain = flat()
            .set(0, 1, 0, Blocks.FARMLAND.defaultBlockState())
            .set(2, 1, 0, bottomSlab)
            .set(4, 1, 0, Blocks.MOSS_CARPET.defaultBlockState())
            .set(6, 0, 0, Blocks.OAK_FENCE.defaultBlockState())
            .terrain()

        assertEquals(1.9375, terrain.standHeight(0, 1, 0), 1.0E-6)
        assertEquals(1.5, terrain.standHeight(2, 1, 0), 1.0E-6)
        assertTrue(terrain.standHeight(2, 2, 0).isNaN(), "Stood above a bottom slab")
        assertEquals(1.0625, terrain.standHeight(4, 1, 0), 1.0E-6)
        assertEquals(1.5, terrain.standHeight(6, 1, 0), 1.0E-6)
    }

    @Test
    fun `lava and cobwebs are never walked into`() {
        val terrain = flat()
            .set(0, 1, 0, Blocks.COBWEB.defaultBlockState())
            .set(2, 0, 0, Blocks.MAGMA_BLOCK.defaultBlockState())
            .set(4, 0, 0, Blocks.LAVA.defaultBlockState())
            .terrain()

        assertFalse(terrain.isStandable(BlockPos(0, 1, 0)))
        assertFalse(terrain.isStandable(BlockPos(2, 1, 0)))
        assertFalse(terrain.isStandable(BlockPos(4, 1, 0)))
        assertFalse(terrain.sweep(feet(-2, 1, 0), feet(1, 1, 0)))
    }

    @Test
    fun `sweep follows the server's axis order`() {
        val start = feet(0, 1, 0)
        val end = Vec3(3.5, 1.0, 1.5)

        // X is the longer axis, so the server moves along X first: the block on that leg stops it.
        assertFalse(flat().set(3, 1, 0, STONE).terrain().sweep(start, end))

        // The opposite corner is only passed by a Z-first move.
        assertTrue(flat().set(0, 1, 1, STONE).terrain().sweep(start, end))
    }

    @Test
    fun `cache only invalidates blocks it looked at that changed shape`() {
        val world = flat().set(1, 1, 0, Blocks.WHEAT.defaultBlockState())
        val terrain = world.terrain()
        assertTrue(terrain.isStandable(BlockPos(0, 1, 0)))
        assertTrue(terrain.isStandable(BlockPos(1, 1, 0)))

        world.set(10, 5, 10, STONE)
        assertFalse(terrain.invalidate(BlockPos(10, 5, 10), STONE))

        val grown = Blocks.WHEAT.defaultBlockState().setValue(CropBlock.AGE, CropBlock.MAX_AGE)
        world.set(1, 1, 0, grown)
        assertFalse(terrain.invalidate(BlockPos(1, 1, 0), grown))

        world.set(0, 2, 0, STONE)
        assertTrue(terrain.invalidate(BlockPos(0, 2, 0), STONE))
        assertFalse(terrain.isStandable(BlockPos(0, 1, 0)))
    }

    @Test
    fun `drops land on ground after crossing at the top, rises go first`() {
        val terrain = flat().fill(0, 1, 0, 0, 5, 0).terrain()
        val top = feet(0, 6, 0)
        val bottom = feet(6, 1, 0)

        val down = assertNotNull(TeleportSteps.plan(terrain, TeleportRoute(listOf(top, bottom)), STEP_LENGTH))
        assertEquals(bottom, down.last().position)
        assertTrue(down.last().onGround)
        val crossing = down.takeWhile { it.position.x != bottom.x }
        assertTrue(crossing.all { it.position.y == top.y }, "Crossed below the ledge before the drop")
        assertNoFallDamage(top, down)

        val rise = TeleportRoute(listOf(feet(-6, 1, 0), feet(-1, 5, 0)))
        val up = assertNotNull(TeleportSteps.plan(terrain, rise, STEP_LENGTH))
        assertTrue(up.first().position.x == -5.5 && up.first().position.y > 1.0, "Did not rise in the start column")
    }

    @Test
    fun `long drops go out in falls the server forgives`() {
        val terrain = flat().fill(0, 1, 0, 0, 19, 0).terrain()
        val top = feet(0, 20, 0)
        val bottom = feet(4, 1, 0)
        val steps = assertNotNull(TeleportSteps.plan(terrain, TeleportRoute(listOf(top, bottom)), STEP_LENGTH))

        assertEquals(bottom, steps.last().position)
        assertTrue(steps.last().onGround, "The landing does not claim ground")
        val drop = steps.dropWhile { it.position.y == top.y }
        assertTrue(drop.all { it.position.x == bottom.x && it.position.z == bottom.z }, "Left the drop column")
        assertTrue(drop.dropLast(1).none { it.onGround }, "Claimed ground in mid-air")

        var previous = top
        for (step in drop) {
            val dy = step.position.y - previous.y
            assertTrue(dy >= -TeleportSteps.MAX_FALL - 1.0E-6, "A packet falls ${-dy} blocks")
            assertTrue(dy < 0.0 || abs(dy - TeleportSteps.BOUNCE) < 1.0E-6, "A packet rises $dy blocks")
            previous = step.position
        }
        assertNoFallDamage(top, steps)

        val back = assertNotNull(TeleportSteps.plan(terrain, TeleportRoute(listOf(bottom, top)), STEP_LENGTH))
        assertEquals(top, back.last().position)
        assertNoFallDamage(bottom, back)
    }

    @Test
    fun `batches stay inside the vanilla movement allowance`() {
        val terrain = flat().terrain()
        val route = TeleportRoute(listOf(feet(-30, 1, 0), feet(30, 1, 0)))
        val steps = assertNotNull(TeleportSteps.plan(terrain, route, 4.0))

        for (perTick in 1..TeleportSteps.MAX_PACKETS_PER_TICK) {
            val batches = TeleportSteps.batches(route.start, steps, perTick)
            assertEquals(steps.size, batches.sum())

            var index = 0
            var tickStart = route.start
            for (size in batches) {
                assertTrue(size in 1..perTick)
                for (packet in 1..size) {
                    val distance = steps[index + packet - 1].position.distanceTo(tickStart)
                    assertTrue(distance <= 10.0 * sqrt(packet.toDouble()), "Packet $packet of a tick moves $distance")
                }
                index += size
                tickStart = steps[index - 1].position
            }
        }
    }

    @Test
    fun `a target far below a tower is reached through the open air beside it`() {
        // The tower top is at feet y 16; a floating wall at x 5 blocks crossing at that height.
        val terrain = flat().fill(0, 1, -2, 2, 15, 2).fill(5, 12, -1, 5, 20, 1).terrain()
        val start = feet(1, 16, 0)
        val plan = found(search(terrain, start, BlockPos(8, 1, 0)))

        assertEquals(feet(8, 1, 0), plan.route.destination)
        val drops = plan.route.points.zipWithNext { a, b -> a.y - b.y }
        assertTrue(drops.any { it > 12.0 }, "No long drop: ${plan.route.points}")
        assertTrue(plan.steps.last().onGround)
        assertNoFallDamage(start, plan.steps)

        val back = assertNotNull(TeleportSteps.plan(terrain, TeleportRoute(plan.route.points.reversed()), STEP_LENGTH))
        assertEquals(start, back.last().position)
        assertNoFallDamage(plan.route.destination, back)
    }

    @Test
    fun `a target on a platform far above is reached by rising beside it`() {
        // A roof over the start keeps it from rising in place; the platform top is at feet y 15.
        val terrain = flat().fill(-2, 4, -2, 2, 4, 2).fill(6, 14, -2, 10, 14, 2).terrain()
        val start = feet(0, 1, 0)
        val plan = found(search(terrain, start, BlockPos(8, 15, 0)))

        assertEquals(feet(8, 15, 0), plan.route.destination)
        val rises = plan.route.points.zipWithNext { a, b -> b.y - a.y }
        assertTrue(rises.any { it > 12.0 }, "No long rise: ${plan.route.points}")
        for (point in plan.route.points.drop(1)) {
            assertTrue(terrain.isStandable(BlockPos.containing(point)), "$point is not standable")
        }
        assertNoFallDamage(start, plan.steps)

        val back = assertNotNull(TeleportSteps.plan(terrain, TeleportRoute(plan.route.points.reversed()), STEP_LENGTH))
        assertEquals(start, back.last().position)
        assertTrue(back.last().onGround)
        assertNoFallDamage(plan.route.destination, back)
    }

    @Test
    fun `open ground goes straight, walls are passed around with few waypoints`() {
        val straight = found(search(flat().terrain(), feet(0, 1, 0), BlockPos(20, 1, 7)))
        assertEquals(listOf(feet(0, 1, 0), feet(20, 1, 7)), straight.route.points)

        val terrain = flat().fill(5, 1, -6, 5, 12, 6).terrain()
        val start = feet(0, 1, 0)
        val plan = found(search(terrain, start, BlockPos(10, 1, 0)))

        val points = plan.route.points
        assertEquals(start, points.first())
        assertEquals(feet(10, 1, 0), points.last())
        assertTrue(points.size in 3..5, "Expected a compact route, got $points")
        assertTrue(points.all { abs(it.z) > 6.0 || it.x < 5.0 || it.x > 6.0 }, "A waypoint is inside the wall")
        assertNotNull(TeleportSteps.plan(terrain, plan.route, STEP_LENGTH))
    }

    @Test
    fun `arriving, missing and impossible destinations are told apart`() {
        // A standable cell walled in on every side.
        val terrain = flat().fill(19, 1, -1, 21, 3, 1).fill(20, 1, 0, 20, 2, 0, AIR).terrain()
        val start = feet(0, 1, 0)

        assertEquals(RouteResult.AlreadyThere, search(terrain, start, BlockPos(0, 1, 0)))
        val exhaustive = RouteLimits(maxLength = 30.0, maxExpansions = Int.MAX_VALUE, maxComputeNanos = Long.MAX_VALUE)
        assertEquals(RouteFailure.NO_PATH, failure(search(terrain, start, BlockPos(20, 1, 0), exhaustive)))
        assertEquals(RouteFailure.INVALID_DESTINATION, failure(search(terrain, start, BlockPos(5, 4, 0))))
        assertEquals(
            RouteFailure.TOO_FAR,
            failure(search(terrain, start, BlockPos(30, 1, 0), RouteLimits(maxLength = 10.0))),
        )
    }

    @Test
    fun `search work is spread over slices and capped`() {
        val world = flat().fill(5, 1, -30, 5, 12, 30)
        val start = feet(0, 1, 0)
        val goal = BlockPos(10, 1, 0)

        val limits = RouteLimits(80.0, expansionsPerSlice = 5)
        val sliced = TeleportRouteSearch(world.terrain(), start, listOf(goal), limits, STEP_LENGTH)
        assertNull(sliced.preview())
        assertEquals(RouteResult.Searching, sliced.advance())
        assertEquals(RouteResult.Searching, sliced.advance())
        assertEquals(start, assertNotNull(sliced.preview()).first())

        val capped = RouteLimits(80.0, maxExpansions = 20, expansionsPerSlice = 5)
        assertEquals(RouteFailure.TOO_COMPLEX, failure(search(world.terrain(), start, goal, capped)))
    }

}
