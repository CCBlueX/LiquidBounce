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

import com.mojang.blaze3d.platform.NativeImage
import net.ccbluex.liquidbounce.config.ConfigSystem
import net.ccbluex.liquidbounce.config.types.group.ModeValueGroup
import net.ccbluex.liquidbounce.features.blink.BlinkManager
import net.ccbluex.liquidbounce.features.module.modules.player.ModuleBlink
import net.ccbluex.liquidbounce.utils.aiming.data.Rotation
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext
import net.minecraft.client.player.LocalPlayer
import net.minecraft.util.ARGB
import net.minecraft.world.phys.Vec3
import java.nio.file.Files
import kotlin.math.hypot

/**
 * A player in first person who walks away while Blink holds the packets back sees the ring on the ground where
 * the server still has them, and no longer once the packets are sent.
 */
class BlinkEspRingGameTest : FabricClientGameTest {

    override fun runTest(context: ClientGameTestContext) {
        context.waitForClient()
        val saved = context.fromClient { ConfigSystem.serializeValueGroup(BlinkManager) }

        try {
            context.worldBuilder().create().use { world ->
                val server = world.server
                val start = Vec3(8.5, server.groundY().toDouble(), 8.5)
                server.runCommand("tp @a ${start.x} ${start.y} ${start.z} 0 0")
                context.waitTicks(SETTLE_TICKS)

                context.onClient {
                    selectRing()
                    ModuleBlink.enabled = true
                }
                context.input.holdKey { it.keyUp }
                context.waitTicks(WALK_TICKS)
                context.input.releaseKey { it.keyUp }
                context.waitTicks(SETTLE_TICKS)

                val held = server.player { it.position() }
                val walked = context.playerPosition().distanceTo(held)
                check(held.distanceTo(start) < NEAR && walked > MIN_WALK) {
                    "Blink did not hold the player at $start: the server has $held, $walked blocks behind"
                }

                context.onClient { client -> checkNotNull(client.player).aimAt(held) }
                context.waitTicks(SETTLE_TICKS)

                val ring = context.marker("blink-esp-ring")
                check(ring.pixels >= MIN_PIXELS) { "No ring where the server has the player: ${ring.pixels} pixels" }
                check(ring.offset <= MAX_OFFSET) { "The ring is not where the server has the player: $ring" }

                context.onClient { ModuleBlink.enabled = false }
                context.waitTicks(SETTLE_TICKS)

                val sent = context.marker("blink-esp-ring-sent")
                check(sent.pixels == 0) { "The ring stayed after the packets were sent: $sent" }
            }
        } finally {
            context.input.releaseKey { it.keyUp }
            context.onClient {
                ModuleBlink.enabled = false
                ConfigSystem.deserializeValueGroup(BlinkManager, saved)
            }
        }
    }

    private fun selectRing() {
        val esp = BlinkManager.containedValues.first { it.name == "Esp" } as ModeValueGroup<*>
        esp.setByString("Ring")
        esp.activeMode.containedValues
            .filter { it.name == "Color" || it.name == "OutlineColor" }
            .forEach { it.setByString(MARKER) }
    }

    private fun LocalPlayer.aimAt(point: Vec3) {
        val rotation = Rotation.lookingAt(point, from = eyePosition)
        yRot = rotation.yaw
        xRot = rotation.pitch
        setOldRot()
    }

    /**
     * How many pixels of a screenshot have the [MARKER] color, and how far their center is from the middle of
     * the screen as a fraction of its height.
     */
    private data class Marker(val pixels: Int, val offset: Double)

    private fun ClientGameTestContext.marker(name: String): Marker =
        Files.newInputStream(takeScreenshot(name)).use { NativeImage.read(it) }.use { image ->
            val marked = (0 until image.width * image.height).filter { index ->
                val pixel = image.getPixel(index % image.width, index / image.width)
                ARGB.red(pixel) >= BRIGHT && ARGB.blue(pixel) >= BRIGHT && ARGB.green(pixel) <= DARK
            }
            val offset = hypot(
                marked.map { it % image.width }.average() - image.width / 2.0,
                marked.map { it / image.width }.average() - image.height / 2.0,
            ) / image.height
            Marker(marked.size, offset)
        }

    private companion object {
        /** A color nothing else in the world has. */
        const val MARKER = "#FF00FF"
        const val BRIGHT = 200
        const val DARK = 80

        const val SETTLE_TICKS = 10
        const val WALK_TICKS = 20
        const val NEAR = 0.1
        const val MIN_WALK = 2.0
        const val MIN_PIXELS = 100
        const val MAX_OFFSET = 0.15
    }

}
