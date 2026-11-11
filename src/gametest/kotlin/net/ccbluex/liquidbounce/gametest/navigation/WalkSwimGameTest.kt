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
import net.ccbluex.liquidbounce.gametest.harness.LiquidBounceGameTest
import net.ccbluex.liquidbounce.gametest.onClient
import net.minecraft.core.BlockPos
import net.minecraft.world.phys.Vec3
import kotlin.math.min

// Above the thin superflat ground, so the pools fit into the stone below the rooms
private const val COURSE_HEIGHT = 10
private const val ARRIVAL_TICKS = 800
private const val GOAL_RADIUS = 0.8

/** A survival player walks between two rooms joined only under water, unhurt and without running out of air. */
class WalkSwimGameTest : LiquidBounceGameTest({
    lateinit var walker: TestWalker
    context.onClient {
        walker = TestWalker("TestSwimWalker", GOAL_RADIUS)
    }

    try {
        val y = origin.y + COURSE_HEIGHT
        buildCourse(y)
        run("gamerule minecraft:natural_health_regeneration false")
        walker.start(context)

        moveTo(Vec3(2.5, y.toDouble(), 0.5))
        var swam = false
        var minAir = Int.MAX_VALUE
        unhurt("Swimming through the tunnel") {
            walker.walk(
                context, Vec3(24.5, y.toDouble(), 0.5), ARRIVAL_TICKS,
                title = "Swimming through the tunnel", failure = "Did not swim through the tunnel",
            ) {
                swam = swam || it.isSwimming
                minAir = min(minAir, it.airSupply)
            }
        }
        check(minAir > 0) { "The player ran out of air in the tunnel" }
        check(swam) { "Went through the tunnel without the swimming pose" }
    } finally {
        walker.stop(context)
    }
})

/** Two closed rooms in stone, each with a pool four blocks deep, joined by a flooded tunnel one block high. */
private fun GameTestScope.buildCourse(y: Int) {
    fill(BlockPos(-2, y - 7, -6), BlockPos(30, y + 4, 6), "minecraft:stone")
    fill(BlockPos(0, y, -3), BlockPos(10, y + 2, 3), "minecraft:air")
    fill(BlockPos(16, y, -3), BlockPos(26, y + 2, 3), "minecraft:air")
    fill(BlockPos(5, y - 4, -2), BlockPos(9, y - 1, 2), "minecraft:water")
    fill(BlockPos(17, y - 4, -2), BlockPos(21, y - 1, 2), "minecraft:water")
    fill(BlockPos(10, y - 4, 0), BlockPos(16, y - 4, 0), "minecraft:water")
    awaitServer("block 16 ${y - 4} 0 minecraft:water")
}
