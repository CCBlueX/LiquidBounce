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

import net.ccbluex.liquidbounce.gametest.waitForClient
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext
import java.nio.file.Path
import javax.imageio.ImageIO

/**
 * The HUD scale enlarges the theme's components: the watermark in the top left corner grows with it.
 */
class HudScaleGameTest : FabricClientGameTest {

    override fun runTest(context: ClientGameTestContext) {
        context.waitForClient()
        context.inVisualTestWorld {
            // Only grass behind the watermark, no white clouds
            context.input.lookAt(0f, LOOK_DOWN_PITCH)
            context.waitTicks(SETTLE_TICKS)
            val unscaled = watermarkWidth(context.takeScreenshot("visual_hud_scale_1"))
            check(unscaled > 0) { "no watermark on screen" }

            try {
                context.setValue("hud.scale", "1.5")
                context.waitTicks(SETTLE_TICKS)
                val scaled = watermarkWidth(context.takeScreenshot("visual_hud_scale_1_5"))

                check(scaled.toFloat() / unscaled in 1.4f..1.6f) {
                    "watermark $unscaled px wide unscaled, $scaled px at scale 1.5"
                }
            } finally {
                context.setValue("hud.scale", "1")
            }
        }
    }

    /**
     * Width of the white logo and lettering of the watermark, which sits above every other default component
     * in the left half of the screen.
     */
    private fun watermarkWidth(screenshot: Path): Int {
        val image = ImageIO.read(screenshot.toFile())
        var left = Int.MAX_VALUE
        var right = -1

        for (y in 0 until image.height / 6) {
            for (x in 0 until image.width / 2) {
                val rgb = image.getRGB(x, y)
                if ((0..2).all { channel -> (rgb shr channel * 8 and 0xFF) >= WHITE }) {
                    left = minOf(left, x)
                    right = maxOf(right, x)
                }
            }
        }

        return (right - left + 1).coerceAtLeast(0)
    }

    private companion object {
        const val SETTLE_TICKS = 40
        const val WHITE = 240
    }

}
