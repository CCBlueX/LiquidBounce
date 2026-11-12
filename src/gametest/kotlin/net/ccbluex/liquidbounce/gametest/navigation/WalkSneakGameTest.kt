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
import net.ccbluex.liquidbounce.gametest.fillArea
import net.ccbluex.liquidbounce.gametest.harness.LiquidBounceGameTest
import net.ccbluex.liquidbounce.gametest.onClient
import net.minecraft.core.BlockPos
import net.minecraft.world.phys.Vec3

private const val GOAL_RADIUS = 0.8

/** A survival player sneaks through a low tunnel and along narrow bridges high over the ground, unhurt. */
class WalkSneakGameTest : LiquidBounceGameTest({
    lateinit var walker: TestWalker
    context.onClient {
        walker = TestWalker("TestSneakWalker", GOAL_RADIUS)
    }

    try {
        val y = origin.y
        buildCourse(y)
        run("gamerule minecraft:natural_health_regeneration false")
        walker.start(context)

        moveTo(Vec3(6.5, y.toDouble(), 0.5))
        walk(walker, Vec3(17.5, y.toDouble(), 0.5), 600, "through the low tunnel")

        moveTo(Vec3(31.5, y + 21.0, 0.5))
        walk(walker, Vec3(42.5, y + 21.0, 10.5), 1200, "along the bridge")

        moveTo(Vec3(31.5, y + 21.0, -9.5))
        walk(walker, Vec3(45.5, y + 21.0, -9.5), 1200, "along the straight bridge")
    } finally {
        walker.stop(context)
    }
})

/**
 * A closed room at x 14..19, entered through a tunnel under top slabs, one and a half blocks high. Twenty blocks
 * up, a bridge one block wide that turns at x 42 towards z 10, and a straight one along z -10.
 */
private fun GameTestScope.buildCourse(y: Int) {
    fillArea(BlockPos(0, y, -20), BlockPos(60, y + 30, 20), "minecraft:air")
    fill(BlockPos(0, y - 1, -20), BlockPos(60, y - 1, 20), "minecraft:stone")

    fill(BlockPos(10, y, -3), BlockPos(20, y + 3, 3), "minecraft:stone")
    fill(BlockPos(14, y, -2), BlockPos(19, y + 2, 2), "minecraft:air")
    fill(BlockPos(10, y, 0), BlockPos(13, y, 0), "minecraft:air")
    fill(BlockPos(10, y + 1, 0), BlockPos(13, y + 1, 0), "minecraft:stone_slab[type=top]")

    fill(BlockPos(30, y + 20, -1), BlockPos(32, y + 20, 1), "minecraft:stone")
    fill(BlockPos(33, y + 20, 0), BlockPos(42, y + 20, 0), "minecraft:stone")
    fill(BlockPos(42, y + 20, 1), BlockPos(42, y + 20, 8), "minecraft:stone")
    fill(BlockPos(41, y + 20, 9), BlockPos(43, y + 20, 11), "minecraft:stone")

    fill(BlockPos(30, y + 20, -11), BlockPos(32, y + 20, -9), "minecraft:stone")
    fill(BlockPos(33, y + 20, -10), BlockPos(45, y + 20, -10), "minecraft:stone")
    awaitServer("block 45 ${y + 20} -10 minecraft:stone")
}

private fun GameTestScope.walk(walker: TestWalker, goal: Vec3, ticks: Int, what: String) {
    var sneaked = false
    unhurt("Walking $what") {
        walker.walk(context, goal, ticks, "Walking $what", "Did not walk $what") {
            sneaked = sneaked || it.isShiftKeyDown
        }
    }
    check(sneaked) { "Walked $what without sneaking" }
}
