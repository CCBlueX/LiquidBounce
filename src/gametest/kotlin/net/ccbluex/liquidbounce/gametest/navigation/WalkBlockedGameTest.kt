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
import net.ccbluex.gametest.harness.ServerGameTest
import net.ccbluex.gametest.harness.action.awaitServer
import net.ccbluex.gametest.harness.action.fill
import net.ccbluex.liquidbounce.gametest.onClient
import net.ccbluex.liquidbounce.gametest.playerPosition
import net.ccbluex.liquidbounce.utils.math.horizontalDistanceTo
import net.minecraft.core.BlockPos
import net.minecraft.world.phys.Vec3

private const val GOAL_RADIUS = 0.8
private const val CLOSE_X = 7.0
private const val STANDING = 0.03

/** A step without progress is given up after 40 ticks at the latest. */
private const val MAX_STANDING_TICKS = 20

private const val APPROACH_TICKS = 200
private const val DETOUR_TICKS = 400

/**
 * The gap in a wall the path leads through is closed while the player walks up to it. The walk has to turn to the
 * other gap right away instead of standing in front of the block until the step times out.
 */
class WalkBlockedGameTest : ServerGameTest({
    lateinit var walker: TestWalker
    context.onClient {
        walker = TestWalker("TestWalker", GOAL_RADIUS)
    }

    try {
        val y = origin.y
        fill(BlockPos(0, y, -14), BlockPos(24, y + 4, 14), "minecraft:air")
        fill(BlockPos(0, y - 1, -14), BlockPos(24, y - 1, 14), "minecraft:stone")
        fill(BlockPos(10, y, -12), BlockPos(10, y + 2, 12), "minecraft:stone")
        fill(BlockPos(10, y, 0), BlockPos(10, y + 1, 0), "minecraft:air")
        fill(BlockPos(10, y, -8), BlockPos(10, y + 1, -8), "minecraft:air")
        awaitServer("block 10 $y -8 minecraft:air")

        moveTo(Vec3(4.5, y.toDouble(), 3.5), yaw = -90f)
        walker.start(context)

        val goal = Vec3(16.5, y.toDouble(), 3.5)
        val standing = walker.walking(context, goal) {
            awaitPlayer(context, APPROACH_TICKS, "Did not walk up to the gap") { it.x > CLOSE_X }
            fill(BlockPos(10, y, 0), BlockPos(10, y + 1, 0), "minecraft:stone")
            standingTicks(goal)
        }

        check(standing <= MAX_STANDING_TICKS) { "Stood $standing ticks in front of the closed gap" }
    } finally {
        walker.stop(context)
    }
})

/** Ticks without getting anywhere until the player is at [goal]. */
private fun GameTestScope.standingTicks(goal: Vec3): Int {
    var standing = 0
    var last = context.playerPosition()
    await(context, DETOUR_TICKS, "Did not walk through the other gap to $goal") {
        val position = context.playerPosition()
        if (position.horizontalDistanceTo(last) < STANDING) {
            standing++
        }
        last = position
        position.arrivedAt(goal, GOAL_RADIUS)
    }
    return standing
}
