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

/** A survival player sneaks through a low tunnel and along narrow bridges high over the ground, unhurt. */
class WalkSneakGameTest : FabricClientGameTest {

    private lateinit var walker: TestWalker

    override fun runTest(context: ClientGameTestContext) {
        if (!navigationGameTests) {
            return
        }
        context.waitForClient()
        context.onClient {
            walker = TestWalker("TestSneakWalker", GOAL_RADIUS)
            useAngleSmooth(walker)
        }

        try {
            context.worldBuilder().create().use { world ->
                val server = world.server
                val y = server.groundY()
                buildCourse(server, y)
                server.runCommand("gamemode survival @a")
                server.runCommand("clear @a")
                server.runCommand("gamerule natural_health_regeneration false")
                walker.start(context)

                checkLowTunnel(context, server, y)
                checkBridge(context, server, y)
                checkStraightBridge(context, server, y)
            }
        } finally {
            walker.stop(context)
        }
    }

    /**
     * A closed room at x 14..19, entered through a tunnel under top slabs, one and a half blocks high. Twenty blocks
     * up, a bridge one block wide that turns at x 42 towards z 10, and a straight one along z -10.
     */
    private fun buildCourse(server: TestServerContext, y: Int) {
        server.runCommand("fill 0 $y -20 60 ${y + 30} 20 air")
        server.runCommand("fill 0 ${y - 1} -20 60 ${y - 1} 20 stone")

        server.runCommand("fill 10 $y -3 20 ${y + 3} 3 stone")
        server.runCommand("fill 14 $y -2 19 ${y + 2} 2 air")
        server.runCommand("fill 10 $y 0 13 $y 0 air")
        server.runCommand("fill 10 ${y + 1} 0 13 ${y + 1} 0 stone_slab[type=top]")

        server.runCommand("fill 30 ${y + 20} -1 32 ${y + 20} 1 stone")
        server.runCommand("fill 33 ${y + 20} 0 42 ${y + 20} 0 stone")
        server.runCommand("fill 42 ${y + 20} 1 42 ${y + 20} 8 stone")
        server.runCommand("fill 41 ${y + 20} 9 43 ${y + 20} 11 stone")

        server.runCommand("fill 30 ${y + 20} -11 32 ${y + 20} -9 stone")
        server.runCommand("fill 33 ${y + 20} -10 45 ${y + 20} -10 stone")
    }

    private fun checkLowTunnel(context: ClientGameTestContext, server: TestServerContext, y: Int) {
        moveTo(context, server, Vec3(6.5, y.toDouble(), 0.5))
        walk(context, server, Vec3(17.5, y.toDouble(), 0.5), 600, "through the low tunnel")
    }

    private fun checkBridge(context: ClientGameTestContext, server: TestServerContext, y: Int) {
        moveTo(context, server, Vec3(31.5, y + 21.0, 0.5))
        walk(context, server, Vec3(42.5, y + 21.0, 10.5), 1200, "along the bridge")
    }

    private fun checkStraightBridge(context: ClientGameTestContext, server: TestServerContext, y: Int) {
        moveTo(context, server, Vec3(31.5, y + 21.0, -9.5))
        walk(context, server, Vec3(45.5, y + 21.0, -9.5), 1200, "along the straight bridge")
    }

    private fun walk(context: ClientGameTestContext, server: TestServerContext, goal: Vec3, ticks: Int, what: String) {
        val health = server.player { it.health }
        var sneaked = false
        walker.walk(context, goal, ticks, "Walking $what", "Did not walk $what") {
            sneaked = sneaked || it.isShiftKeyDown
        }

        val after = server.player { it.health }
        check(after >= health) { "Walking $what hurt the player from $health to $after" }
        check(sneaked) { "Walked $what without sneaking" }
    }

    private companion object {
        const val GOAL_RADIUS = 0.8
    }

}
