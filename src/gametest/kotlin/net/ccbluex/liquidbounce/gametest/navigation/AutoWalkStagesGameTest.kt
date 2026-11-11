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

import net.ccbluex.liquidbounce.features.module.modules.player.ModuleAutoWalk
import net.ccbluex.liquidbounce.gametest.command
import net.ccbluex.liquidbounce.gametest.fromClient
import net.ccbluex.liquidbounce.gametest.groundY
import net.ccbluex.liquidbounce.gametest.player
import net.ccbluex.liquidbounce.gametest.playerPosition
import net.ccbluex.liquidbounce.gametest.waitForClient
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext
import net.minecraft.world.phys.Vec3

/**
 * `.goto` walks to a destination far beyond the path range, across a field with three walls, in stages and without
 * stopping between them, and turns AutoWalk off on arrival.
 */
class AutoWalkStagesGameTest : FabricClientGameTest {

    override fun runTest(context: ClientGameTestContext) {
        if (!navigationGameTests) {
            return
        }
        context.waitForClient()
        context.restoring(ModuleAutoWalk) {
            context.worldBuilder().create().use { world ->
                val server = world.server
                val y = server.groundY()
                buildField(server, y)
                server.runCommand("gamemode survival @a")
                server.runCommand("clear @a")

                checkFarGoto(context, server, y)
            }
        }
    }

    /** A stone field from x -90 to 90 with walls at x -45, 0 and 45 that leave a gap on alternating sides. */
    private fun buildField(server: TestServerContext, y: Int) {
        server.runCommand("forceload add -96 -16 96 16")
        server.runCommand("fill -90 $y -14 90 ${y + 5} 14 air")
        server.runCommand("fill -90 ${y - 1} -14 90 ${y - 1} 14 stone")
        server.runCommand("fill -45 $y -14 -45 ${y + 2} 6 stone")
        server.runCommand("fill 0 $y -6 0 ${y + 2} 14 stone")
        server.runCommand("fill 45 $y -14 45 ${y + 2} 6 stone")
    }

    private fun checkFarGoto(context: ClientGameTestContext, server: TestServerContext, y: Int) {
        moveTo(context, server, Vec3(-75.5, y.toDouble(), 0.5), yaw = -90f)
        val destination = Vec3(75.5, y.toDouble(), 0.5)
        val health = server.player { it.health }

        context.command("goto ${destination.x} ${destination.y} ${destination.z}")
        check(context.fromClient { ModuleAutoWalk.enabled }) { ".goto did not turn AutoWalk on" }

        var stops = 0
        var last = context.playerPosition()
        var ticks = 0
        TurnWatch.watch(context, "Walking to the far destination") {
            while (context.fromClient { ModuleAutoWalk.enabled }) {
                check(ticks++ < MAX_TICKS) { "Did not arrive within $MAX_TICKS ticks, stopped at $last" }
                context.waitTick()
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

        val position = context.playerPosition()
        check(position.distanceTo(destination) <= ARRIVAL) { "AutoWalk turned off at $position, not at $destination" }

        val after = server.player { it.health }
        check(after >= health) { "Walking to the far destination hurt the player from $health to $after" }
        check(stops <= MAX_STOPS) { "Stood still on the ground for $stops ticks on the way" }
    }

    private companion object {
        const val MAX_TICKS = 1200
        const val STILL = 0.02
        const val MAX_STOPS = 5
        const val ARRIVAL = 1.1
    }

}
