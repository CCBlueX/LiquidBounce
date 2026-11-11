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

import net.ccbluex.liquidbounce.features.module.modules.movement.ModuleElytraRecast
import net.ccbluex.liquidbounce.features.module.modules.player.ModuleAutoWalk
import net.ccbluex.liquidbounce.gametest.fromClient
import net.ccbluex.liquidbounce.gametest.onClient
import net.ccbluex.liquidbounce.gametest.player
import net.ccbluex.liquidbounce.gametest.waitForClient
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext
import net.minecraft.world.phys.Vec3

/**
 * `.goto` with an elytra over long distances, unhurt: with rockets between platforms over the void of the End, a
 * pillar in the way, and bouncing along the Nether roof with ElytraRecast, faster than sprinting.
 */
class ElytraTravelGameTest : FabricClientGameTest {

    override fun runTest(context: ClientGameTestContext) {
        if (!navigationGameTests) {
            return
        }
        context.waitForClient()
        context.restoring(ModuleAutoWalk, ModuleElytraRecast) {
            context.worldBuilder().create().use { world ->
                val server = world.server
                server.runCommand("gamemode survival @a")
                server.runCommand("clear @a")
                server.runCommand("item replace entity @a armor.chest with elytra")

                checkEndCruise(context, server)
                checkRoofBounce(context, server)
            }
        }
    }

    /** Platforms at x 300 and x 650, the second ten blocks higher, with a pillar reaching above both halfway. */
    private fun checkEndCruise(context: ClientGameTestContext, server: TestServerContext) {
        server.runCommand("item replace entity @a hotbar.0 with firework_rocket 64")
        server.runCommand("gamemode creative @a")
        for ((x, y) in listOf(PLATFORM_A to LOW, PLATFORM_B to LOW + RISE, PILLAR to null)) {
            server.runCommand("execute in minecraft:the_end run tp @a $x ${LOW + 40} 300")
            context.waitTicks(LOAD_TICKS)
            if (y == null) {
                server.runCommand(
                    "execute in minecraft:the_end run fill $x ${LOW - 30} 298 ${x + 2} ${LOW + 40} 302 obsidian"
                )
            } else {
                server.runCommand("execute in minecraft:the_end run fill ${x - 4} $y 296 ${x + 4} $y 304 end_stone")
            }
        }
        server.runCommand("kill @e[type=ender_dragon]")

        val start = Vec3(PLATFORM_A + 0.5, LOW + 1.0, 300.5)
        server.runCommand("execute in minecraft:the_end run tp @a ${start.x} ${start.y} ${start.z} -90 0")
        awaitPosition(context, start, LOAD_TICKS, NEAR_SQR)
        // Falling in creative still counts towards fall damage, so survival only starts once standing.
        server.runCommand("gamemode survival @a")
        context.waitTicks(SETTLE_TICKS)

        val goal = Vec3(PLATFORM_B + 0.5, LOW + RISE + 1.0, 300.5)
        val glided = goto(context, server, goal, "across the void", CRUISE_TICKS)
        check(glided) { "Crossed the void without flying" }
    }

    /** The natural roof of the Nether, flat bedrock with its top at y 127. */
    private fun checkRoofBounce(context: ClientGameTestContext, server: TestServerContext) {
        server.runCommand("clear @a firework_rocket")
        val start = Vec3(0.5, ROOF, 0.5)
        server.runCommand("execute in minecraft:the_nether run tp @a ${start.x} ${start.y} ${start.z} -90 0")
        awaitPosition(context, start, LOAD_TICKS, NEAR_SQR)
        context.onClient { ModuleElytraRecast.enabled = true }

        val goal = Vec3(BOUNCE_DISTANCE + 0.5, ROOF, 0.5)
        val began = context.fromClient { it.level!!.gameTime }
        val bounced = goto(context, server, goal, "along the roof", BOUNCE_TICKS)
        val ticks = context.fromClient { it.level!!.gameTime } - began
        check(bounced) { "Crossed the roof without the elytra" }

        val speed = BOUNCE_DISTANCE / (ticks / TICKS_PER_SECOND)
        println("Bounced $BOUNCE_DISTANCE blocks in $ticks ticks, $speed blocks per second")
        check(speed > MIN_BOUNCE_SPEED) { "Bounced at only $speed blocks per second" }
    }

    private fun goto(
        context: ClientGameTestContext,
        server: TestServerContext,
        goal: Vec3,
        what: String,
        ticks: Int,
    ): Boolean {
        val health = server.player { it.health }
        val flew = goto(context, goal, what, ticks, ARRIVAL) { it.isFallFlying }
        val after = server.player { it.health }
        check(after >= health) { "Going $what hurt the player from $health to $after" }
        return flew
    }

    private companion object {
        const val PLATFORM_A = 300
        const val PLATFORM_B = 650
        const val PILLAR = 475
        const val LOW = 100
        const val RISE = 10
        const val ROOF = 128.0
        const val BOUNCE_DISTANCE = 400.0
        const val MIN_BOUNCE_SPEED = 12.0
        const val TICKS_PER_SECOND = 20.0
        const val LOAD_TICKS = 60
        const val SETTLE_TICKS = 10
        const val CRUISE_TICKS = 1600
        const val BOUNCE_TICKS = 1600
        const val ARRIVAL = 1.5
        const val NEAR_SQR = 0.01
    }

}
