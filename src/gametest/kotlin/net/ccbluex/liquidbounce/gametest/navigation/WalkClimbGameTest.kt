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
import net.minecraft.core.BlockPos
import net.minecraft.world.phys.Vec3

private const val COURSE_HEIGHT = 10
private const val TOWER_HEIGHT = 5
private const val CLIMB_TICKS = 400
private const val GOAL_RADIUS = 0.8

/** A survival player climbs a ladder onto a tower too high to drop from, and back down it unhurt. */
class WalkClimbGameTest : ServerGameTest({
    lateinit var walker: TestWalker
    context.onClient {
        walker = TestWalker("TestClimbWalker", GOAL_RADIUS)
    }

    try {
        val y = origin.y + COURSE_HEIGHT
        buildCourse(y)
        run("gamerule minecraft:natural_health_regeneration false")
        walker.start(context)

        val ground = Vec3(0.5, y.toDouble(), 0.5)
        val top = Vec3(6.5, (y + TOWER_HEIGHT).toDouble(), 0.5)
        moveTo(ground, yaw = 90f)
        walker.walk(context, top, CLIMB_TICKS, "Climbing the ladder", "Did not climb the ladder")
        unhurt("Climbing down the ladder") {
            walker.walk(context, ground, CLIMB_TICKS, "Climbing down the ladder", "Did not climb down the ladder")
        }
    } finally {
        walker.stop(context)
    }
})

/** A stone tower five blocks high with a ladder up its west side, on a floor in the air. */
private fun GameTestScope.buildCourse(y: Int) {
    fill(BlockPos(-3, y - 1, -4), BlockPos(10, y - 1, 4), "minecraft:stone")
    fill(BlockPos(-3, y, -4), BlockPos(10, y + TOWER_HEIGHT + 3, 4), "minecraft:air")
    fill(BlockPos(4, y, -2), BlockPos(8, y + TOWER_HEIGHT - 1, 2), "minecraft:stone")
    fill(BlockPos(3, y, 0), BlockPos(3, y + TOWER_HEIGHT - 1, 0), "minecraft:ladder[facing=west]")
    awaitServer("block 3 ${y + TOWER_HEIGHT - 1} 0 minecraft:ladder[facing=west]")
}
