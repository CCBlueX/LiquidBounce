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
import net.minecraft.network.protocol.game.ClientboundPlayerRotationPacket
import kotlin.math.abs

private const val SERVER_ROTATION = 90f
private const val AIM = 0f
private const val EPSILON = 0.01f
private const val REPEATS = 5

private val SERVER_ROTATION_PACKET = ClientboundPlayerRotationPacket(SERVER_ROTATION, false, SERVER_ROTATION, false)

/**
 * `/rotate` reaches the client as [ClientboundPlayerRotationPacket], not as a position packet, so NoRotateSet
 * has to handle that packet too. It also has to keep the old rotation in sync with the restored one, otherwise
 * the camera still snaps to the server-side angle.
 */
class NoRotateSetGameTest : FabricClientGameTest {

    override fun runTest(context: ClientGameTestContext) {
        context.waitForClient()
        val saved = context.fromClient { ConfigSystem.serializeValueGroup(ModuleNoRotateSet) }

        try {
            context.worldBuilder().create().use {
                context.onClient { mc ->
                    val player = checkNotNull(mc.player)
                    val connection = checkNotNull(mc.connection)

                    ModuleNoRotateSet.enabled = false
                    connection.handleRotatePlayer(SERVER_ROTATION_PACKET)
                    check(abs(player.yRot - SERVER_ROTATION) < EPSILON) {
                        "The server rotation did not reach the camera: ${player.yRot}"
                    }

                    ModuleNoRotateSet.mode.setByString("SilentAccept")
                    ModuleNoRotateSet.enabled = true
                    aim(player)

                    // A repeating command block rotates every tick, which used to make the camera jitter.
                    repeat(REPEATS) {
                        connection.handleRotatePlayer(SERVER_ROTATION_PACKET)
                        checkCameraStaysAimed(player)
                    }

                    // ResetRotation hands the server rotation to the rotation engine instead of the camera.
                    ModuleNoRotateSet.mode.setByString("ResetRotation")
                    aim(player)
                    connection.handleRotatePlayer(SERVER_ROTATION_PACKET)

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

    private fun checkCameraStaysAimed(player: LocalPlayer) {
        check(abs(player.yRot - AIM) < EPSILON && abs(player.xRot - AIM) < EPSILON) {
            "The server rotation reached the camera: ${player.yRot}/${player.xRot}"
        }
        check(abs(player.yRotO - AIM) < EPSILON && abs(player.xRotO - AIM) < EPSILON) {
            "The server rotation reached the old rotation: ${player.yRotO}/${player.xRotO}"
        }
    }

}
