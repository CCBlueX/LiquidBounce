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
import net.ccbluex.liquidbounce.features.module.modules.player.ModuleAutoWalk
import net.ccbluex.liquidbounce.gametest.command
import net.ccbluex.liquidbounce.gametest.fillArea
import net.ccbluex.liquidbounce.gametest.forceLoadArea
import net.ccbluex.liquidbounce.gametest.fromClient
import net.ccbluex.liquidbounce.gametest.harness.LiquidBounceGameTest
import net.ccbluex.liquidbounce.gametest.playerPosition
import net.ccbluex.liquidbounce.gametest.restoring
import net.minecraft.core.BlockPos
import net.minecraft.world.phys.Vec3

private const val MAX_TICKS = 1200
private const val STILL = 0.02
private const val MAX_STOPS = 5
private const val ARRIVAL = 1.1

/**
 * `.goto` walks to a destination far beyond the path range, across a field with three walls, in stages and without
 * stopping between them, and turns AutoWalk off on arrival.
 */
class AutoWalkStagesGameTest : LiquidBounceGameTest({
    context.restoring(ModuleAutoWalk) {
        val y = origin.y
        buildField(y)
        checkFarGoto(y)
    }
})

/** A stone field from x -90 to 90 with walls at x -45, 0 and 45 that leave a gap on alternating sides. */
private fun GameTestScope.buildField(y: Int) {
    forceLoadArea(BlockPos(-90, y, -14), BlockPos(90, y, 14))
    fillArea(BlockPos(-90, y, -14), BlockPos(90, y + 5, 14), "minecraft:air")
    fillArea(BlockPos(-90, y - 1, -14), BlockPos(90, y - 1, 14), "minecraft:stone")
    fill(BlockPos(-45, y, -14), BlockPos(-45, y + 2, 6), "minecraft:stone")
    fill(BlockPos(0, y, -6), BlockPos(0, y + 2, 14), "minecraft:stone")
    fill(BlockPos(45, y, -14), BlockPos(45, y + 2, 6), "minecraft:stone")
    awaitServer("block 45 ${y + 2} 6 minecraft:stone")
}

private fun GameTestScope.checkFarGoto(y: Int) {
    moveTo(Vec3(-75.5, y.toDouble(), 0.5), yaw = -90f)
    val destination = Vec3(75.5, y.toDouble(), 0.5)

    var stops = 0
    unhurt("Walking to the far destination") {
        context.command("goto ${destination.x} ${destination.y} ${destination.z}")
        check(context.fromClient { ModuleAutoWalk.enabled }) { ".goto did not turn AutoWalk on" }

        var last = context.playerPosition()
        var elapsed = 0
        TurnWatch.watch(context, "Walking to the far destination") {
            while (context.fromClient { ModuleAutoWalk.enabled }) {
                check(elapsed++ < MAX_TICKS) { "Did not arrive within $MAX_TICKS ticks, stopped at $last" }
                ticks(1)
                val (position, onGround) = context.fromClient { mc ->
                    val player = checkNotNull(mc.player)
                    player.position() to player.onGround()
                }
                if (onGround && position.subtract(last).horizontalDistance() < STILL) {
                    stops++
                }
                last = position
            }
        }
    }

    val position = context.playerPosition()
    check(position.distanceTo(destination) <= ARRIVAL) { "AutoWalk turned off at $position, not at $destination" }
    check(stops <= MAX_STOPS) { "Stood still on the ground for $stops ticks on the way" }
}
