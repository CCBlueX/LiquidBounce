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

import net.ccbluex.liquidbounce.features.module.modules.movement.ModuleParkour
import net.ccbluex.liquidbounce.gametest.groundY
import net.ccbluex.liquidbounce.gametest.onClient
import net.ccbluex.liquidbounce.gametest.waitForClient
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext
import net.minecraft.core.BlockPos
import net.minecraft.world.phys.Vec3

/**
 * A player who only holds forward and sprint along a Jump 'n' Run course over a lake makes it to the end with
 * Parkour on, and falls into the lake without it.
 */
class ParkourAssistGameTest : FabricClientGameTest {

    override fun runTest(context: ClientGameTestContext) {
        if (!navigationGameTests) {
            return
        }
        context.waitForClient()

        try {
            context.worldBuilder().create().use { world ->
                val server = world.server
                val ground = server.groundY()
                val origin = BlockPos(0, ground + HEIGHT, 0)
                server.runCommand("gamemode survival @a")
                server.runCommand("effect give @a saturation infinite 0 true")
                server.runCommand("fill -10 ${ground - 2} -20 40 ${ground - 1} 20 water")
                for (block in COURSE) {
                    val pos = origin.offset(block)
                    server.runCommand("setblock ${pos.x} ${pos.y} ${pos.z} stone")
                }

                val unassisted = run(context, server, origin, assist = false)
                check(unassisted != null) { "Without Parkour the course was run to the end" }

                val failure = run(context, server, origin, assist = true)
                check(failure == null) { "With Parkour the run failed: $failure" }
            }
        } finally {
            context.onClient { ModuleParkour.enabled = false }
        }
    }

    private fun run(
        context: ClientGameTestContext,
        server: TestServerContext,
        origin: BlockPos,
        assist: Boolean,
    ): String? {
        val start = Vec3.atBottomCenterOf(origin.offset(COURSE.first()).above())
        val goal = origin.offset(COURSE.last()).above()
        val lowest = origin.y + COURSE.minOf { it.y }

        moveTo(context, server, start, yaw = -90f, settleTicks = SETTLE)
        context.onClient { ModuleParkour.enabled = assist }

        val input = context.input
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

    private companion object {
        const val HEIGHT = 24
        const val SETTLE = 10
        const val TIMEOUT = 600

        /** Along +X: a gap of two, three, a jump up, one down, and two blocks off to the side. */
        val COURSE = listOf(
            BlockPos(0, 0, 0), BlockPos(1, 0, 0), BlockPos(4, 0, 0), BlockPos(8, 0, 0), BlockPos(11, 1, 0),
            BlockPos(15, 0, 0), BlockPos(18, 0, 1), BlockPos(21, 0, 0), BlockPos(22, 0, 0),
        )
    }

}
