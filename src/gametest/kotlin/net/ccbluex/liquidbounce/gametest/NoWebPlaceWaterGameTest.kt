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
import net.ccbluex.liquidbounce.config.types.group.ValueGroup
import net.ccbluex.liquidbounce.features.module.modules.movement.noweb.ModuleNoWeb
import net.ccbluex.liquidbounce.features.module.modules.movement.noweb.modes.NoWebPlaceWater
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext
import net.minecraft.world.item.Items

/**
 * The PlaceWater mode of [ModuleNoWeb]: with Crosshair it only empties the bucket once the player looks at the web,
 * Delay holds it back for the rolled ticks, and without PartialWebs a web that only reaches into the player's box
 * from the side is left alone.
 */
class NoWebPlaceWaterGameTest : FabricClientGameTest {

    private var groundY = 0

    override fun runTest(context: ClientGameTestContext) {
        context.waitForClient()
        val saved = context.fromClient { ConfigSystem.serializeValueGroup(ModuleNoWeb) }

        try {
            context.worldBuilder().create().use { world ->
                val server = world.server
                server.runCommand("gamemode survival @a")
                context.waitFor({ client -> client.player?.onGround() == true }, SETTLE_TICKS)
                groundY = context.fromClient { client -> checkNotNull(client.player).blockY }
                context.onClient {
                    ModuleNoWeb.modes.setByString("PlaceWater")
                    placeWater("Pickup", "Enabled", "false")
                    placeWater("Crosshair", "true")
                    ModuleNoWeb.enabled = true
                }

                // Crosshair: nothing while looking up, the bucket goes once the player looks down at the web
                reset(context, server, x = 0.5, pitch = -90)
                server.runCommand("setblock 0 $groundY 0 cobweb")
                check(ticksUntilPlaced(context, server, WAIT_TICKS) == null) { "Crosshair placed while looking away" }
                server.runCommand("execute as @a run tp @s 0.5 $groundY 0.5 0 90")
                check(ticksUntilPlaced(context, server, WAIT_TICKS) != null) { "Crosshair did not place on the web" }

                // Delay: the bucket waits the rolled ticks after walking into the web
                context.onClient {
                    placeWater("Crosshair", "false")
                    placeWater("Delay", "$MIN_DELAY..$MAX_DELAY")
                }
                reset(context, server, x = 0.5, pitch = 0)
                server.runCommand("setblock 0 $groundY 0 cobweb")
                val delayed = ticksUntilPlaced(context, server, WAIT_TICKS)
                check(delayed != null && delayed >= MIN_DELAY) { "Delay $MIN_DELAY..$MAX_DELAY placed after $delayed" }

                // PartialWebs: a web overlapping the box by 0.05 from the side
                context.onClient {
                    placeWater("Delay", "0..0")
                    placeWater("PartialWebs", "false")
                }
                reset(context, server, x = 0.75, pitch = 0)
                server.runCommand("setblock 1 $groundY 0 cobweb")
                check(ticksUntilPlaced(context, server, WAIT_TICKS) == null) { "A partly entered web was handled" }
                context.onClient { placeWater("PartialWebs", "true") }
                check(ticksUntilPlaced(context, server, WAIT_TICKS) != null) {
                    "PartialWebs ignored a partly entered web"
                }
            }
        } finally {
            context.onClient {
                ModuleNoWeb.enabled = false
                ConfigSystem.deserializeValueGroup(ModuleNoWeb, saved)
            }
        }
    }

    /**
     * Clears webs and water, puts a full water bucket in the hand and the player at [x] looking south at [pitch].
     */
    private fun reset(context: ClientGameTestContext, server: TestServerContext, x: Double, pitch: Int) {
        server.runCommand("fill -2 $groundY -2 2 ${groundY + 2} 2 air replace cobweb")
        server.runCommand("fill -2 $groundY -2 2 ${groundY + 2} 2 air replace water")
        server.runCommand("item replace entity @a hotbar.0 with water_bucket")
        server.runCommand("execute as @a run tp @s $x $groundY 0.5 0 $pitch")
        context.waitFor({ client ->
            val player = client.player
            player != null && player.mainHandItem.`is`(Items.WATER_BUCKET) && player.onGround()
        }, SETTLE_TICKS)
        context.waitTicks(SETTLE_WEB_TICKS)
    }

    /**
     * Ticks until the server sees the bucket emptied, null if it stays full for [maxTicks].
     */
    private fun ticksUntilPlaced(context: ClientGameTestContext, server: TestServerContext, maxTicks: Int): Int? {
        for (tick in 1..maxTicks) {
            context.waitTick()
            val emptied = server.player { player -> player.inventory.countItem(Items.BUCKET) > 0 }

            if (emptied) {
                return tick
            }
        }

        return null
    }

    private fun placeWater(vararg path: String) {
        var group: ValueGroup = NoWebPlaceWater
        for (name in path.dropLast(2)) {
            group = group.containedValues.first { it.name == name } as ValueGroup
        }
        group.containedValues.first { it.name == path[path.size - 2] }.setByString(path.last())
    }

    private companion object {
        const val MIN_DELAY = 8
        const val MAX_DELAY = 12

        const val SETTLE_TICKS = 200
        const val SETTLE_WEB_TICKS = 3
        const val WAIT_TICKS = 30
    }
}
