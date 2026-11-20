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

import net.ccbluex.gametest.harness.ServerGameTest
import net.ccbluex.gametest.harness.action.awaitServer
import net.ccbluex.gametest.harness.action.fill
import net.ccbluex.gametest.harness.action.setBlock
import net.ccbluex.liquidbounce.config.types.group.ToggleableValueGroup
import net.ccbluex.liquidbounce.features.module.modules.movement.ModuleParkour
import net.ccbluex.liquidbounce.gametest.fromClient
import net.ccbluex.liquidbounce.gametest.onClient
import net.minecraft.core.BlockPos
import net.minecraft.world.phys.Vec3

private const val HEIGHT = 24
private const val TIMEOUT = 400
/** Along +X: a run-up, gaps of one, two and three blocks and one down; each landing fits a sprint jump off its edge. */
private val COURSE = listOf(
    BlockPos(0, 0, 0), BlockPos(1, 0, 0), BlockPos(3, 0, 0), BlockPos(4, 0, 0), BlockPos(5, 0, 0),
    BlockPos(8, 0, 0), BlockPos(9, 0, 0), BlockPos(13, 0, 0), BlockPos(14, 0, 0), BlockPos(17, -1, 0),
    BlockPos(18, -1, 0), BlockPos(19, -1, 0),
)

/** With Assist off, Parkour still jumps at the very edge: holding forward and sprint gets across gaps over a lake. */
class ParkourEdgeGameTest : ServerGameTest({
    val assist = context.fromClient {
        ModuleParkour.containedValues.first { it.name == "Assist" } as ToggleableValueGroup
    }
    try {
        val ground = origin.y
        val start = BlockPos(0, ground + HEIGHT, 0)
        run("effect give $playerName minecraft:saturation infinite 0 true")
        fill(BlockPos(-10, ground - 2, -10), BlockPos(30, ground - 1, 10), "minecraft:water")
        for (block in COURSE) {
            setBlock(start.offset(block), "minecraft:stone")
        }
        val last = start.offset(COURSE.last())
        awaitServer("block ${last.x} ${last.y} ${last.z} minecraft:stone")

        val goal = last.above()
        moveTo(Vec3.atBottomCenterOf(start.above()), yaw = -90f)
        context.onClient {
            assist.enabled = false
            ModuleParkour.enabled = true
        }
        input.holdKey { it.keyUp }
        input.holdKey { it.keySprint }
        val failure = try {
            runCourse(context, "Edge jumps", "EDGE", start.y + COURSE.minOf { it.y }, TIMEOUT) {
                BlockPos.containing(it) == goal
            }
        } finally {
            input.releaseKey { it.keyUp }
            input.releaseKey { it.keySprint }
        }
        check(failure == null) { "With Assist off the run failed: $failure" }
    } finally {
        context.onClient {
            ModuleParkour.enabled = false
            assist.enabled = true
        }
    }
})
