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
import net.ccbluex.liquidbounce.features.module.modules.render.crosshair.ModuleCrosshair
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext
import net.minecraft.util.ARGB
import kotlin.math.hypot

/**
 * The pulse mode of [ModuleCrosshair] keeps its dot in the middle of the screen while the ring around it
 * shows up and fades out again.
 */
class CrosshairPulseGameTest : FabricClientGameTest {

    override fun runTest(context: ClientGameTestContext) {
        context.waitForClient()
        val saved = context.fromClient { ConfigSystem.serializeValueGroup(ModuleCrosshair) }

        try {
            context.worldBuilder().create().use { world ->
                world.server.runCommand("gamemode creative @a")
                world.connection.waitForChunksRender()

                context.onClient {
                    ModuleCrosshair.modes.setByString("Pulse")
                    setValue(ModuleCrosshair, "Mode", "Pulse", "DotRadius", to = "$DOT_RADIUS")
                    setValue(ModuleCrosshair, "Mode", "Pulse", "RingRadius", to = "$RING_START..$RING_END")
                    setValue(ModuleCrosshair, "Mode", "Pulse", "RingThickness", to = "$RING_THICKNESS")
                    setValue(ModuleCrosshair, "Mode", "Pulse", "Speed", to = "$SPEED")
                    setValue(ModuleCrosshair, "Mode", "Pulse", "Color", "Sync", to = "true")
                    setValue(ModuleCrosshair, "Mode", "Pulse", "Color", "FirstColor", to = "#FF00FF")
                    ModuleCrosshair.enabled = true
                }
                val (guiWidth, guiHeight) = context.fromClient { it.window.guiScaledWidth to it.window.guiScaledHeight }
                fun distance(x: Float, y: Float) = hypot((x - 0.5f) * guiWidth, (y - 0.5f) * guiHeight)

                val dot = context.countPixels("crosshair-pulse-dot", ::isMagenta) { x, y ->
                    distance(x, y) < DOT_RADIUS - 0.5f
                }[0]
                check(dot > 0) { "The pulse crosshair drew no dot" }

                val ring = sampleRing(context) { x, y -> distance(x, y) in RING_AREA }
                check(ring.max() > VISIBLE_PIXELS) { "The ring never showed up, tinted pixels per frame: $ring" }
                check(ring.min() < HIDDEN_PIXELS) { "The ring never faded out, tinted pixels per frame: $ring" }
            }
        } finally {
            context.onClient {
                ModuleCrosshair.enabled = false
                ConfigSystem.deserializeValueGroup(ModuleCrosshair, saved)
            }
        }
    }

    /**
     * Counts the ring's pixels on frames over more than one pulse; the pulse runs on wall-clock time.
     */
    private fun sampleRing(context: ClientGameTestContext, area: (Float, Float) -> Boolean): List<Int> {
        val start = System.currentTimeMillis()
        val counts = mutableListOf<Int>()
        while (System.currentTimeMillis() - start < SAMPLE_MILLIS) {
            counts += context.countPixels("crosshair-pulse-ring-${counts.size}", ::isMagentaTinted, area = area)[0]
        }
        return counts
    }

    /** Magenta blended at least half opaque over the sky or the ground. */
    private fun isMagentaTinted(pixel: Int) =
        ARGB.red(pixel) - ARGB.green(pixel) > TINT && ARGB.blue(pixel) - ARGB.green(pixel) > TINT

    private companion object {
        const val DOT_RADIUS = 2f
        const val RING_START = 5f
        const val RING_END = 13f
        const val RING_THICKNESS = 2f
        const val SPEED = 0.25f
        const val SAMPLE_MILLIS = 5000L
        const val TINT = 100
        const val VISIBLE_PIXELS = 30
        const val HIDDEN_PIXELS = 5

        val RING_AREA = DOT_RADIUS + 1f..RING_END + 1f
    }
}
