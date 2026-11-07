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

package net.ccbluex.liquidbounce.gametest

import net.ccbluex.liquidbounce.config.ConfigSystem
import net.ccbluex.liquidbounce.features.module.modules.movement.fly.ModuleFly
import net.ccbluex.liquidbounce.features.module.modules.player.ModuleAutoWalk
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext
import net.minecraft.client.Minecraft
import net.minecraft.world.level.levelgen.Heightmap
import net.minecraft.world.phys.Vec3

/**
 * `.goto` flies where walking cannot go: a creative player across a canyon into the void and over a wall wider
 * than the searched area, and a survival player with the Vanilla mode of Fly onto a pillar.
 */
class FlightNavigationGameTest : FabricClientGameTest {

    private val modules = listOf(ModuleAutoWalk, ModuleFly)

    override fun runTest(context: ClientGameTestContext) {
        context.waitForClient()
        val saved = context.fromClient { modules.map { ConfigSystem.serializeValueGroup(it) } }

        try {
            context.worldBuilder().create().use { world ->
                val server = world.server
                val y = server.computeOnServer<Int, RuntimeException> {
                    it.overworld().getHeight(Heightmap.Types.MOTION_BLOCKING, 8, 8)
                }
                buildCourse(server, y)
                server.runCommand("clear @a")

                checkCreative(context, server, y)
                checkFlyModule(context, server, y)
            }
        } finally {
            context.onClient {
                for ((module, config) in modules.zip(saved)) {
                    module.enabled = false
                    ConfigSystem.deserializeValueGroup(module, config)
                }
            }
        }
    }

    /**
     * A canyon down to the void at x 10..19, a stone wall twelve blocks high at x 40 and a stone pillar nine blocks
     * high at x 60..62, z 10..12. The ground is the superflat one at y - 1.
     */
    private fun buildCourse(server: TestServerContext, y: Int) {
        server.runCommand("fill 10 ${y - 4} -30 19 ${y - 1} 30 air")
        server.runCommand("fill 40 $y -30 40 ${y + 11} 30 stone")
        server.runCommand("fill 60 $y 10 62 ${y + 8} 12 stone")
    }

    private fun checkCreative(context: ClientGameTestContext, server: TestServerContext, y: Int) {
        server.runCommand("gamemode creative @a")
        moveTo(context, server, Vec3(4.5, y.toDouble(), 0.5))

        check(goto(context, Vec3(26.5, y.toDouble(), 0.5), "across the canyon")) {
            "Crossed the canyon without flying"
        }
        check(goto(context, Vec3(52.5, y.toDouble(), 0.5), "over the wall")) { "Crossed the wall without flying" }
        check(context.fromClient { checkNotNull(it.player).y } > y - 1) { "Fell into the canyon" }
    }

    private fun checkFlyModule(context: ClientGameTestContext, server: TestServerContext, y: Int) {
        server.runCommand("gamemode survival @a")
        moveTo(context, server, Vec3(54.5, y.toDouble(), 11.5))
        val health = server.computeOnServer<Float, RuntimeException> { it.playerList.players.first().health }

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
        val after = server.computeOnServer<Float, RuntimeException> { it.playerList.players.first().health }
        check(after >= health) { "Flying onto the pillar hurt the player from $health to $after" }
    }

    /**
     * Goes to [destination] with `.goto`; true if the player flew on the way.
     */
    private fun goto(context: ClientGameTestContext, destination: Vec3, what: String): Boolean {
        context.command("goto ${destination.x} ${destination.y} ${destination.z}")
        check(context.fromClient { ModuleAutoWalk.enabled }) { ".goto did not turn AutoWalk on" }

        var flew = false
        repeat(GOTO_TICKS) {
            val (arrived, flying) = context.fromClient { mc ->
                val player = checkNotNull(mc.player)
                !ModuleAutoWalk.enabled to (player.abilities.flying || !player.onGround())
            }
            flew = flew || flying
            if (arrived) {
                val position = context.fromClient { checkNotNull(it.player).position() }
                check(position.distanceTo(destination) <= ARRIVAL) { "AutoWalk stopped at $position going $what" }
                return flew
            }
            context.waitTick()
        }

        val position = context.fromClient { checkNotNull(it.player).position() }
        context.onClient { ModuleAutoWalk.enabled = false }
        error("Did not get $what to $destination within $GOTO_TICKS ticks, stopped at $position")
    }

    private fun moveTo(context: ClientGameTestContext, server: TestServerContext, position: Vec3) {
        server.runCommand("tp @a ${position.x} ${position.y} ${position.z} -90 0")
        context.waitFor({ client: Minecraft ->
            client.player?.position()?.distanceToSqr(position)?.let { it < EPSILON } == true
        }, WAIT)
        context.waitTicks(5)
    }

    private companion object {
        const val WAIT = 200
        const val EPSILON = 1.0E-4
        const val GOTO_TICKS = 900
        const val ARRIVAL = 1.1
        const val LAND_TICKS = 20
    }

}
