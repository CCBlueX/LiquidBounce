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
import net.ccbluex.liquidbounce.features.module.modules.movement.ModuleParkour
import net.ccbluex.liquidbounce.gametest.harness.LiquidBounceGameTest
import net.ccbluex.liquidbounce.gametest.onClient
import net.minecraft.core.BlockPos
import net.minecraft.world.phys.Vec3

private const val HEIGHT = 24
private const val TIMEOUT = 600

/** Along +X: a gap of two, three, a jump up, one down, and two blocks off to the side. */
private val COURSE = listOf(
    BlockPos(0, 0, 0), BlockPos(1, 0, 0), BlockPos(4, 0, 0), BlockPos(8, 0, 0), BlockPos(11, 1, 0),
    BlockPos(15, 0, 0), BlockPos(18, 0, 1), BlockPos(21, 0, 0), BlockPos(22, 0, 0),
)

/**
 * A player who only holds forward and sprint along a Jump 'n' Run course over a lake makes it to the end with
 * Parkour on, and falls into the lake without it.
 */
class ParkourAssistGameTest : LiquidBounceGameTest({
    try {
        val ground = origin.y
        val start = BlockPos(0, ground + HEIGHT, 0)
        run("effect give $playerName minecraft:saturation infinite 0 true")
        fill(BlockPos(-10, ground - 2, -20), BlockPos(40, ground - 1, 20), "minecraft:water")
        for (block in COURSE) {
            setBlock(start.offset(block), "minecraft:stone")
        }
        val last = start.offset(COURSE.last())
        awaitServer("block ${last.x} ${last.y} ${last.z} minecraft:stone")

        val unassisted = runCourse(start, assist = false)
        check(unassisted != null) { "Without Parkour the course was run to the end" }

        val failure = runCourse(start, assist = true)
        check(failure == null) { "With Parkour the run failed: $failure" }
    } finally {
        context.onClient { ModuleParkour.enabled = false }
    }
})

/** Runs the course from [origin] holding forward and sprint; why it failed, or null. */
private fun GameTestScope.runCourse(origin: BlockPos, assist: Boolean): String? {
    val goal = origin.offset(COURSE.last()).above()
    val lowest = origin.y + COURSE.minOf { it.y }

    moveTo(Vec3.atBottomCenterOf(origin.offset(COURSE.first()).above()), yaw = -90f)
    context.onClient { ModuleParkour.enabled = assist }

    input.holdKey { it.keyUp }
    input.holdKey { it.keySprint }
    try {
        val title = if (assist) "With Parkour" else "Without Parkour"
        return runCourse(context, title, "ASSIST", lowest, TIMEOUT) { BlockPos.containing(it) == goal }
    } finally {
        input.releaseKey { it.keyUp }
        input.releaseKey { it.keySprint }
        context.onClient { ModuleParkour.enabled = false }
    }
}
