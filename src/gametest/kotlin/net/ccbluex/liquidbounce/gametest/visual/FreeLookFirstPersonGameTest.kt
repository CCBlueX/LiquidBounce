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
package net.ccbluex.liquidbounce.gametest.visual

import net.ccbluex.liquidbounce.features.module.modules.render.ModuleFreeLook
import net.ccbluex.liquidbounce.gametest.fromClient
import net.ccbluex.liquidbounce.gametest.onClient
import net.ccbluex.liquidbounce.gametest.waitForClient
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext
import net.minecraft.util.Mth
import kotlin.math.abs

/**
 * The first person perspective of [ModuleFreeLook]: the mouse turns the camera in the head, the player keeps
 * looking where he did, and looking down stops straight down instead of turning the view upside down.
 */
class FreeLookFirstPersonGameTest : FabricClientGameTest {

    override fun runTest(context: ClientGameTestContext) {
        context.waitForClient()
        context.inVisualTestWorld {
            context.setValue("freeLook.perspective", "FirstPerson")
            context.onClient { ModuleFreeLook.enabled = true }
            val (yaw, pitch) = context.fromClient { client -> client.player().yRot to client.player().xRot }

            moveMouse(context, CURSOR_STEP, 0.0)
            context.onClient { client ->
                val camera = client.gameRenderer.mainCamera()
                val player = client.player()
                check(!camera.isDetached) { "the first person camera left the head" }
                check(camera.position().distanceTo(player.eyePosition) < EPSILON) {
                    "the camera is at ${camera.position()}, not at the eyes ${player.eyePosition}"
                }
                check(abs(Mth.wrapDegrees(camera.yRot() - yaw)) > MIN_TURN) {
                    "moving the mouse only turned the camera from $yaw to ${camera.yRot()}"
                }
                check(abs(Mth.wrapDegrees(player.yRot - yaw)) < EPSILON) {
                    "the player turned from $yaw to ${player.yRot} with the camera"
                }
            }
            context.takeScreenshot("visual_freelook_first_person")

            moveMouse(context, 0.0, CURSOR_STEP)
            context.onClient { client ->
                val camera = client.gameRenderer.mainCamera()
                check(abs(camera.xRot() - STRAIGHT_DOWN) < EPSILON) {
                    "looking down ended at pitch ${camera.xRot()} instead of $STRAIGHT_DOWN"
                }
                check(abs(client.player().xRot - pitch) < EPSILON) {
                    "the player's pitch moved from $pitch to ${client.player().xRot} with the camera"
                }
            }

            context.onClient { ModuleFreeLook.enabled = false }
        }
    }

    private fun moveMouse(context: ClientGameTestContext, deltaX: Double, deltaY: Double) {
        repeat(MOVE_TICKS) {
            context.input.moveCursor(deltaX, deltaY)
            context.waitTick()
        }
    }

    private companion object {
        const val MOVE_TICKS = 10
        const val CURSOR_STEP = 100.0
        const val MIN_TURN = 30f
        const val STRAIGHT_DOWN = 90f
        const val EPSILON = 0.1f
    }

}
