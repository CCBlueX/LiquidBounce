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
import net.ccbluex.liquidbounce.gametest.onClient
import net.ccbluex.liquidbounce.gametest.waitForClient
import net.ccbluex.liquidbounce.integration.theme.ThemeManager
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext
import net.minecraft.core.Holder
import net.minecraft.util.ARGB
import net.minecraft.world.effect.MobEffect
import net.minecraft.world.effect.MobEffects
import java.nio.file.Files
import kotlin.math.abs
import kotlin.math.max

/**
 * The theme's effects HUD shows levels as roman numerals on request, and pulses effects red with a red bar in their
 * last five seconds.
 */
class HudEffectDetailsGameTest : FabricClientGameTest {

    override fun runTest(context: ClientGameTestContext) {
        context.waitForClient()
        context.inVisualTestWorld { world ->
            // Spectators have no hand in view, whose skin could pass for a red bar
            world.server.runCommand("gamemode spectator @a")
            // Without particles, which have the colors of the bars
            world.server.runCommand("effect give @a minecraft:speed 600 $SPEED_AMPLIFIER true")
            context.waitTicks(SETTLE_TICKS)
            val arabic = context.levelWidth("hud-effect-details-arabic")

            context.setRomanNumerals(true)
            try {
                context.waitTicks(SETTLE_TICKS)
                val roman = context.levelWidth("hud-effect-details-roman")
                check(roman * 2 >= arabic * 3) { "level VIII is $roman px wide, level 8 is $arabic px" }
            } finally {
                context.setRomanNumerals(false)
            }

            world.server.runCommand("effect clear @a")
            world.server.runCommand("effect give @a minecraft:strength $STRENGTH_SECONDS 0 true")
            world.server.runCommand("effect give @a minecraft:regeneration $REGENERATION_SECONDS 0 true")
            context.waitTicks(SETTLE_TICKS)

            val shots = List(PULSE_SHOTS) { shot ->
                context.waitTicks(PULSE_SHOT_INTERVAL)
                context.screenshot("hud-effect-details-critical-$shot") { image ->
                    Bars(
                        critical = image.longestRun(::isCriticalRed),
                        regeneration = image.longestRun(MobEffects.REGENERATION),
                        strength = image.longestRun(MobEffects.STRENGTH),
                    )
                }
            }
            check(shots.all { it.critical >= MIN_BAR_WIDTH && it.regeneration < MIN_BAR_WIDTH }) {
                "the regeneration bar did not stay red while running out: $shots"
            }
            check(shots.any { it.strength >= MIN_BAR_WIDTH } && shots.any { it.strength < MIN_BAR_WIDTH }) {
                "the strength effect did not keep blinking in its own color: $shots"
            }
        }
    }

    private data class Bars(val critical: Int, val regeneration: Int, val strength: Int)

    private fun ClientGameTestContext.setRomanNumerals(enabled: Boolean) = onClient {
        checkNotNull(ThemeManager.theme).components.first { it.name == "Effects" }
            .containedValues.first { it.name == "RomanNumerals" }.setByString(enabled.toString())
    }

    private fun <T> ClientGameTestContext.screenshot(name: String, measure: (NativeImage) -> T): T =
        NativeImage.read(Files.newInputStream(takeScreenshot(name))).use(measure)

    /**
     * The width of the level next to the only effect shown: the span of the theme's error red, which only the level
     * text has.
     */
    private fun ClientGameTestContext.levelWidth(name: String): Int = screenshot(name) { image ->
        val text = (0 until image.height).flatMap { y ->
            (0 until image.width).filter { x -> isColor(image.getPixel(x, y), ERROR_RED) }
        }
        check(text.isNotEmpty()) { "no level shown next to the speed effect" }
        text.max() - text.min() + 1
    }

    private fun NativeImage.longestRun(effect: Holder<MobEffect>): Int {
        val color = effect.value().color
        return longestRun { isColor(it, color) }
    }

    private fun NativeImage.longestRun(matches: (Int) -> Boolean): Int =
        (0 until height).maxOf { y -> longestRun(y, matches) }

    private fun NativeImage.longestRun(y: Int, matches: (Int) -> Boolean): Int {
        var longest = 0
        var run = 0
        for (x in 0 until width) {
            run = if (matches(getPixel(x, y))) run + 1 else 0
            longest = max(longest, run)
        }
        return longest
    }

    private fun isColor(pixel: Int, color: Int) =
        abs(ARGB.red(pixel) - ARGB.red(color)) <= COLOR_TOLERANCE &&
            abs(ARGB.green(pixel) - ARGB.green(color)) <= COLOR_TOLERANCE &&
            abs(ARGB.blue(pixel) - ARGB.blue(color)) <= COLOR_TOLERANCE

    /**
     * The red of a running out bar, also while faded by the pulse.
     */
    private fun isCriticalRed(pixel: Int) =
        ARGB.red(pixel) >= MIN_RED && ARGB.red(pixel) > 2 * ARGB.green(pixel) && ARGB.red(pixel) > 2 * ARGB.blue(pixel)

    private companion object {
        const val SETTLE_TICKS = 20

        /** Level 8, which is VIII in roman numerals. */
        const val SPEED_AMPLIFIER = 7

        /** Expiring, but still above five seconds after all screenshots. */
        const val STRENGTH_SECONDS = 9

        /** Below five seconds. */
        const val REGENERATION_SECONDS = 4

        /**
         * Enough screenshots to see the strength effect both shown and blinked out within its blink of a second, and
         * few enough to still find the shrinking regeneration bar in the last one.
         */
        const val PULSE_SHOTS = 8
        const val PULSE_SHOT_INTERVAL = 3

        /** Longer than any stroke of the level text, which shares the red of a running out bar. */
        const val MIN_BAR_WIDTH = 40
        const val COLOR_TOLERANCE = 8
        const val MIN_RED = 100
        const val ERROR_RED = 0xFC4130
    }

}
