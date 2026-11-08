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

import com.mojang.blaze3d.platform.NativeImage
import net.ccbluex.liquidbounce.gametest.waitForClient
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext
import net.minecraft.util.ARGB
import net.minecraft.world.effect.MobEffects
import java.nio.file.Files
import kotlin.math.abs
import kotlin.math.max

/**
 * The duration bars of the theme's effects HUD: a bar shrinks with the time its effect has left, and the effect
 * blinks once it is about to run out.
 */
class HudEffectBarsGameTest : FabricClientGameTest {

    override fun runTest(context: ClientGameTestContext) {
        context.waitForClient()
        context.inVisualTestWorld { world ->
            // Without particles, which have the colors of the bars
            world.server.runCommand("effect give @a minecraft:strength $STRENGTH_SECONDS 0 true")
            world.server.runCommand("effect give @a minecraft:speed $SPEED_SECONDS 0 true")
            context.waitTicks(SETTLE_TICKS)

            val (strengthFull, speedFull) = context.barWidths("hud-effect-bars-full")
            check(strengthFull >= MIN_BAR_WIDTH && speedFull >= MIN_BAR_WIDTH) {
                "no bars for new effects: strength $strengthFull px, speed $speedFull px"
            }

            context.waitTicks(SPEED_TICKS / 2 - SETTLE_TICKS)
            val (strengthLater, speedHalf) = context.barWidths("hud-effect-bars-half")
            check(speedHalf * 2.0 in speedFull * 0.85..speedFull * 1.2) {
                "the speed bar went from $speedFull px to $speedHalf px with half of its time left"
            }
            check(strengthLater >= strengthFull * 0.9) {
                "the strength bar went from $strengthFull px to $strengthLater px with most of its time left"
            }

            context.waitTicks(SPEED_TICKS / 2 - EXPIRING_TICKS + SETTLE_TICKS)
            val expiring = List(BLINK_SHOTS) { shot ->
                context.waitTicks(BLINK_SHOT_INTERVAL)
                context.barWidths("hud-effect-bars-expiring-$shot")
            }
            check(expiring.all { (strength, _) -> strength >= MIN_BAR_WIDTH }) {
                "the strength effect blinked while not running out: $expiring"
            }
            check(expiring.any { (_, speed) -> speed >= MIN_BAR_WIDTH } &&
                expiring.any { (_, speed) -> speed < MIN_BAR_WIDTH }) {
                "the speed effect did not blink while running out: $expiring"
            }
        }
    }

    /**
     * Takes a screenshot and measures the strength and the speed bar in it: the longest row of pixels in the color
     * of the effect, which is gone while the effect is blinked out.
     */
    private fun ClientGameTestContext.barWidths(name: String): List<Int> =
        NativeImage.read(Files.newInputStream(takeScreenshot(name))).use { image ->
            listOf(MobEffects.STRENGTH, MobEffects.SPEED).map { effect ->
                (0 until image.height).maxOf { y -> image.longestRun(y, effect.value().color) }
            }
        }

    private fun NativeImage.longestRun(y: Int, color: Int): Int {
        var longest = 0
        var run = 0
        for (x in 0 until width) {
            run = if (isColor(getPixel(x, y), color)) run + 1 else 0
            longest = max(longest, run)
        }
        return longest
    }

    private fun isColor(pixel: Int, color: Int) =
        abs(ARGB.red(pixel) - ARGB.red(color)) <= COLOR_TOLERANCE &&
            abs(ARGB.green(pixel) - ARGB.green(color)) <= COLOR_TOLERANCE &&
            abs(ARGB.blue(pixel) - ARGB.blue(color)) <= COLOR_TOLERANCE

    private companion object {
        const val SETTLE_TICKS = 20

        const val STRENGTH_SECONDS = 600
        const val SPEED_SECONDS = 30
        const val SPEED_TICKS = SPEED_SECONDS * 20

        /** Effects with fewer ticks left blink. */
        const val EXPIRING_TICKS = 200

        /** Enough screenshots to see the effect both shown and blinked out within its blink of one second. */
        const val BLINK_SHOTS = 12
        const val BLINK_SHOT_INTERVAL = 3

        const val MIN_BAR_WIDTH = 10
        const val COLOR_TOLERANCE = 8
    }

}
