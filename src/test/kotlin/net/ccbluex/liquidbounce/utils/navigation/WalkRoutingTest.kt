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
import net.ccbluex.liquidbounce.utils.navigation.TestNavigationWorld.Companion.STONE
import net.ccbluex.liquidbounce.utils.navigation.TestNavigationWorld.Companion.feet
import net.ccbluex.liquidbounce.utils.navigation.route.BlockGoal
import net.ccbluex.liquidbounce.utils.navigation.route.NearGoal
import net.ccbluex.liquidbounce.utils.navigation.route.RouteLimits
import net.ccbluex.liquidbounce.utils.navigation.route.RouteResult
import net.ccbluex.liquidbounce.utils.navigation.walk.Abilities
import net.ccbluex.liquidbounce.utils.navigation.walk.Move
import net.ccbluex.liquidbounce.utils.navigation.walk.Path
import net.ccbluex.liquidbounce.utils.navigation.walk.PathSearch
import net.ccbluex.liquidbounce.utils.navigation.walk.isStraightWalkable
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.DoorBlock
import net.minecraft.world.level.block.FenceGateBlock
import net.minecraft.world.level.block.LadderBlock
import net.minecraft.world.level.block.SlabBlock
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf
import net.minecraft.world.level.block.state.properties.SlabType
import net.minecraft.world.phys.Vec3
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WalkRoutingTest {

    companion object {
        init {
            MinecraftBootstrap.ensureInitialized()
        }

        private val LIMITS = RouteLimits(maxLength = 64.0, maxExpansions = 20_000, maxComputeNanos = Long.MAX_VALUE)
        private const val MAX_SLICES = 1000
    }

    private fun abilities(
        canBreak: Boolean = false,
        canPlace: Boolean = false,
        canParkour: Boolean = false,
        avoid: LongSet = LongOpenHashSet(),
    ) = walkAbilities(canBreak, canPlace, canParkour, breakTicks = 20.0, avoid = avoid)

    private fun search(
        world: TestNavigationWorld,
        start: Vec3,
        goal: BlockPos,
        abilities: Abilities = abilities(),
    ): RouteResult<Path> {
        val terrain = world.terrain(opensDoors = true)
        return PathSearch(terrain, start, BlockGoal(goal), abilities, LIMITS).advanceToEnd(MAX_SLICES)
    }

    private fun isComplete(result: RouteResult<Path>) =
        result is RouteResult.Found && result.value.complete

    private fun Path.moves() = steps.map { it.move }.toSet()

    @Test
    fun `walls are walked around`() {
        val world = TestNavigationWorld.flat().fill(5, 1, -5, 5, 2, 5)
        val path = completePath(search(world, feet(0, 1, 0), BlockPos(10, 1, 0)))

        assertEquals(feet(10, 1, 0), path.end)
        assertTrue(path.steps.none { it.to.x == 5 && abs(it.to.z) <= 5 })
        assertTrue(path.steps.all { it.breaks.isEmpty() && it.places.isEmpty() })
    }

    @Test
    fun `one block steps are jumped up`() {
        val world = TestNavigationWorld.flat().fill(3, 1, -40, 40, 1, 40)
        val path = completePath(search(world, feet(0, 1, 0), BlockPos(10, 2, 0)))

        assertTrue(Move.ASCEND in path.moves())
        assertEquals(feet(10, 2, 0), path.end)
    }

    @Test
    fun `drops of three blocks are taken, of five onto ground refused`() {
        fun cliff(height: Int) = TestNavigationWorld.flat().fill(-40, 1, -40, 0, height, 40)

        val path = completePath(search(cliff(3), feet(-2, 4, 0), BlockPos(5, 1, 0)))
        assertTrue(path.steps.any { it.move == Move.FALL && it.from.y - it.to.y == 3 })

        assertFalse(isComplete(search(cliff(5), feet(-2, 6, 0), BlockPos(5, 1, 0))))
    }

    @Test
    fun `drops of five blocks into water are taken`() {
        val world = TestNavigationWorld.flat()
            .fill(-40, 1, -40, 0, 5, 40)
            .fill(1, 1, -40, 3, 1, 40, Blocks.WATER.defaultBlockState())
        val path = completePath(search(world, feet(-2, 6, 0), BlockPos(6, 1, 0)))

        val fall = path.steps.single { it.move == Move.FALL }
        assertEquals(1, fall.to.y)
        assertTrue(world.getBlockState(fall.to).`is`(Blocks.WATER))
    }

    @Test
    fun `two block gaps are jumped only with parkour`() {
        // A trench two blocks deep: a missed jump lands safely but cannot climb out.
        val world = TestNavigationWorld.flat().fill(3, 0, -40, 4, 0, 40, AIR).fill(3, -2, -40, 4, -2, 40)

        val path = completePath(search(world, feet(0, 1, 0), BlockPos(8, 1, 0), abilities(canParkour = true)))
        assertTrue(Move.PARKOUR in path.moves())

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
    fun `leaning over the corner of a bridge, the path starts on the block holding the player`() {
        // A bridge eight blocks up ending at x 5; the feet hang past its corner, held by the last block.
        val world = TestNavigationWorld.flat().fill(0, 8, -1, 4, 8, 1).set(5, 8, 0, STONE)
        val start = Vec3(6.29, 9.0, 1.29)

        val path = completePath(search(world, start, BlockPos(9, 9, 0), abilities(canPlace = true)))
        assertEquals(BlockPos(5, 9, 0), path.steps.first().from)
        assertTrue(path.steps.all { it.to.y == 9 }, "the path leaves the bridge height: ${path.steps}")
    }

    @Test
    fun `three block cliffs are pillared up`() {
        val world = TestNavigationWorld.flat().fill(3, 1, -40, 40, 3, 40)
        val path = completePath(search(world, feet(0, 1, 0), BlockPos(6, 4, 0), abilities(canPlace = true)))

        assertTrue(Move.PILLAR in path.moves())
        assertTrue(path.steps.flatMap { it.places }.isNotEmpty())
    }

    @Test
    fun `ladders are climbed`() {
        val ladder = Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, Direction.NORTH)
        val world = TestNavigationWorld.flat()
            .fill(-40, 1, 1, 40, 4, 3)
            .fill(0, 1, 0, 0, 4, 0, ladder)
        val path = completePath(search(world, feet(0, 1, -3), BlockPos(0, 5, 2)))

        assertTrue(Move.CLIMB_UP in path.moves())
        assertTrue(path.steps.all { it.breaks.isEmpty() && it.places.isEmpty() })
    }

    @Test
    fun `unreachable goals give an incomplete path that ends closer`() {
        val world = TestNavigationWorld.flat()
            .fill(8, 1, -2, 12, 3, 2)
            .fill(10, 1, 0, 10, 2, 0, AIR)
        val goal = BlockPos(10, 1, 0)

        val path = found(search(world, feet(0, 1, 0), goal))
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
    fun `gaps one and a half blocks high are crouched through, one block high walked around`() {
        val slab = Blocks.STONE_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.TOP)
        fun wall() = TestNavigationWorld.flat()
            .fill(5, 1, -20, 5, 3, 20)
            .set(5, 1, 0, AIR)
        val gap = BlockPos(5, 1, 0)

        val path = completePath(search(wall().set(5, 2, 0, slab), feet(0, 1, 0), BlockPos(10, 1, 0)))
        val crouched = path.steps.filter { it.from == gap || it.to == gap }
        assertEquals(2, crouched.size, "the path should lead through the gap")
        assertTrue(crouched.all { it.sneak })
        assertTrue(path.steps.none { it.sneak && it.from != gap && it.to != gap })

        val low = completePath(search(wall(), feet(0, 1, 0), BlockPos(10, 1, 0)))
        assertTrue(low.steps.none { it.to == gap })
    }

    @Test
    fun `narrow bridges over deep drops are walked sneaking`() {
        // Two platforms ten blocks over the ground, joined by a bridge one block wide.
        val world = TestNavigationWorld.flat()
            .fill(-3, 10, -3, 0, 10, 3)
            .fill(1, 10, 0, 8, 10, 0)
            .fill(9, 10, -3, 12, 10, 3)
        val path = completePath(search(world, feet(-2, 11, 0), BlockPos(11, 11, 0)))

        val bridge = path.steps.filter { it.to.x in 1..8 }
        assertTrue(bridge.isNotEmpty() && bridge.all { it.sneak })
        assertFalse(path.steps.first().sneak, "the middle of a platform is no edge")
        assertTrue(path.steps.all { it.to.y == 11 })
    }

    /**
     * Ground down to y = -6 with a wall across it at x -1..1 and a pool four blocks deep on each side of the wall,
     * joined by a flooded tunnel one block high and [length] blocks long along their bottom.
     */
    private fun pools(length: Int): TestNavigationWorld {
        val water = Blocks.WATER.defaultBlockState()
        val west = -length / 2
        val east = west + length - 1
        return TestNavigationWorld()
            .fill(-40, -6, -40, 40, 0, 40)
            .fill(-1, 1, -40, 1, 4, 40)
            .fill(west - 5, -3, -2, west - 1, 0, 2, water)
            .fill(east + 1, -3, -2, east + 5, 0, 2, water)
            .fill(west, -3, 0, east, -3, 0, water)
    }

    @Test
    fun `flooded tunnels one block high are swum through`() {
        val path = completePath(search(pools(8), feet(-12, 1, 0), BlockPos(11, 1, 0)))

        val tunnel = path.steps.filter { it.to.y == -3 && it.to.x in -4..3 }
        assertEquals(8, tunnel.size, "the path should lead through the tunnel")
        assertTrue(tunnel.all { it.underwater && it.move == Move.SWIM })
        assertTrue(path.steps.none { it.underwater && it.to.y > 0 })
    }

    @Test
    fun `flooded tunnels longer than the air lasts are swum through only past air pockets`() {
        assertFalse(isComplete(search(pools(30), feet(-23, 1, 0), BlockPos(22, 1, 0))))

        val world = pools(30).set(-6, -2, 0, AIR).set(6, -2, 0, AIR)
        val path = completePath(search(world, feet(-23, 1, 0), BlockPos(22, 1, 0)))

        val pockets = path.steps.filter { it.to == BlockPos(-6, -3, 0) || it.to == BlockPos(6, -3, 0) }
        assertEquals(2, pockets.size, "the path should lead through both air pockets")
        assertTrue(pockets.none { it.underwater })
    }

    @Test
    fun `straight walks along deep drops are refused`() {
        val high = TestNavigationWorld.flat().fill(0, 10, 0, 10, 10, 0)
        assertFalse(isStraightWalkable(high.terrain(), feet(0, 11, 0), feet(10, 11, 0), 0.5, safeFall = 3))

        val low = TestNavigationWorld.flat().fill(0, 1, 0, 10, 1, 0)
        assertTrue(isStraightWalkable(low.terrain(), feet(0, 2, 0), feet(10, 2, 0), 0.5, safeFall = 3))
    }

    @Test
    fun `a point resting on farmland is reached`() {
        val world = TestNavigationWorld.flat().fill(5, 0, -1, 7, 0, 1, Blocks.FARMLAND.defaultBlockState())
        val item = Vec3(6.7, 0.9375, 0.8)
        val search = PathSearch(world.terrain(), feet(0, 1, 0), NearGoal(item, 0.75), abilities(), LIMITS)

        val path = completePath(search.advanceToEnd())
        assertTrue(path.end.distanceTo(item) <= 0.75, "the path ends at ${path.end}, away from $item")
    }

    @Test
    fun `soul sand is walked around while ice is crossed like plain ground`() {
        val plain = completePath(search(TestNavigationWorld.flat(), feet(0, 1, 0), BlockPos(10, 1, 0)))

        val soulSand = Blocks.SOUL_SAND.defaultBlockState()
        val sandWorld = TestNavigationWorld.flat().fill(3, 0, -2, 7, 0, 2, soulSand)
        val sand = completePath(search(sandWorld, feet(0, 1, 0), BlockPos(10, 1, 0)))
        assertTrue(sand.steps.none { sandWorld.getBlockState(it.to) == soulSand })

        val ice = Blocks.ICE.defaultBlockState()
        val iceWorld = TestNavigationWorld.flat().fill(3, 0, -2, 7, 0, 2, ice)
        val path = completePath(search(iceWorld, feet(0, 1, 0), BlockPos(10, 1, 0)))
        assertTrue(path.steps.any { iceWorld.getBlockState(it.to.below()) == ice })
        assertEquals(plain.cost, path.cost, 1.0E-9)
    }

    @Test
    fun `honey is no floor to jump up from`() {
        fun world(floor: BlockState) = TestNavigationWorld.flat()
            .fill(-40, 0, -40, 2, 0, 40, floor)
            .fill(3, 1, -40, 40, 1, 40, Blocks.FARMLAND.defaultBlockState())
        val start = Vec3(0.5, 0.875, 0.5)
        val goal = BlockPos(10, 1, 0)

        val path = completePath(search(world(Blocks.SOUL_SAND.defaultBlockState()), start, goal))
        assertTrue(Move.ASCEND in path.moves())

        assertFalse(isComplete(search(world(Blocks.HONEY_BLOCK.defaultBlockState()), start, goal)))
    }

    private fun TestNavigationWorld.door(x: Int, z: Int, block: Block) = apply {
        val lower = block.defaultBlockState().setValue(DoorBlock.FACING, Direction.EAST)
        set(x, 1, z, lower)
        set(x, 2, z, lower.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER))
    }

    @Test
    fun `closed wooden doors and fence gates are opened instead of a long detour`() {
        val gate = Blocks.OAK_FENCE_GATE.defaultBlockState().setValue(FenceGateBlock.FACING, Direction.EAST)
        val world = TestNavigationWorld.flat()
            .fill(5, 1, -20, 5, 3, 20)
            .door(5, 0, Blocks.OAK_DOOR)
            .fill(8, 1, -20, 8, 3, 20)
            .set(8, 1, 0, gate)
            .set(8, 2, 0, AIR)
        val path = completePath(search(world, feet(0, 1, 0), BlockPos(12, 1, 0)))

        assertEquals(listOf(BlockPos(5, 1, 0), BlockPos(8, 1, 0)), path.steps.flatMap { it.opens })
    }

    @Test
    fun `iron doors are walked around`() {
        val world = TestNavigationWorld.flat().fill(5, 1, -20, 5, 3, 20).door(5, 0, Blocks.IRON_DOOR)
        val path = completePath(search(world, feet(0, 1, 0), BlockPos(10, 1, 0)))

        assertTrue(path.steps.none { it.to.x == 5 && abs(it.to.z) <= 20 })
        assertTrue(path.steps.all { it.opens.isEmpty() })
    }

    @Test
    fun `trapdoors are opened in walls and walked over on floors`() {
        val trapdoor = Blocks.OAK_TRAPDOOR.defaultBlockState()
        val floor = TestNavigationWorld.flat().set(5, 1, 0, trapdoor)
        val over = completePath(search(floor, feet(0, 1, 0), BlockPos(10, 1, 0)))
        assertTrue(over.steps.any { it.to == BlockPos(5, 1, 0) })
        assertTrue(over.steps.all { it.opens.isEmpty() })

        val wall = TestNavigationWorld.flat()
            .fill(5, 1, -20, 5, 3, 20)
            .set(5, 1, 0, AIR)
            .set(5, 2, 0, trapdoor)
        val through = completePath(search(wall, feet(0, 1, 0), BlockPos(10, 1, 0)))
        assertEquals(listOf(BlockPos(5, 2, 0)), through.steps.flatMap { it.opens })
    }

}
