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
import net.ccbluex.liquidbounce.gametest.player
import net.ccbluex.liquidbounce.gametest.useAngleSmooth
import net.ccbluex.liquidbounce.gametest.waitForClient
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext
import net.minecraft.world.phys.Vec3
import kotlin.math.min

/** A survival player walks between two rooms joined only under water, unhurt and without running out of air. */
class WalkSwimGameTest : FabricClientGameTest {

    private lateinit var walker: TestWalker

    override fun runTest(context: ClientGameTestContext) {
        if (!navigationGameTests) {
            return
        }
        context.waitForClient()
        context.onClient {
            walker = TestWalker("TestSwimWalker", GOAL_RADIUS)
            useAngleSmooth(walker)
        }

        try {
            context.worldBuilder().create().use { world ->
                val server = world.server
                val ground = server.groundY()
                // Above the thin superflat ground, so the pools fit into the stone below the rooms.
                val y = ground + COURSE_HEIGHT
                buildCourse(server, y)
                server.runCommand("gamemode survival @a")
                server.runCommand("clear @a")
                server.runCommand("gamerule natural_health_regeneration false")
                walker.start(context)

                checkTunnel(context, server, y)
            }
        } finally {
            walker.stop(context)
        }
    }

    /** Two closed rooms in stone, each with a pool four blocks deep, joined by a flooded tunnel one block high. */
    private fun buildCourse(server: TestServerContext, y: Int) {
        server.runCommand("fill -2 ${y - 7} -6 30 ${y + 4} 6 stone")
        server.runCommand("fill 0 $y -3 10 ${y + 2} 3 air")
        server.runCommand("fill 16 $y -3 26 ${y + 2} 3 air")
        server.runCommand("fill 5 ${y - 4} -2 9 ${y - 1} 2 water")
        server.runCommand("fill 17 ${y - 4} -2 21 ${y - 1} 2 water")
        server.runCommand("fill 10 ${y - 4} 0 16 ${y - 4} 0 water")
    }

    private fun checkTunnel(context: ClientGameTestContext, server: TestServerContext, y: Int) {
        moveTo(context, server, Vec3(2.5, y.toDouble(), 0.5))
        val health = server.player { it.health }

        var swam = false
        var minAir = Int.MAX_VALUE
        walker.walk(
            context, Vec3(24.5, y.toDouble(), 0.5), ARRIVAL_TICKS,
            title = "Swimming through the tunnel", failure = "Did not swim through the tunnel",
        ) {
            swam = swam || it.isSwimming
            minAir = min(minAir, it.airSupply)
        }

        val after = server.player { it.health }
        check(after >= health) { "Swimming through the tunnel hurt the player from $health to $after" }
        check(minAir > 0) { "The player ran out of air in the tunnel" }
        check(swam) { "Went through the tunnel without the swimming pose" }
    }

    private companion object {
        const val COURSE_HEIGHT = 10
        const val ARRIVAL_TICKS = 800
        const val GOAL_RADIUS = 0.8
    }

}
