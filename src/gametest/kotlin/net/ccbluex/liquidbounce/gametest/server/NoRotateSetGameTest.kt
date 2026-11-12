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
package net.ccbluex.liquidbounce.gametest.server

import net.ccbluex.gametest.harness.GameTestScope
import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.features.module.modules.player.ModuleNoRotateSet
import net.ccbluex.liquidbounce.gametest.harness.LiquidBounceGameTest
import net.ccbluex.liquidbounce.gametest.harness.enable
import net.ccbluex.liquidbounce.gametest.restoring
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.minecraft.client.Minecraft
import net.minecraft.network.protocol.game.ClientboundPlayerRotationPacket
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.abs

private const val SERVER_ROTATION = 90f
private const val AIM = 0f
private const val EPSILON = 0.01f
private const val REPEATS = 5

/**
 * `/rotate` reaches the client as [ClientboundPlayerRotationPacket], not as a position packet, so NoRotateSet
 * has to handle that packet too. It also has to keep the old rotation in sync with the restored one, otherwise
 * the camera still snaps to the server-side angle.
 */
class NoRotateSetGameTest : LiquidBounceGameTest({
    val rotations = AtomicInteger()
    val listener = object : EventListener {}
    context.restoring(ModuleNoRotateSet) {
        listener.on(PacketEvent::class.java) {
            if (it.packet is ClientboundPlayerRotationPacket) {
                rotations.incrementAndGet()
            }
        }.use {
            rotate(rotations)
            awaitClient("the server rotation on the camera") {
                abs(checkNotNull(it.player).yRot - SERVER_ROTATION) < EPSILON
            }

            client {
                ModuleNoRotateSet.mode.setByString("SilentAccept")
                it.aim()
            }
            enable(ModuleNoRotateSet)

            // A repeating command block rotates every tick, which used to make the camera jitter
            repeat(REPEATS) {
                rotate(rotations)
                // Packets are handled on the next tick of the client
                ticks(2)
                client { it.checkCameraStaysAimed() }
            }

            // ResetRotation hands the server rotation to the rotation engine instead of the camera
            client {
                ModuleNoRotateSet.mode.setByString("ResetRotation")
                it.aim()
            }
            rotate(rotations)
            awaitClient("ResetRotation to hand the server rotation to the rotation engine", timeout = 5) {
                val target = RotationManager.activeRotationTarget?.rotation?.yRot
                target != null && abs(target - SERVER_ROTATION) < EPSILON
            }
        }
    }
})

/** Has the server rotate the player and waits until the client received it. */
private fun GameTestScope.rotate(rotations: AtomicInteger) {
    val before = rotations.get()
    run("rotate $playerName $SERVER_ROTATION $SERVER_ROTATION")
    awaitClient("the server rotation") { rotations.get() > before }
}

private fun Minecraft.aim() {
    val player = checkNotNull(player)
    player.yRot = AIM
    player.xRot = AIM
    player.setOldRot()
}

private fun Minecraft.checkCameraStaysAimed() {
    val player = checkNotNull(player)
    check(abs(player.yRot - AIM) < EPSILON && abs(player.xRot - AIM) < EPSILON) {
        "The server rotation reached the camera: ${player.yRot}/${player.xRot}"
    }
    check(abs(player.yRotO - AIM) < EPSILON && abs(player.xRotO - AIM) < EPSILON) {
        "The server rotation reached the old rotation: ${player.yRotO}/${player.xRotO}"
    }
}
