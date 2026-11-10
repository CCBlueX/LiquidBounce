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
import net.ccbluex.liquidbounce.gametest.waitForClient
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext
import net.minecraft.client.Minecraft
import net.minecraft.world.entity.animal.equine.Horse
import net.minecraft.world.entity.vehicle.boat.AbstractBoat
import net.minecraft.world.phys.Vec3

/**
 * `.goto` rides an oak boat around an island across a lake, gets off at the shore and walks onto the destination,
 * and rides a saddled horse up a step, around a fenced wall and up a ledge it jumps, staying on the horse.
 */
class RideNavigationGameTest : FabricClientGameTest {

    override fun runTest(context: ClientGameTestContext) {
        if (!navigationGameTests) {
            return
        }
        context.waitForClient()
        context.restoring(ModuleAutoWalk) {
            context.worldBuilder().create().use { world ->
                val server = world.server
                val y = server.groundY()
                buildLake(server, y)
                buildField(server, y)
                server.runCommand("gamemode survival @a")
                server.runCommand("clear @a")

                checkBoat(context, server, y)
                checkHorse(context, server, y)
            }
        }
    }

    /** A lake one block deep from x 4 to 34 with a stone island at x 16..22, in grass ground at y - 1. */
    private fun buildLake(server: TestServerContext, y: Int) {
        server.runCommand("fill 0 ${y - 3} -14 50 ${y - 2} 14 stone")
        server.runCommand("fill 0 ${y - 1} -14 50 ${y - 1} 14 grass_block")
        server.runCommand("fill 0 $y -14 50 ${y + 6} 14 air")
        server.runCommand("fill 4 ${y - 1} -10 34 ${y - 1} 10 water")
        server.runCommand("fill 16 ${y - 1} -4 22 ${y + 1} 4 stone")
    }

    /**
     * Grass ground with a stone floor one block higher from x 18 on, a fenced wall across it at x 28 from z 36 on,
     * and a ledge two blocks higher from x 38 on.
     */
    private fun buildField(server: TestServerContext, y: Int) {
        server.runCommand("fill 0 ${y - 1} 26 50 ${y - 1} 64 grass_block")
        server.runCommand("fill 0 $y 26 50 ${y + 8} 64 air")
        server.runCommand("fill 18 $y 30 50 $y 60 stone")
        server.runCommand("fill 28 ${y + 1} 36 28 ${y + 1} 60 stone")
        server.runCommand("fill 28 ${y + 2} 36 28 ${y + 2} 60 oak_fence")
        server.runCommand("fill 38 ${y + 1} 30 50 ${y + 2} 60 stone")
    }

    private fun checkBoat(context: ClientGameTestContext, server: TestServerContext, y: Int) {
        moveTo(context, server, Vec3(8.5, y.toDouble(), 0.5), yaw = -90f)
        server.runCommand("summon oak_boat 8.5 ${y - 0.4} 0.5 {Rotation:[-90f,0f]}")
        mount(context, server, "oak_boat")

        val destination = Vec3(42.5, y.toDouble(), 0.5)
        var farthestAfloat = Double.NEGATIVE_INFINITY
        unhurt(server, "riding the boat") {
            context.command("goto 42 $y 0")
            check(context.fromClient { ModuleAutoWalk.enabled }) { ".goto did not turn AutoWalk on" }

            awaitPlayer(context, BOAT_TICKS, "Did not arrive across the lake") { player ->
                val boat = player.vehicle as? AbstractBoat
                if (boat != null) {
                    farthestAfloat = maxOf(farthestAfloat, boat.x)
                }
                !ModuleAutoWalk.enabled
            }
        }

        val (position, riding) = context.fromClient { mc ->
            val player = checkNotNull(mc.player)
            player.position() to player.isPassenger
        }
        check(farthestAfloat >= SHORE_X) { "The boat only got to x $farthestAfloat" }
        check(!riding) { "The player stayed in the boat" }
        check(position.distanceTo(destination) <= ARRIVAL) { "AutoWalk turned off at $position, not at $destination" }
        server.runCommand("kill @e[type=oak_boat]")
    }

    private fun checkHorse(context: ClientGameTestContext, server: TestServerContext, y: Int) {
        moveTo(context, server, Vec3(6.5, y.toDouble(), 50.5), yaw = -90f)
        server.runCommand(
            "summon horse 6.5 $y 50.5 {Tame:1b,PersistenceRequired:1b,Rotation:[-90f,0f]," +
                "equipment:{saddle:{id:\"minecraft:saddle\",count:1}}," +
                "attributes:[{id:\"minecraft:jump_strength\",base:0.6d},{id:\"minecraft:movement_speed\",base:0.25d}]}"
        )
        mount(context, server, "horse")
        check(context.fromClient { checkNotNull(it.player).controlledVehicle is Horse }) {
            "The player does not control the horse"
        }

        val destination = Vec3(44.5, y + 3.0, 50.5)
        unhurt(server, "riding the horse") {
            context.command("goto 44 ${y + 3} 50")
            check(context.fromClient { ModuleAutoWalk.enabled }) { ".goto did not turn AutoWalk on" }
            awaitPlayer(context, HORSE_TICKS, "Did not ride up to the ledge") { !ModuleAutoWalk.enabled }
        }

        val (position, riding) = context.fromClient { mc ->
            val player = checkNotNull(mc.player)
            player.rootVehicle.position() to (player.vehicle is Horse)
        }
        check(riding) { "The player got off the horse" }
        check(position.distanceTo(destination) <= ARRIVAL) { "AutoWalk turned off at $position, not at $destination" }
        server.runCommand("ride @p dismount")
        server.runCommand("kill @e[type=horse]")
    }

    private fun mount(context: ClientGameTestContext, server: TestServerContext, type: String) {
        server.runCommand("ride @p mount @e[type=$type,limit=1,sort=nearest]")
        context.waitFor({ client: Minecraft -> client.player?.isPassenger == true }, WAIT)
        context.waitTicks(10)
    }

    private companion object {
        const val WAIT = 200
        const val BOAT_TICKS = 1200
        const val HORSE_TICKS = 900
        const val SHORE_X = 30.0
        const val ARRIVAL = 1.1
    }

}
