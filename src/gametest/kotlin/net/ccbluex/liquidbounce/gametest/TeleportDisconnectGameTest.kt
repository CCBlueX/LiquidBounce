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
import net.ccbluex.liquidbounce.features.module.modules.movement.ModuleTeleport
import net.ccbluex.liquidbounce.utils.client.mc
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext

/** A queued teleport belongs to the connection where it was requested. */
class TeleportDisconnectGameTest : FabricClientGameTest {

    override fun runTest(context: ClientGameTestContext) {
        context.waitForClient()
        val saved = context.fromClient { ConfigSystem.serializeValueGroup(ModuleTeleport) }

        try {
            context.worldBuilder().create().use {
                context.onClient {
                    ModuleTeleport.enabled = false
                    ModuleTeleport.settings.getValue("FunctionAfterTeleports").setByString("2")
                    val player = checkNotNull(mc.player)
                    ModuleTeleport.indicateTeleport(player.x + 128, player.y, player.z + 128)
                    check(ModuleTeleport.enabled) { "The test did not arm a delayed teleport" }
                }
            }

            check(context.fromClient { !ModuleTeleport.enabled }) { "A pending teleport survived disconnect" }

            context.worldBuilder().create().use { world ->
                world.server.runCommand("tp @a 8.0 ~ 8.0")
                context.waitFor({ client -> client.player?.let { it.x == 8.0 && it.z == 8.0 } == true }, 200)
                world.server.runCommand("tp @a 9.0 ~ 9.0")
                context.waitFor({ client -> client.player?.let { it.x == 9.0 && it.z == 9.0 } == true }, 200)
                check(context.fromClient { !ModuleTeleport.enabled }) { "Joining reactivated the old teleport" }

                // A fresh request still works in the new connection.
                context.onClient {
                    val player = checkNotNull(mc.player)
                    ModuleTeleport.indicateTeleport(12.0, player.y, 12.0)
                    check(ModuleTeleport.enabled)
                }
                world.server.runCommand("tp @a 10.0 ~ 10.0")
                context.waitFor({ client -> client.player?.let { it.x == 10.0 && it.z == 10.0 } == true }, 200)
                check(context.fromClient { ModuleTeleport.enabled }) { "The request completed before both corrections" }
                world.server.runCommand("tp @a 11.0 ~ 11.0")
                context.waitFor({ client ->
                    !ModuleTeleport.enabled && client.player?.let { it.x == 12.0 && it.z == 12.0 } == true
                }, 200)
            }
        } finally {
            context.onClient {
                ModuleTeleport.enabled = false
                ModuleTeleport.onDisabled()
                ConfigSystem.deserializeValueGroup(ModuleTeleport, saved)
            }
        }
    }

}
