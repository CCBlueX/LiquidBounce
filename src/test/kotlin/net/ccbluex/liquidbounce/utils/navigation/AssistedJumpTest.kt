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
import net.ccbluex.liquidbounce.utils.math.yaw
import net.ccbluex.liquidbounce.utils.navigation.walk.AssistedJump
import net.ccbluex.liquidbounce.utils.navigation.walk.JumpTargets
import net.ccbluex.liquidbounce.utils.navigation.walk.Jumps
import net.ccbluex.liquidbounce.utils.navigation.walk.LeapInput
import net.minecraft.core.BlockPos
import net.minecraft.world.phys.Vec3
import kotlin.test.Test
import kotlin.test.assertEquals

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
        private const val MIN_AIM_DISTANCE = 0.75
        private const val TICKS_PER_BLOCK = 60
    }

    /**
     * Runs the course of single blocks [course] from [start], looking at the first block of it not stood on yet; the
     * blocks stood on, in order, until the last one or a fall below all of them.
     */
    private fun run(world: TestWorld, start: Vec3, course: List<BlockPos>): List<BlockPos> {
        val terrain = world.terrain()
        val abilities = walkAbilities(canParkour = true, riskyJumps = true)
        val jumps = Jumps(terrain, abilities)
        val targets = JumpTargets(terrain)
        val assisted = AssistedJump()
        val state = PlayerMotion(terrain)
        state.reset(start, PlayerMotion.RESTING, onGround = true)
        val lowest = course.minOf { it.y }
        val stood = ArrayList<BlockPos>()
        var next = 0
        var aimYaw = Float.NaN

        repeat(course.size * TICKS_PER_BLOCK) {
            val on = course.indexOfFirst { state.standsOn(it.x, it.y, it.z) }
            if (on >= 0 && stood.lastOrNull() != course[on]) {
                stood += course[on]
                next = on + 1
            }
            if (next >= course.size || state.y < lowest) {
                return stood
            }

            val look = Vec3.atBottomCenterOf(course[next].above())
            val camera = look.subtract(state.position).yaw
            val yaw = if (aimYaw.isNaN()) camera else aimYaw
            val held = LeapInput(1, 0, jump = false, sprint = true)
            val keys = assisted.tick(state, yaw, camera, held, MAX_ANGLE, jumps, targets) ?: jumpAtEdge(state, yaw)
            state.tick(yaw, keys.forward, keys.strafe, keys.jump, keys.sprint, keys.sneak)

            val target = assisted.target
            if (target == null) {
                aimYaw = Float.NaN
            } else {
                val delta = target.aim.subtract(state.position)
                if (delta.horizontalDistanceSqr() >= MIN_AIM_DISTANCE * MIN_AIM_DISTANCE) {
                    aimYaw = delta.yaw
                }
            }
        }
        return stood
    }

    /** ModuleParkour without a target: forward and sprint, with a jump on the last tick on the ground. */
    private fun jumpAtEdge(state: PlayerMotion, yaw: Float): LeapInput {
        val probe = state.copy()
        probe.tick(yaw, 1, 0, jump = false, sprint = true)
        return LeapInput(1, 0, jump = state.onGround && !probe.onGround, sprint = true)
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
}
