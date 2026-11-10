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

package net.ccbluex.liquidbounce.gametest.navigation

import net.ccbluex.liquidbounce.gametest.groundY
import net.ccbluex.liquidbounce.gametest.onClient
import net.ccbluex.liquidbounce.gametest.useAngleSmooth
import net.ccbluex.liquidbounce.gametest.waitForClient
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.phys.Vec3

/**
 * A survival player runs Jump 'n' Run courses high over a lake with the walking navigation: gaps of one to three
 * blocks, jumps up and down, diagonal and odd angles, turns, landings on fences, panes and walls and jumps under a
 * low ceiling, each without falling.
 */
class WalkParkourGameTest : FabricClientGameTest {

    private class Block(val pos: BlockPos, val state: String = "stone")

    /** Blocks to jump along, start first and goal last, and [others] around them, relative to the course origin. */
    private class Course(val name: String, val path: List<Block>, val others: List<Block> = emptyList())

    private lateinit var walker: TestWalker

    override fun runTest(context: ClientGameTestContext) {
        if (!navigationGameTests) {
            return
        }
        context.waitForClient()
        context.onClient {
            walker = TestWalker("TestParkourWalker", GOAL_RADIUS)
            walker.containedValues.first { it.name == "RiskyJumps" }.setByString("true")
            useAngleSmooth(walker)
        }

        try {
            context.worldBuilder().create().use { world ->
                val server = world.server
                val ground = server.groundY()
                server.runCommand("gamemode survival @a")
                server.runCommand("clear @a")
                server.runCommand("effect give @a saturation infinite 0 true")
                walker.start(context)

                val failures = COURSES.withIndex().mapNotNull { (index, course) ->
                    run(context, server, ground, BlockPos(index * SPACING, ground + HEIGHT, 0), course)
                }
                check(failures.isEmpty()) { "Parkour courses failed:\n${failures.joinToString("\n")}" }
            }
        } finally {
            walker.stop(context)
        }
    }

    /** Builds [course] at [origin] over a lake and walks it; why it failed, or null. */
    private fun run(
        context: ClientGameTestContext,
        server: TestServerContext,
        ground: Int,
        origin: BlockPos,
        course: Course,
    ): String? {
        server.runCommand("fill ${origin.x - MARGIN} ${ground - 2} -20 ${origin.x + MARGIN * 2} ${ground - 1} 20 water")
        for (block in course.path + course.others) {
            val pos = origin.offset(block.pos)
            server.runCommand("setblock ${pos.x} ${pos.y} ${pos.z} ${block.state}")
        }

        val start = feet(server, origin.offset(course.path.first().pos))
        val goal = feet(server, origin.offset(course.path.last().pos))
        val lowest = origin.y + course.path.minOf { it.pos.y }

        moveTo(context, server, start, settleTicks = SETTLE)

        val failure = walker.walking(context, goal) {
            runCourse(context, course.name, course.name, lowest, TIMEOUT) { it.arrivedAt(goal, GOAL_RADIUS) }
        }
        return failure?.let { "${course.name}: $it" }
    }

    /** Where the feet stand on top of the block at [pos]. */
    private fun feet(server: TestServerContext, pos: BlockPos): Vec3 = server.computeOnServer<Vec3, RuntimeException> {
        val level = it.overworld()
        val top = level.getBlockState(pos).getCollisionShape(level, pos).max(Direction.Axis.Y)
        Vec3(pos.x + 0.5, pos.y + top, pos.z + 0.5)
    }

    private companion object {
        const val GOAL_RADIUS = 0.5
        const val HEIGHT = 24
        const val SPACING = 40
        const val MARGIN = 10
        const val SETTLE = 10
        const val TIMEOUT = 1200

        fun stone(x: Int, y: Int, z: Int) = Block(BlockPos(x, y, z))

        fun block(x: Int, y: Int, z: Int, state: String) = Block(BlockPos(x, y, z), state)

        val COURSES = listOf(
            Course(
                "straight gaps",
                listOf(stone(0, 0, 0), stone(2, 0, 0), stone(5, 0, 0), stone(9, 0, 0), stone(12, 0, 0)),
            ),
            Course("up", listOf(stone(0, 0, 0), stone(2, 1, 0), stone(5, 2, 0), stone(7, 3, 0), stone(10, 4, 0))),
            Course(
                "down",
                listOf(stone(0, 0, 0), stone(4, -1, 0), stone(8, -2, 0), stone(12, -3, 0), stone(14, -4, 0)),
            ),
            Course(
                "diagonal",
                listOf(stone(0, 0, 0), stone(2, 0, 1), stone(4, 0, 3), stone(7, 0, 4), stone(9, 0, 6), stone(12, 0, 8)),
            ),
            Course(
                "turns",
                listOf(stone(0, 0, 0), stone(3, 0, 0), stone(3, 0, 3), stone(0, 0, 4), stone(0, 0, 7), stone(3, 0, 8)),
            ),
            Course(
                "thin landings",
                listOf(
                    stone(0, 0, 0), block(3, 0, 0, "oak_fence"), stone(6, 0, 0), block(9, 0, 0, "glass_pane"),
                    stone(12, 0, 0), block(15, 0, 0, "cobblestone_wall"), stone(17, 0, 0),
                ),
            ),
            Course(
                "low ceiling",
                listOf(stone(0, 0, 0), stone(2, 0, 0), stone(4, 0, 0), stone(7, 0, 0)),
                listOf(stone(0, 3, 0), stone(1, 3, 0), stone(2, 3, 0), stone(3, 3, 0), stone(4, 3, 0)),
            ),
        )
    }

}
