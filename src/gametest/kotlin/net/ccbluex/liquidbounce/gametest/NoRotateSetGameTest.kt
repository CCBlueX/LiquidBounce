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

import net.ccbluex.liquidbounce.features.module.modules.player.ModuleNoRotateSet
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext
import kotlin.math.abs

private const val TICKS = 3
private const val AIM = 0f

class NoRotateSetGameTest : FabricClientGameTest {

    override fun runTest(context: ClientGameTestContext) {
        context.waitForClient()
        context.onClient { ModuleNoRotateSet.enabled = false }

        try {
            context.worldBuilder().create().use { world ->
                // Without the module, the server rotation has to arrive - otherwise the test proves nothing.
                world.server.runCommand("rotate @a 90 90")
                context.waitFor(
                    { client -> client.player?.let { abs(it.yRot - 90f) < 0.01f } == true },
                    TICKS
                )

                context.onClient {
                    ModuleNoRotateSet.enabled = true
                    checkNotNull(it.player).apply {
                        yRot = AIM
                        xRot = AIM
                        setOldRot()
                    }
                }

                // A repeating command block rotates every tick, which used to make the camera jitter.
                repeat(5) {
                    world.server.runCommand("rotate @a 90 90")
                    context.waitTicks(TICKS)

                    context.onClient {
                        val player = checkNotNull(it.player)
                        check(abs(player.yRot - AIM) < 0.01f && abs(player.xRot - AIM) < 0.01f) {
                            "Server rotation reached the camera: ${player.yRot}/${player.xRot}"
                        }
                        check(abs(player.yRotO - AIM) < 0.01f && abs(player.xRotO - AIM) < 0.01f) {
                            "Server rotation reached the old rotation: ${player.yRotO}/${player.xRotO}"
                        }
                    }
                }
            }
        } finally {
            context.onClient { ModuleNoRotateSet.enabled = false }
        }
    }

}
