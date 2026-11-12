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

import net.ccbluex.gametest.harness.GameTestScope
import net.ccbluex.gametest.harness.action.awaitServer
import net.ccbluex.gametest.harness.action.fill
import net.ccbluex.gametest.harness.action.setBlock
import net.ccbluex.liquidbounce.gametest.forceLoadArea
import net.ccbluex.liquidbounce.gametest.harness.LiquidBounceGameTest
import net.ccbluex.liquidbounce.gametest.onClient
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.phys.Vec3

private const val GOAL_RADIUS = 0.5
private const val HEIGHT = 24
private const val SPACING = 40
private const val MARGIN = 10
private const val TIMEOUT = 1200

private class ParkourBlock(val pos: BlockPos, val state: String = "minecraft:stone")

/** Blocks to jump along, start first and goal last, and [others] around them, relative to the course origin. */
private class ParkourCourse(
    val name: String,
    val path: List<ParkourBlock>,
    val others: List<ParkourBlock> = emptyList(),
)

/**
 * A survival player runs Jump 'n' Run courses high over a lake with the walking navigation: gaps of one to three
 * blocks, jumps up and down, diagonal and odd angles, turns, landings on fences, panes and walls and jumps under a
 * low ceiling, each without falling.
 */
class WalkParkourGameTest : LiquidBounceGameTest({
    lateinit var walker: TestWalker
    context.onClient {
        walker = TestWalker("TestParkourWalker", GOAL_RADIUS)
        walker.containedValues.first { it.name == "RiskyJumps" }.setByString("true")
    }

    try {
        val ground = origin.y
        run("effect give $playerName minecraft:saturation infinite 0 true")
        walker.start(context)

        val failures = COURSES.withIndex().mapNotNull { (index, course) ->
            runCourse(walker, ground, BlockPos(index * SPACING, ground + HEIGHT, 0), course)
        }
        check(failures.isEmpty()) { "Parkour courses failed:\n${failures.joinToString("\n")}" }
    } finally {
        walker.stop(context)
    }
})

/** Builds [course] at [origin] over a lake and walks it; why it failed, or null. */
private fun GameTestScope.runCourse(walker: TestWalker, ground: Int, origin: BlockPos, course: ParkourCourse): String? {
    val from = BlockPos(origin.x - MARGIN, ground - 2, -20)
    val to = BlockPos(origin.x + MARGIN * 2, ground - 1, 20)
    forceLoadArea(from, to)
    fill(from, to, "minecraft:water")
    for (block in course.path + course.others) {
        setBlock(origin.offset(block.pos), block.state)
    }
    val placed = (course.path + course.others).last()
    val at = origin.offset(placed.pos)
    awaitServer("block ${at.x} ${at.y} ${at.z} ${placed.state}")

    val start = origin.offset(course.path.first().pos).above()
    moveTo(Vec3.atBottomCenterOf(start))
    val goal = feet(origin.offset(course.path.last().pos))
    val lowest = origin.y + course.path.minOf { it.pos.y }

    val failure = walker.walking(context, goal) {
        runCourse(context, course.name, course.name, lowest, TIMEOUT) { it.arrivedAt(goal, GOAL_RADIUS) }
    }
    return failure?.let { "${course.name}: $it" }
}

/** Where the feet stand on top of the block at [pos], once the client has it. */
private fun GameTestScope.feet(pos: BlockPos): Vec3 {
    awaitClient("the block at $pos") { !checkNotNull(it.level).getBlockState(pos).isAir }
    return client { mc ->
        val level = checkNotNull(mc.level)
        val top = level.getBlockState(pos).getCollisionShape(level, pos).max(Direction.Axis.Y)
        Vec3(pos.x + 0.5, pos.y + top, pos.z + 0.5)
    }
}

private fun stone(x: Int, y: Int, z: Int) = ParkourBlock(BlockPos(x, y, z))

private fun block(x: Int, y: Int, z: Int, state: String) = ParkourBlock(BlockPos(x, y, z), "minecraft:$state")

private val COURSES = listOf(
    ParkourCourse(
        "straight gaps",
        listOf(stone(0, 0, 0), stone(2, 0, 0), stone(5, 0, 0), stone(9, 0, 0), stone(12, 0, 0)),
    ),
    ParkourCourse("up", listOf(stone(0, 0, 0), stone(2, 1, 0), stone(5, 2, 0), stone(7, 3, 0), stone(10, 4, 0))),
    ParkourCourse(
        "down",
        listOf(stone(0, 0, 0), stone(4, -1, 0), stone(8, -2, 0), stone(12, -3, 0), stone(14, -4, 0)),
    ),
    ParkourCourse(
        "diagonal",
        listOf(stone(0, 0, 0), stone(2, 0, 1), stone(4, 0, 3), stone(7, 0, 4), stone(9, 0, 6), stone(12, 0, 8)),
    ),
    ParkourCourse(
        "turns",
        listOf(stone(0, 0, 0), stone(3, 0, 0), stone(3, 0, 3), stone(0, 0, 4), stone(0, 0, 7), stone(3, 0, 8)),
    ),
    ParkourCourse(
        "thin landings",
        listOf(
            stone(0, 0, 0), block(3, 0, 0, "oak_fence"), stone(6, 0, 0), block(9, 0, 0, "glass_pane"),
            stone(12, 0, 0), block(15, 0, 0, "cobblestone_wall"), stone(17, 0, 0),
        ),
    ),
    ParkourCourse(
        "low ceiling",
        listOf(stone(0, 0, 0), stone(2, 0, 0), stone(4, 0, 0), stone(7, 0, 0)),
        listOf(stone(0, 3, 0), stone(1, 3, 0), stone(2, 3, 0), stone(3, 3, 0), stone(4, 3, 0)),
    ),
)
