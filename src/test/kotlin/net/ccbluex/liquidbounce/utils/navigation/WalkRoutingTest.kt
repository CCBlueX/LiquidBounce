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
import it.unimi.dsi.fastutil.longs.LongSet
import net.ccbluex.liquidbounce.test.MinecraftBootstrap
import net.ccbluex.liquidbounce.utils.navigation.TestNavigationWorld.Companion.AIR
import net.ccbluex.liquidbounce.utils.navigation.TestNavigationWorld.Companion.feet
import net.ccbluex.liquidbounce.utils.navigation.route.BlockGoal
import net.ccbluex.liquidbounce.utils.navigation.route.NearGoal
import net.ccbluex.liquidbounce.utils.navigation.route.RouteLimits
import net.ccbluex.liquidbounce.utils.navigation.route.RouteResult
import net.ccbluex.liquidbounce.utils.navigation.walk.WalkAbilities
import net.ccbluex.liquidbounce.utils.navigation.walk.WalkMove
import net.ccbluex.liquidbounce.utils.navigation.walk.WalkPath
import net.ccbluex.liquidbounce.utils.navigation.walk.WalkRouteSearch
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.LadderBlock
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.Vec3
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class WalkRoutingTest {

    companion object {
        init {
            MinecraftBootstrap.ensureInitialized()
        }

        private val LIMITS = RouteLimits(maxLength = 64.0, maxExpansions = 20_000, maxComputeNanos = Long.MAX_VALUE)
    }

    private fun abilities(
        canBreak: Boolean = false,
        canPlace: Boolean = false,
        canParkour: Boolean = false,
        safeFall: Int = 3,
        avoid: LongSet = LongOpenHashSet(),
    ) = WalkAbilities(
        canBreak = canBreak,
        canPlace = canPlace,
        placeableBlocks = if (canPlace) 64 else 0,
        canParkour = canParkour,
        canSprint = true,
        safeFall = safeFall,
        waterFall = 20,
        breakTicks = { 20.0 },
        avoid = avoid,
    )

    private fun search(
        world: TestNavigationWorld,
        start: Vec3,
        goal: BlockPos,
        abilities: WalkAbilities = abilities(),
    ): RouteResult<WalkPath> {
        val search = WalkRouteSearch(world.terrain(), start, BlockGoal(listOf(goal)), abilities, LIMITS)
        var result = search.advance()
        repeat(1000) {
            if (result != RouteResult.Searching) {
                return result
            }
            result = search.advance()
        }
        return result
    }

    private fun completePath(result: RouteResult<WalkPath>): WalkPath {
        val path = assertIs<RouteResult.Found<WalkPath>>(result).value
        assertTrue(path.complete, "path should reach the goal")
        return path
    }

    private fun isComplete(result: RouteResult<WalkPath>) =
        result is RouteResult.Found && result.value.complete

    private fun WalkPath.moves() = steps.map { it.move }.toSet()

    @Test
    fun `flat goal is reached with a complete path`() {
        val path = completePath(search(TestNavigationWorld.flat(), feet(0, 1, 0), BlockPos(10, 1, 0)))

        assertEquals(BlockPos(0, 1, 0), path.steps.first().from)
        assertEquals(BlockPos(10, 1, 0), path.steps.last().to)
        assertEquals(feet(10, 1, 0), path.end)
        assertTrue(path.steps.all { it.breaks.isEmpty() && it.places.isEmpty() })
    }

    @Test
    fun `walls are walked around`() {
        val world = TestNavigationWorld.flat().fill(5, 1, -5, 5, 2, 5)
        val path = completePath(search(world, feet(0, 1, 0), BlockPos(10, 1, 0)))

        assertTrue(path.steps.none { it.to.x == 5 && abs(it.to.z) <= 5 })
        assertTrue(path.steps.all { it.breaks.isEmpty() && it.places.isEmpty() })
    }

    @Test
    fun `one block steps are jumped up`() {
        val world = TestNavigationWorld.flat().fill(3, 1, -40, 40, 1, 40)
        val path = completePath(search(world, feet(0, 1, 0), BlockPos(10, 2, 0)))

        assertTrue(WalkMove.ASCEND in path.moves())
        assertEquals(feet(10, 2, 0), path.end)
    }

    @Test
    fun `drops of three blocks are taken`() {
        val world = TestNavigationWorld.flat().fill(-40, 1, -40, 0, 3, 40)
        val path = completePath(search(world, feet(-2, 4, 0), BlockPos(5, 1, 0)))

        assertTrue(path.steps.any { it.move == WalkMove.FALL && it.from.y - it.to.y == 3 })
    }

    @Test
    fun `drops of five blocks onto ground are refused`() {
        val world = TestNavigationWorld.flat().fill(-40, 1, -40, 0, 5, 40)

        assertFalse(isComplete(search(world, feet(-2, 6, 0), BlockPos(5, 1, 0))))
    }

    @Test
    fun `drops of five blocks into water are taken`() {
        val world = TestNavigationWorld.flat()
            .fill(-40, 1, -40, 0, 5, 40)
            .fill(1, 1, -40, 3, 1, 40, Blocks.WATER.defaultBlockState())
        val path = completePath(search(world, feet(-2, 6, 0), BlockPos(6, 1, 0)))

        val fall = path.steps.single { it.move == WalkMove.FALL }
        assertEquals(1, fall.to.y)
        assertTrue(world.getBlockState(fall.to).`is`(Blocks.WATER))
    }

    @Test
    fun `two block gaps are jumped only with parkour`() {
        // A trench two blocks deep: a missed jump lands safely but cannot climb out.
        val world = TestNavigationWorld.flat().fill(3, 0, -40, 4, 0, 40, AIR).fill(3, -2, -40, 4, -2, 40)

        val path = completePath(search(world, feet(0, 1, 0), BlockPos(8, 1, 0), abilities(canParkour = true)))
        assertTrue(WalkMove.PARKOUR in path.moves())

        assertFalse(isComplete(search(world, feet(0, 1, 0), BlockPos(8, 1, 0))))
    }

    @Test
    fun `gaps over a deadly drop are never jumped`() {
        val world = TestNavigationWorld.flat().fill(3, 0, -40, 4, 0, 40, AIR)

        assertFalse(isComplete(search(world, feet(0, 1, 0), BlockPos(8, 1, 0), abilities(canParkour = true))))
    }

    @Test
    fun `walls are tunnelled through only when breaking is allowed`() {
        val world = TestNavigationWorld.flat().fill(5, 1, -40, 5, 10, 40)

        val path = completePath(search(world, feet(0, 1, 0), BlockPos(10, 1, 0), abilities(canBreak = true)))
        val breaks = path.steps.flatMap { it.breaks }
        assertTrue(breaks.isNotEmpty())
        assertTrue(breaks.all { it.x == 5 })

        assertFalse(isComplete(search(world, feet(0, 1, 0), BlockPos(10, 1, 0))))
    }

    @Test
    fun `gaps are bridged only when placing is allowed`() {
        val world = TestNavigationWorld.flat().fill(3, 0, -40, 5, 0, 40, AIR)

        val path = completePath(search(world, feet(0, 1, 0), BlockPos(8, 1, 0), abilities(canPlace = true)))
        val places = path.steps.flatMap { it.places }
        assertEquals(3, places.size)
        assertTrue(places.all { it.y == 0 && it.x in 3..5 })

        assertFalse(isComplete(search(world, feet(0, 1, 0), BlockPos(8, 1, 0))))
    }

    @Test
    fun `three block cliffs are pillared up`() {
        val world = TestNavigationWorld.flat().fill(3, 1, -40, 40, 3, 40)
        val path = completePath(search(world, feet(0, 1, 0), BlockPos(6, 4, 0), abilities(canPlace = true)))

        assertTrue(WalkMove.PILLAR in path.moves())
        assertTrue(path.steps.flatMap { it.places }.isNotEmpty())
    }

    @Test
    fun `ladders are climbed`() {
        val ladder = Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, Direction.NORTH)
        val world = TestNavigationWorld.flat()
            .fill(-40, 1, 1, 40, 4, 3)
            .fill(0, 1, 0, 0, 4, 0, ladder)
        val path = completePath(search(world, feet(0, 1, -3), BlockPos(0, 5, 2)))

        assertTrue(WalkMove.CLIMB_UP in path.moves())
        assertTrue(path.steps.all { it.breaks.isEmpty() && it.places.isEmpty() })
    }

    @Test
    fun `unreachable goals give an incomplete path that ends closer`() {
        val world = TestNavigationWorld.flat()
            .fill(8, 1, -2, 12, 3, 2)
            .fill(10, 1, 0, 10, 2, 0, AIR)
        val goal = BlockPos(10, 1, 0)

        val path = assertIs<RouteResult.Found<WalkPath>>(search(world, feet(0, 1, 0), goal)).value
        assertFalse(path.complete)
        val last = path.steps.last().to
        assertTrue(last.distSqr(goal) < BlockPos(0, 1, 0).distSqr(goal))
    }

    @Test
    fun `avoided blocks change the path`() {
        val avoid = LongOpenHashSet()
        for (x in 1..9) {
            avoid.add(BlockPos.asLong(x, 1, 0))
        }

        val path = completePath(
            search(TestNavigationWorld.flat(), feet(0, 1, 0), BlockPos(10, 1, 0), abilities(avoid = avoid))
        )
        assertTrue(path.steps.none { avoid.contains(it.to.asLong()) })
    }

    @Test
    fun `blocks below gravel are never broken`() {
        val world = TestNavigationWorld.flat()
            .fill(5, 1, -40, 5, 2, 40)
            .fill(5, 3, -40, 5, 3, 40, Blocks.GRAVEL.defaultBlockState())

        val result = search(world, feet(0, 1, 0), BlockPos(10, 1, 0), abilities(canBreak = true))
        assertFalse(isComplete(result))
        if (result is RouteResult.Found) {
            val breaks = result.value.steps.flatMap { it.breaks }
            assertTrue(breaks.none { world.getBlockState(it.above()).`is`(Blocks.GRAVEL) })
        }
    }

    @Test
    fun `a point resting on farmland is reached`() {
        val world = TestNavigationWorld.flat().fill(5, 0, -1, 7, 0, 1, Blocks.FARMLAND.defaultBlockState())
        val item = Vec3(6.7, 0.9375, 0.8)
        val search = WalkRouteSearch(world.terrain(), feet(0, 1, 0), NearGoal(item, 0.75), abilities(), LIMITS)
        var result = search.advance()
        while (result == RouteResult.Searching) {
            result = search.advance()
        }

        val path = completePath(result)
        assertTrue(path.end.distanceTo(item) <= 0.75, "the path ends at ${path.end}, away from $item")
    }

    @Test
    fun `short detours around soul sand are preferred`() {
        val soulSand = Blocks.SOUL_SAND.defaultBlockState()
        val world = TestNavigationWorld.flat().fill(3, 0, -2, 7, 0, 2, soulSand)
        val path = completePath(search(world, feet(0, 1, 0), BlockPos(10, 1, 0)))

        assertTrue(path.steps.none { world.getBlockState(it.to) == soulSand })
    }

    @Test
    fun `honey is no floor to jump up from`() {
        fun world(floor: BlockState) = TestNavigationWorld.flat()
            .fill(-40, 0, -40, 2, 0, 40, floor)
            .fill(3, 1, -40, 40, 1, 40, Blocks.FARMLAND.defaultBlockState())
        val start = Vec3(0.5, 0.875, 0.5)
        val goal = BlockPos(10, 1, 0)

        val path = completePath(search(world(Blocks.SOUL_SAND.defaultBlockState()), start, goal))
        assertTrue(WalkMove.ASCEND in path.moves())

        assertFalse(isComplete(search(world(Blocks.HONEY_BLOCK.defaultBlockState()), start, goal)))
    }

    @Test
    fun `ice is crossed like plain ground`() {
        val ice = Blocks.ICE.defaultBlockState()
        val world = TestNavigationWorld.flat().fill(3, 0, -2, 7, 0, 2, ice)
        val path = completePath(search(world, feet(0, 1, 0), BlockPos(10, 1, 0)))
        val plain = completePath(search(TestNavigationWorld.flat(), feet(0, 1, 0), BlockPos(10, 1, 0)))

        assertTrue(path.steps.any { world.getBlockState(it.to.below()) == ice })
        assertEquals(plain.cost, path.cost, 1.0E-9)
    }

}
