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
import net.ccbluex.liquidbounce.features.module.modules.player.ModuleNoRotateSet
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext
import net.minecraft.client.player.LocalPlayer
import kotlin.math.abs

private const val ROTATE_COMMAND = "rotate @a 90 90"
private const val SERVER_ROTATION = 90f
private const val AIM = 0f
private const val EPSILON = 0.01f
private const val SETTLE_TICKS = 20
private const val TIMEOUT_TICKS = 200
private const val APPLY_TICKS = 3
private const val REPEATS = 5

/**
 * `/rotate` reaches the client as [net.minecraft.network.protocol.game.ClientboundPlayerRotationPacket],
 * not as a position packet. NoRotateSet has to swallow that packet too, and has to keep the old rotation
 * in sync with the restored one, otherwise the camera snaps to the server-side angle.
 */
class NoRotateSetGameTest : FabricClientGameTest {

    override fun runTest(context: ClientGameTestContext) {
        context.waitForClient()
        val saved = context.fromClient { ConfigSystem.serializeValueGroup(ModuleNoRotateSet) }
        context.onClient { ModuleNoRotateSet.enabled = false }

        try {
            context.worldBuilder().create().use { world ->
                // Let the spawn corrections finish, they would otherwise overwrite the measured rotation.
                context.waitTicks(SETTLE_TICKS)

                // Without the module the server rotation has to arrive, otherwise the assertions prove nothing.
                world.server.runCommand(ROTATE_COMMAND)
                world.server.waitFor({ server ->
                    server.playerList.players.any { abs(it.yRot - SERVER_ROTATION) < EPSILON }
                }, TIMEOUT_TICKS)
                context.waitFor({ client ->
                    client.player?.let { abs(it.yRot - SERVER_ROTATION) < EPSILON } == true
                }, TIMEOUT_TICKS)

                context.onClient {
                    ModuleNoRotateSet.mode.setByString("SilentAccept")
                    ModuleNoRotateSet.enabled = true
                    aim(checkNotNull(it.player))
                }

                // A repeating command block rotates every tick, which used to make the camera jitter.
                repeat(REPEATS) {
                    world.server.runCommand(ROTATE_COMMAND)
                    context.waitTicks(APPLY_TICKS)
                    context.onClient {
                        val player = checkNotNull(it.player)
                        check(abs(player.yRot - AIM) < EPSILON && abs(player.xRot - AIM) < EPSILON) {
                            "Server rotation reached the camera: ${player.yRot}/${player.xRot}"
                        }
                        check(abs(player.yRotO - AIM) < EPSILON && abs(player.xRotO - AIM) < EPSILON) {
                            "Server rotation reached the old rotation: ${player.yRotO}/${player.xRotO}"
                        }
                    }
                }

                // ResetRotation has to hand the server rotation to the rotation engine instead of the camera.
                context.onClient {
                    ModuleNoRotateSet.mode.setByString("ResetRotation")
                    aim(checkNotNull(it.player))
                }
                world.server.runCommand(ROTATE_COMMAND)
                context.waitTicks(APPLY_TICKS)
                context.fromClient {
                    val target = checkNotNull(RotationManager.activeRotationTarget) {
                        "ResetRotation did not hand the server rotation to the rotation engine"
                    }
                    check(abs(target.rotation.yRot - SERVER_ROTATION) < EPSILON) {
                        "Rotation engine targets ${target.rotation.yRot} instead of the server rotation"
                    }
                }
            }
        } finally {
            context.onClient {
                ModuleNoRotateSet.enabled = false
                ConfigSystem.deserializeValueGroup(ModuleNoRotateSet, saved)
            }
        }
    }

    private fun aim(player: LocalPlayer) {
        player.yRot = AIM
        player.xRot = AIM
        player.setOldRot()
    }

}
