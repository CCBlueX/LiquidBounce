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
import net.ccbluex.liquidbounce.features.module.modules.movement.ModuleParkour
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext

/**
 * [ModuleParkour] walking south off a stone platform: by default it jumps at the last moment, with an EdgeDistance
 * range it jumps that far before the edge, at distances that change from jump to jump.
 */
class ParkourGameTest : FabricClientGameTest {

    private var standY = 0

    override fun runTest(context: ClientGameTestContext) {
        context.waitForClient()
        val saved = context.fromClient { ConfigSystem.serializeValueGroup(ModuleParkour) }

        try {
            context.worldBuilder().create().use { world ->
                val server = world.server
                server.runCommand("gamemode survival @a")
                context.waitFor({ client -> client.player?.onGround() == true }, SETTLE_TICKS)
                val platformY = server.groundY() + HEIGHT
                standY = platformY + 1
                server.runCommand("fill -2 $platformY 0 2 $platformY ${EDGE_Z - 1} stone")
                context.onClient { ModuleParkour.enabled = true }

                val lastMoment = takeOffDistance(context, server)
                check(lastMoment < LAST_MOMENT_SLACK) { "Parkour jumped ${lastMoment}m before the edge by default" }

                context.onClient {
                    ModuleParkour.settings.getValue("EdgeDistance").setByString("$MIN_DISTANCE..$MAX_DISTANCE")
                }

                val distances = List(RANDOM_JUMPS) { takeOffDistance(context, server) }
                check(distances.all { it in MIN_DISTANCE - STEP..MAX_DISTANCE + STEP }) {
                    "EdgeDistance $MIN_DISTANCE..$MAX_DISTANCE jumped at $distances"
                }
                check(distances.max() - distances.min() > STEP / 2) { "The edge distance did not change: $distances" }
            }
        } finally {
            context.onClient {
                ModuleParkour.enabled = false
                ConfigSystem.deserializeValueGroup(ModuleParkour, saved)
            }
        }
    }

    /**
     * Walks south from the platform's start and returns how far before the last spot on the platform the player
     * took off.
     */
    private fun takeOffDistance(context: ClientGameTestContext, server: TestServerContext): Double {
        server.runCommand("execute as @a run tp @s 0.5 $standY $START_Z 0 0")
        context.waitFor({ client ->
            val player = client.player
            player != null && player.onGround() && player.z == START_Z && player.y == standY.toDouble()
        }, SETTLE_TICKS)
        context.waitTicks(TELEPORT_SETTLE_TICKS)

        context.input.holdKey { options -> options.keyUp }

        try {
            var lastZ = START_Z
            repeat(WALK_TICKS) {
                context.waitTick()
                val (z, rising) = context.fromClient { client ->
                    val player = checkNotNull(client.player)
                    player.z to (!player.onGround() && player.y > standY)
                }

                if (rising) {
                    return EDGE_Z + HALF_WIDTH - lastZ
                }

                lastZ = z
            }
        } finally {
            context.input.releaseKey { options -> options.keyUp }
        }

        error("The player did not jump off the platform")
    }

    private companion object {
        // Far enough above the ground that Assist finds nothing below to land on
        const val HEIGHT = 8
        const val EDGE_Z = 6
        const val START_Z = 2.5
        const val HALF_WIDTH = 0.3

        const val MIN_DISTANCE = 0.2
        const val MAX_DISTANCE = 1.0
        const val RANDOM_JUMPS = 5

        // How far one walking tick moves the player, at most
        const val STEP = 0.25
        const val LAST_MOMENT_SLACK = 0.25

        const val SETTLE_TICKS = 200
        const val TELEPORT_SETTLE_TICKS = 5
        const val WALK_TICKS = 60
    }

}
