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

import net.ccbluex.liquidbounce.features.module.modules.movement.fly.ModuleFly
import net.ccbluex.liquidbounce.features.module.modules.player.ModuleAutoWalk
import net.ccbluex.liquidbounce.gametest.command
import net.ccbluex.liquidbounce.gametest.fromClient
import net.ccbluex.liquidbounce.gametest.groundY
import net.ccbluex.liquidbounce.gametest.onClient
import net.ccbluex.liquidbounce.gametest.player
import net.ccbluex.liquidbounce.gametest.playerPosition
import net.ccbluex.liquidbounce.gametest.waitForClient
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext
import net.minecraft.world.phys.Vec3

/**
 * `.goto` flies where walking cannot go: a creative player across a canyon into the void and over a wall wider
 * than the searched area, a survival player with the Vanilla mode of Fly onto a pillar, and a survival player with
 * an elytra from the top of a tower down to the ground far away, unhurt.
 */
class FlightNavigationGameTest : FabricClientGameTest {

    override fun runTest(context: ClientGameTestContext) {
        if (!navigationGameTests) {
            return
        }
        context.waitForClient()
        context.restoring(ModuleAutoWalk, ModuleFly) {
            context.worldBuilder().create().use { world ->
                val server = world.server
                val y = server.groundY()
                buildCourse(server, y)
                server.runCommand("clear @a")

                checkCreative(context, server, y)
                checkFlyModule(context, server, y)
                checkElytra(context, server, y)
            }
        }
    }

    /** A canyon down to the void at x 10..19, a wall twelve blocks high at x 40, a pillar and a tower. */
    private fun buildCourse(server: TestServerContext, y: Int) {
        server.runCommand("fill 10 ${y - 4} -30 19 ${y - 1} 30 air")
        server.runCommand("fill 40 $y -30 40 ${y + 11} 30 stone")
        server.runCommand("fill 60 $y 10 62 ${y + 8} 12 stone")
        server.runCommand("fill 0 $y -52 2 ${y + 39} -50 stone")
    }

    private fun checkCreative(context: ClientGameTestContext, server: TestServerContext, y: Int) {
        server.runCommand("gamemode creative @a")
        moveTo(context, server, Vec3(4.5, y.toDouble(), 0.5), yaw = -90f)

        check(goto(context, Vec3(26.5, y.toDouble(), 0.5), "across the canyon")) {
            "Crossed the canyon without flying"
        }
        check(goto(context, Vec3(52.5, y.toDouble(), 0.5), "over the wall")) { "Crossed the wall without flying" }
        check(context.fromClient { checkNotNull(it.player).y } > y - 1) { "Fell into the canyon" }
    }

    private fun checkFlyModule(context: ClientGameTestContext, server: TestServerContext, y: Int) {
        server.runCommand("gamemode survival @a")
        moveTo(context, server, Vec3(54.5, y.toDouble(), 11.5), yaw = -90f)
        val health = server.player { it.health }

        context.onClient {
            ModuleFly.containedValues.first { it.name == "Mode" }.setByString("Vanilla")
            ModuleFly.enabled = true
        }
        try {
            goto(context, Vec3(61.5, y + 9.0, 11.5), "onto the pillar")
        } finally {
            context.onClient { ModuleFly.enabled = false }
        }

        context.waitTicks(LAND_TICKS)
        val (position, onGround) = context.fromClient { mc ->
            val player = checkNotNull(mc.player)
            player.position() to player.onGround()
        }
        check(onGround && position.y >= y + 9.0) { "Did not land on the pillar, but at $position" }
        val after = server.player { it.health }
        check(after >= health) { "Flying onto the pillar hurt the player from $health to $after" }
    }

    private fun checkElytra(context: ClientGameTestContext, server: TestServerContext, y: Int) {
        server.runCommand("item replace entity @a armor.chest with elytra")
        moveTo(context, server, Vec3(1.5, y + 40.0, -50.5), yaw = -90f)
        val health = server.player { it.health }

        val destination = Vec3(71.5, y.toDouble(), -50.5)
        context.command("goto ${destination.x} ${destination.y} ${destination.z}")
        var glided = false
        repeat(GOTO_TICKS) {
            val (arrived, gliding) = context.fromClient { mc ->
                !ModuleAutoWalk.enabled to checkNotNull(mc.player).isFallFlying
            }
            glided = glided || gliding
            if (arrived) {
                val position = context.playerPosition()
                check(position.distanceTo(destination) <= ARRIVAL) { "AutoWalk stopped at $position" }
                check(glided) { "Got down from the tower without gliding" }

                val after = server.player { it.health }
                check(after >= health) { "Gliding down hurt the player from $health to $after" }
                return
            }
            context.waitTick()
        }

        val position = context.playerPosition()
        context.onClient { ModuleAutoWalk.enabled = false }
        error("Did not glide to $destination within $GOTO_TICKS ticks, stopped at $position")
    }

    private fun goto(context: ClientGameTestContext, destination: Vec3, what: String): Boolean =
        goto(context, destination, what, GOTO_TICKS, ARRIVAL) { it.abilities.flying || !it.onGround() }

    private companion object {
        const val GOTO_TICKS = 900
        const val ARRIVAL = 1.1
        const val LAND_TICKS = 20
    }

}
