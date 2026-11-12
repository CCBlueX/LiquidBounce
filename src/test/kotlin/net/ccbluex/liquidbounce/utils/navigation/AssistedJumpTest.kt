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
import net.ccbluex.liquidbounce.utils.entity.PlayerMotion
import net.ccbluex.liquidbounce.utils.math.ifNaN
import net.ccbluex.liquidbounce.utils.math.yaw
import net.ccbluex.liquidbounce.utils.navigation.walk.AssistedJump
import net.ccbluex.liquidbounce.utils.navigation.walk.JumpTargets
import net.ccbluex.liquidbounce.utils.navigation.walk.Jumps
import net.ccbluex.liquidbounce.utils.navigation.walk.LeapInput
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.EndRodBlock
import net.minecraft.world.level.block.LadderBlock
import net.minecraft.world.phys.Vec3
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * The Parkour assist driving a player who looks at the next block of a course and holds forward and sprint, simulated
 * as the game moves the player and ModuleParkour turns it.
 */
class AssistedJumpTest {

    companion object {
        init {
            MinecraftBootstrap.ensureInitialized()
        }

        private const val MAX_ANGLE = 35f
        private const val TICKS_PER_BLOCK = 60
        private const val MAX_JUMP_TICKS = 40
        private const val LANDING_TOLERANCE = 0.3
        private const val MAX_RUN_TICKS = 200
        private const val TRACE_TOLERANCE = 1.0E-3
        private val QUARTZ = Blocks.QUARTZ_PILLAR.defaultBlockState()
    }

    /**
     * Runs the course [course] from [start], looking at the first block of it not stood on yet and jumping up the step
     * walked into; the blocks stood on, in order, until the last one or a fall below all of them. A block of the course
     * counts as stood on anywhere within [platform] blocks around it, as on a wider platform. [end] gets the player as
     * the run ends.
     */
    private fun run(
        world: TestWorld,
        start: Vec3,
        course: List<BlockPos>,
        platform: Int = 0,
        end: (PlayerMotion) -> Unit = {},
    ): List<BlockPos> {
        val terrain = world.terrain()
        val abilities = walkAbilities(canParkour = true, riskyJumps = true)
        val jumps = Jumps(terrain, abilities, holdsYaw = true)
        val targets = JumpTargets(terrain)
        val assisted = AssistedJump()
        val state = PlayerMotion(terrain)
        state.reset(start, PlayerMotion.RESTING, onGround = true)
        val lowest = course.minOf { it.y }
        val stood = ArrayList<BlockPos>()
        var next = 0
        var aimYaw = Float.NaN

        repeat(course.size * TICKS_PER_BLOCK) {
            val on = course.indexOfFirst { block ->
                (-platform..platform).any { dx ->
                    (-platform..platform).any { dz -> state.standsOn(block.x + dx, block.y, block.z + dz) }
                }
            }
            if (on >= 0 && stood.lastOrNull() != course[on]) {
                stood += course[on]
                next = on + 1
            }
            if (next >= course.size || state.y < lowest) {
                end(state)
                return stood
            }

            val look = Vec3.atBottomCenterOf(course[next].above())
            val camera = look.subtract(state.position).yaw
            val yaw = if (aimYaw.isNaN()) camera else aimYaw
            val held = LeapInput(1, 0, jump = state.horizontalCollision, sprint = true)
            val keys = assisted.tick(state, yaw, camera, held, MAX_ANGLE, jumps, targets)
                ?: jumpAtEdge(state, yaw, held)
            state.tick(yaw, keys.forward, keys.strafe, keys.jump, keys.sprint, keys.sneak)
            if (assisted.target == null) {
                aimYaw = Float.NaN
            } else {
                val turn = assisted.aimYaw(state.position, state.deltaMovement, state.onGround)
                if (!turn.isNaN()) {
                    aimYaw = turn
                }
            }
        }
        end(state)
        return stood
    }

    /** ModuleParkour without a target: [held], with a jump on the last tick on the ground but not off a bounce. */
    private fun jumpAtEdge(state: PlayerMotion, yaw: Float, held: LeapInput): LeapInput {
        val probe = state.copy()
        probe.tick(yaw, held.forward, held.strafe, jump = false, sprint = held.sprint)
        val edge = state.onGround && !probe.onGround && state.motionY <= 0.0
        return LeapInput(held.forward, held.strafe, jump = held.jump || edge, sprint = held.sprint)
    }

    private fun course(world: TestWorld, vararg blocks: BlockPos) = blocks.toList().onEach {
        world.set(it.x, it.y, it.z, TestWorld.STONE)
    }

    @Test
    fun `the next block of the course is jumped to rather than the floor below it`() {
        // 196 Level Parkour, level 1: the floor three blocks down was the safer landing.
        val world = TestWorld().fill(-8, -3, -8, 12, -3, 8)
        val course = course(world, BlockPos(0, 0, 0), BlockPos(3, 0, 0), BlockPos(6, 1, 0))
        world.fill(-3, 0, 0, -1, 0, 0)

        assertEquals(course, run(world, TestWorld.feet(-3, 1, 0), course))
    }

    @Test
    fun `a block out of reach from the landing spot is jumped to from a run-up`() {
        // 196 Level Parkour, level 1: the course turns at most blocks, so a landing leaves no run for the next jump.
        val world = room(0)
        val course = course(
            world, BlockPos(3, 20, 7), BlockPos(3, 21, 10), BlockPos(7, 21, 10), BlockPos(7, 22, 7),
            BlockPos(7, 22, 5), BlockPos(9, 23, 2), BlockPos(10, 23, 6),
        )
        world.set(3, 19, 4, TestWorld.STONE)

        assertEquals(course, run(world, TestWorld.feet(3, 20, 4), course))
    }

    @Test
    fun `the player turns where the jump was simulated to aim`() {
        // 196 Level Parkour, level 10: the block of the course at 99 22 8 hangs over the jump from 97 20 9 to
        // 99 20 5, which only works aimed beside the middle of the block.
        val world = room(90).set(99, 22, 8, TestWorld.STONE)
        val course = course(world, BlockPos(94, 19, 9), BlockPos(97, 20, 9), BlockPos(99, 20, 5), BlockPos(96, 21, 3))

        assertEquals(course, run(world, TestWorld.feet(92, 19, 9), course))
    }

    @Test
    fun `a landing on slime with no jump from the landing spot settles instead of hopping off`() {
        // Slime Parkour by GeoBilder: carpeted slime platforms, the next one up and a run-up away. With no jump from
        // the landing spot, the bounce carried the player over the edge, and the edge jump on every bounce sent it
        // further.
        val slime = Blocks.SLIME_BLOCK.defaultBlockState()
        val carpet = Blocks.MOSS_CARPET.defaultBlockState()
        val world = TestWorld().fill(7, 11, 24, 9, 11, 24).fill(8, 11, 23, 8, 11, 25)
            .fill(7, 11, 18, 9, 11, 20, slime).fill(7, 12, 18, 9, 12, 20, carpet)
            .fill(3, 12, 13, 5, 12, 15, slime).fill(3, 13, 13, 5, 13, 15, carpet)
        val course = listOf(BlockPos(8, 11, 24), BlockPos(8, 12, 19), BlockPos(4, 13, 14))
        var last: PlayerMotion? = null

        val stood = run(world, TestWorld.feet(8, 12, 24), course, platform = 1) { last = it }
        val state = assertNotNull(last)
        assertEquals(course.take(2), stood.take(2))
        assertTrue(state.onGround && state.motionY <= 0.0 && state.y > 12.0, "settled, at ${state.position}")
    }

    @Test
    fun `a bounce goes on to the next block rather than back onto the slime it came off`() {
        // Gamerichie's Academy of Parkour, stage 16: a drop onto a 3x3 slime pad bounces up to a ladder on a block four
        // higher. The next slime block of the pad was the closer landing.
        val slime = Blocks.SLIME_BLOCK.defaultBlockState()
        val ladder = Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, Direction.EAST)
        val world = TestWorld().fill(12, 106, 28, 15, 106, 28).fill(5, 101, 27, 7, 101, 29, slime)
            .set(2, 105, 28, TestWorld.STONE).set(3, 105, 28, ladder)
        val course = listOf(BlockPos(12, 106, 28), BlockPos(6, 101, 28), BlockPos(2, 105, 28))

        assertEquals(course, run(world, TestWorld.feet(14, 107, 28), course, platform = 1))
    }

    @Test
    fun `a fall onto slime bounces up to a block higher than a jump reaches`() {
        val world = TestWorld().fill(-3, 6, 0, -1, 6, 0)
        val course = course(world, BlockPos(0, 6, 0), BlockPos(4, 0, 0), BlockPos(8, 3, 0))
        world.set(4, 0, 0, Blocks.SLIME_BLOCK.defaultBlockState())

        assertEquals(course, run(world, TestWorld.feet(-2, 7, 0), course))
    }

    @Test
    fun `a fall onto a bed bounces up to a block higher than a jump reaches`() {
        // A bed bounces back three quarters of the speed. The feet stand in the bed block, so the block below it was
        // taken for the floor, and the bed far below did not count as a landing; the block beside it was jumped to.
        val world = TestWorld().fill(-3, 8, 0, -1, 8, 0).set(4, 8, 1, TestWorld.STONE)
        val course = course(world, BlockPos(0, 8, 0), BlockPos(4, 0, 0), BlockPos(8, 2, 0))
        world.set(4, 0, 0, Blocks.BED.red().defaultBlockState())

        assertEquals(course, run(world, TestWorld.feet(-2, 9, 0), course))
    }

    @Test
    fun `a chain of single slime blocks is jumped along on every bounce`() {
        // Ultimate Slime Challenge 1-2, "DO press SPACE": single slime blocks five and four apart. The bounce off one
        // is lower than a jump, so only a jump on the bounce with the speed of the jump before reaches the next one.
        val slime = Blocks.SLIME_BLOCK.defaultBlockState()
        val world = TestWorld().fill(16, 9, -44, 22, 9, -40).fill(16, 9, -66, 22, 9, -62)
            .set(19, 9, -48, slime).set(19, 9, -53, slime).set(19, 9, -57, slime)
        val course = listOf(
            BlockPos(19, 9, -42), BlockPos(19, 9, -48), BlockPos(19, 9, -53), BlockPos(19, 9, -57),
            BlockPos(19, 9, -63),
        )

        assertEquals(course, run(world, TestWorld.feet(19, 10, -41), course, platform = 1))
    }

    @Test
    fun `a slab on slime far below is no landing`() {
        // A slab on slime stops a fall like any slab: 0.2 below the feet is the slab, not the slime. It was taken for a
        // bounce pad, an easier landing than the next block of the course four blocks on.
        val world = TestWorld().fill(-3, 8, 0, -1, 8, 0)
            .fill(2, 0, -1, 4, 0, 1, Blocks.SLIME_BLOCK.defaultBlockState())
            .fill(2, 1, -1, 4, 1, 1, Blocks.STONE_SLAB.defaultBlockState())
        val course = course(world, BlockPos(0, 8, 0), BlockPos(5, 8, 0))

        assertEquals(course, run(world, TestWorld.feet(-2, 9, 0), course))
    }

    @Test
    fun `a jump past a pot on the straight line takes off beside it`() {
        // A server Jump 'n' Run: from a carpeted 1x1 pillar to the next one, lower and to the left, with a pot on a
        // post at feet height on the straight line between them.
        val world = TestWorld()
        val course = listOf(pillar(world, 0, 10, 0), pillar(world, -4, 9, -1))
        pot(world, -2, 11, 0)

        assertEquals(course, run(world, Vec3(0.5, 11.0625, 0.5), course))
    }

    @Test
    fun `a jump between two pots takes off from the edge in line with the gap`() {
        // The same Jump 'n' Run: pots on posts front left and front right, the next pillar behind and between them.
        val world = TestWorld()
        val course = listOf(pillar(world, 0, 10, 0), pillar(world, 1, 9, -4))
        pot(world, 0, 11, -2)
        pot(world, 2, 11, -2)

        assertEquals(course, run(world, Vec3(0.5, 11.0625, 0.5), course))
    }

    @Test
    fun `a post right beside the block jumped from is jumped past`() {
        // The straight line leaves the pillar through a post too high to hop onto, which hid the pillar behind it.
        val world = TestWorld()
        val course = listOf(pillar(world, 0, 10, 0), pillar(world, -4, 10, -1))
        pot(world, -1, 12, 0)

        assertEquals(course, run(world, Vec3(0.5, 11.0625, 0.5), course))
    }

    @Test
    fun `a block behind a wall seen end on is no jump around a post`() {
        // 196 Level Parkour, level 122: from a doorway in a wall a block thick, the way on is down to the floor of the
        // next room. Its first block, one lower and further along the wall, was taken for a jump around the wall, and
        // the player was walked back and forth to a run-up for it.
        val air = Blocks.AIR.defaultBlockState()
        val world = TestWorld().fill(44, 18, 111, 56, 26, 121).fill(44, 19, 112, 56, 26, 120, air)
            .fill(51, 19, 112, 51, 26, 120).fill(51, 25, 118, 51, 26, 118, air).set(50, 23, 115, TestWorld.STONE)
        val course = listOf(BlockPos(51, 24, 118), BlockPos(50, 18, 117))

        assertEquals(course, run(world, TestWorld.feet(51, 25, 118), course))
    }

    @Test
    fun `a jump around the end of a wall lands on the block behind it`() {
        // A neo: the next block is behind a wall a block thick whose end is in front of the block jumped from, so the
        // straight line runs into it.
        val world = TestWorld().fill(1, 4, -4, 1, 13, 0)
        val course = listOf(pillar(world, 0, 10, 0), pillar(world, 3, 10, 0))

        assertEquals(course, run(world, Vec3(0.5, 11.0625, 0.5), course))
    }

    @Test
    fun `a jump past an end rod under a hanging pillar goes beside it`() {
        // Parkour Challenge, level 3: three blocks between quartz pillars, straight through a quartz pillar that hangs
        // down to head height with an end rod under it at feet height. A ladder is on the pillar landed on.
        val ladder = Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, Direction.WEST)
        val world = TestWorld().set(-124, 66, 9998, rod(Direction.DOWN)).fill(-124, 67, 9998, -124, 70, 9998, QUARTZ)
            .fill(-125, 67, 9998, -125, 70, 9998, ladder)
        val course = listOf(quartz(world, -122, 65, 9998), quartz(world, -125, 65, 9998))

        assertEquals(course, run(world, TestWorld.feet(-122, 66, 9998), course))
    }

    @Test
    fun `a corner jump past a wall post on the diagonal goes around it`() {
        // Parkour Challenge, level 8: a corner jump one up, with a stone brick wall post on the diagonal between.
        val world = TestWorld().set(-425, 113, 9997, Blocks.STONE_BRICK_WALL.defaultBlockState())
        val course = listOf(quartz(world, -424, 112, 9998), quartz(world, -426, 113, 9996))

        assertEquals(course, run(world, TestWorld.feet(-424, 113, 9998), course))
    }

    @Test
    fun `a jump around a hanging pillar straight ahead goes beside it`() {
        // Parkour Challenge, level 10: four blocks on and one down, through a quartz pillar hanging down to feet
        // height with an end rod under it.
        val world = TestWorld().set(-553, 129, 10001, rod(Direction.DOWN))
            .fill(-553, 130, 10001, -553, 131, 10001, QUARTZ)
        val course = listOf(quartz(world, -553, 129, 9999), quartz(world, -553, 128, 10003))

        assertEquals(course, run(world, TestWorld.feet(-553, 130, 9999), course))
    }

    @Test
    fun `the traced way of a jump is the way the player takes`() {
        val world = room(0).set(3, 19, 4, TestWorld.STONE).set(3, 20, 7, TestWorld.STONE)
        val terrain = world.terrain()
        val jumps = Jumps(terrain, walkAbilities(canParkour = true, riskyJumps = true), holdsYaw = true)
        val targets = JumpTargets(terrain)
        val assisted = AssistedJump()
        val state = PlayerMotion(terrain)
        state.reset(TestWorld.feet(3, 20, 3).add(0.0, 0.0, 0.7), PlayerMotion.RESTING, onGround = true)
        val held = LeapInput(1, 0, jump = false, sprint = true)

        assisted.tick(state, 0f, 0f, held, MAX_ANGLE, jumps, targets)
        assisted.trace(state, 0f, 0f, MAX_ANGLE, jumps, targets)
        val target = assertNotNull(assisted.target)
        val path = ArrayList(assisted.arcs.first().path)
        val takeoff = assisted.arcs.first().takeoff
        assertTrue(takeoff >= 0, "the jump starts on the ground")

        var yaw = 0f
        var ground = state.position
        var lastGround = 0
        for (tick in 1..MAX_JUMP_TICKS) {
            val keys = assisted.tick(state, yaw, yaw, held, MAX_ANGLE, jumps, targets) ?: held
            state.tick(yaw, keys.forward, keys.strafe, keys.jump, keys.sprint, keys.sneak)
            if (state.onGround && tick > takeoff + 1) {
                break
            }
            if (state.onGround) {
                ground = state.position
                lastGround = tick
            }
            yaw = target.aim.subtract(state.position).yaw
        }

        assertEquals(takeoff, lastGround, "take-off tick")
        assertEquals(0.0, ground.distanceTo(path[takeoff]), 1.0E-6, "take-off spot")
        assertTrue(state.standsOn(3, 20, 7), "lands on the target, at ${state.position}")
        assertTrue(path.last().distanceTo(state.position) < LANDING_TOLERANCE, "predicted landing ${path.last()}")
    }

    @Test
    fun `the traced landing of a jump is where the player comes down`() {
        val world = room(0).set(3, 19, 4, TestWorld.STONE).set(3, 20, 7, TestWorld.STONE)
        val (traced, landed) = tracedAndLanded(world, TestWorld.feet(3, 20, 4).add(0.0, 0.0, -0.3), heading = 0f)

        assertEquals(1, traced.size, "arcs")
        assertLandsAsTraced(traced, landed)
    }

    @Test
    fun `the traced landing on slime and the jump off its bounce are where the player comes down`() {
        val world = TestWorld().fill(-3, 6, 0, -1, 6, 0)
        course(world, BlockPos(0, 6, 0), BlockPos(4, 0, 0), BlockPos(8, 3, 0))
        world.set(4, 0, 0, Blocks.SLIME_BLOCK.defaultBlockState())
        val heading = Vec3(1.0, 0.0, 0.0).yaw
        val (traced, landed) = tracedAndLanded(world, TestWorld.feet(-2, 7, 0), heading)

        assertEquals(2, traced.size, "arcs")
        assertLandsAsTraced(traced, landed)
    }

    /**
     * Where the arcs the assist traces first come down, and where the run it then makes from [start] comes down, one
     * landing per arc, holding forward towards [heading] and turning as ModuleParkour turns the player.
     */
    private fun tracedAndLanded(world: TestWorld, start: Vec3, heading: Float): Pair<List<Vec3>, List<Vec3>> {
        val terrain = world.terrain()
        val jumps = Jumps(terrain, walkAbilities(canParkour = true, riskyJumps = true), holdsYaw = true)
        val targets = JumpTargets(terrain)
        val assisted = AssistedJump()
        val state = PlayerMotion(terrain)
        state.reset(start, PlayerMotion.RESTING, onGround = true)
        val held = LeapInput(1, 0, jump = false, sprint = true)
        var traced = emptyList<Vec3>()
        val landed = ArrayList<Vec3>()
        var aimYaw = Float.NaN

        for (tick in 0 until MAX_RUN_TICKS) {
            val yaw = if (aimYaw.isNaN()) heading else aimYaw
            val keys = assisted.tick(state, yaw, heading, held, MAX_ANGLE, jumps, targets) ?: held
            if (traced.isEmpty() && assisted.target != null) {
                assisted.trace(state, yaw, heading, MAX_ANGLE, jumps, targets)
                traced = assisted.arcs.map { it.path.last() }
            }
            val inAir = !state.onGround
            state.tick(yaw, keys.forward, keys.strafe, keys.jump, keys.sprint, keys.sneak)
            if (traced.isNotEmpty() && inAir && state.onGround) {
                landed += state.position
            }
            if (traced.isNotEmpty() && landed.size >= traced.size) {
                break
            }
            val turn = assisted.aimYaw(state.position, state.deltaMovement, state.onGround)
            aimYaw = if (assisted.target == null) Float.NaN else turn.ifNaN { aimYaw }
        }
        return traced to landed
    }

    private fun assertLandsAsTraced(traced: List<Vec3>, landed: List<Vec3>) {
        assertEquals(traced.size, landed.size, "landings, traced $traced")
        for ((predicted, actual) in traced.zip(landed)) {
            assertEquals(0.0, predicted.distanceTo(actual), TRACE_TOLERANCE, "traced $predicted, landed $actual")
        }
    }

    /** A stone pillar up to [top] with red carpet on it; the block of the course stood on. */
    private fun pillar(world: TestWorld, x: Int, top: Int, z: Int): BlockPos {
        world.fill(x, top - 6, z, x, top, z).set(x, top + 1, z, Blocks.CARPET.red().defaultBlockState())
        return BlockPos(x, top + 1, z)
    }

    /** A full block at [y] on a post of stripped logs, with a potted flower on it. */
    private fun pot(world: TestWorld, x: Int, y: Int, z: Int) {
        world.fill(x, y - 6, z, x, y - 1, z, Blocks.STRIPPED_OAK_LOG.defaultBlockState())
            .set(x, y, z, Blocks.MUD_BRICKS.defaultBlockState())
            .set(x, y + 1, z, Blocks.POTTED_POPPY.defaultBlockState())
    }

    /** A quartz pillar block at [top] with an end rod hanging under it, as Parkour Challenge builds its blocks. */
    private fun quartz(world: TestWorld, x: Int, top: Int, z: Int): BlockPos {
        world.set(x, top, z, QUARTZ).set(x, top - 1, z, rod(Direction.DOWN))
        return BlockPos(x, top, z)
    }

    private fun rod(facing: Direction) = Blocks.END_ROD.defaultBlockState().setValue(EndRodBlock.FACING, facing)

    /** A room of 196 Level Parkour: a floor at y 18 walled in from x 1 to 11 east of [x] and z 1 to 11. */
    private fun room(x: Int) = TestWorld().fill(x + 1, 18, 1, x + 11, 18, 11)
        .fill(x + 1, 19, 1, x + 11, 23, 1).fill(x + 1, 19, 11, x + 11, 23, 11)
        .fill(x + 1, 19, 1, x + 1, 23, 11).fill(x + 11, 19, 1, x + 11, 23, 11)
}
