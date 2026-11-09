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
import net.ccbluex.liquidbounce.features.module.modules.render.esp.ModuleESP
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext
import net.minecraft.util.ARGB
import net.minecraft.world.entity.monster.zombie.Husk

/**
 * The 2D mode of [ModuleESP] around husks on a black stage: the fill opacity, the color of the health bar at full
 * health, the limit to the nearest target, and a box fading in and fading out where its husk was last seen.
 */
class Esp2DPolishGameTest : FabricClientGameTest {

    override fun runTest(context: ClientGameTestContext) {
        context.waitForClient()
        val saved = context.fromClient { ConfigSystem.serializeValueGroup(ModuleESP) }

        try {
            context.worldBuilder().create().use { world ->
                val y = buildStage(world.server)
                world.connection.waitForChunksRender()
                context.waitTicks(SETTLE_TICKS)

                context.onClient {
                    setValue(ModuleESP, "Mode", to = "2D")
                    setValue(ModuleESP, "ColorMode", to = "Static")
                    setValue(ModuleESP, "ColorMode", "Static", "Color", to = "#FF00FF")
                    ModuleESP.enabled = true
                }
                context.waitTicks(SETTLE_TICKS)

                context.checkFillOpacity()
                context.checkHealthBarColor()
                context.checkLimit(world.server, y)
                context.checkFade(world.server, y)
            }
        } finally {
            context.onClient {
                ModuleESP.enabled = false
                ConfigSystem.deserializeValueGroup(ModuleESP, saved)
            }
        }
    }

    /**
     * A husk in front of the player, between a black floor and a black wall.
     */
    private fun buildStage(server: TestServerContext): Int {
        val y = server.groundY()
        server.runCommand("gamerule spawn_mobs false")
        server.runCommand("difficulty easy")
        server.runCommand("gamemode creative @a")
        server.runCommand("tp @a 0.5 $y 0.5 -90 9")

        server.runCommand("fill -4 $y -16 12 ${y + 12} 16 air")
        server.runCommand("fill -4 ${y - 1} -16 12 ${y - 1} 16 black_concrete")
        server.runCommand("fill 12 $y -16 12 ${y + 12} 16 black_concrete")
        server.runCommand("kill @e[type=!player]")

        server.runCommand("summon husk 4.5 $y 0.5 $HUSK_DATA")
        return y
    }

    private fun ClientGameTestContext.checkFillOpacity() {
        onClient { setBoxValue("FillOpacity", to = "1") }
        val opaque = countPixels("esp-2d-fill-opaque", ::isMagenta)[0]
        onClient { setBoxValue("FillOpacity", to = "0.2") }
        val translucent = countPixels("esp-2d-fill-translucent", ::isMagenta)[0]

        check(opaque > translucent * MIN_FILL_RATIO) {
            "The opaque fill drew $opaque magenta pixels, the translucent one $translucent"
        }
    }

    private fun ClientGameTestContext.checkHealthBarColor() {
        onClient { setBoxValue("HealthBar", "HighColor", to = "#0000FF") }
        val (blue, green) = countPixels("esp-2d-health-high-color", ::isBlue, ::isGreen)

        check(blue > MIN_FEATURE_PIXELS && green == 0) {
            "The health bar of the unhurt husk drew $blue pixels in the high color and $green in the default one"
        }
    }

    /**
     * Two farther husks to the left and the right lose their boxes once only the nearest target is shown.
     */
    private fun ClientGameTestContext.checkLimit(server: TestServerContext, y: Int) {
        server.runCommand("summon husk 8.5 $y 4.5 $FARTHER_HUSK_DATA")
        server.runCommand("summon husk 8.5 $y -3.5 $FARTHER_HUSK_DATA")
        waitTicks(SETTLE_TICKS)

        val all = countPixels("esp-2d-limit-none", ::isMagenta, area = beside)[0]
        check(all > MIN_FEATURE_PIXELS) { "The boxes of the farther husks drew only $all pixels" }

        onClient { setBoxValue("Limit", to = "1") }
        val besideCount = countPixels("esp-2d-limit-one", ::isMagenta, area = beside)[0]
        val aheadCount = countPixels("esp-2d-limit-one-ahead", ::isMagenta, area = ahead)[0]
        check(besideCount == 0 && aheadCount > MIN_FEATURE_PIXELS) {
            "With a limit of one, the farther husks drew $besideCount pixels and the nearest one $aheadCount"
        }

        onClient { setBoxValue("Limit", to = "0") }
        server.runCommand("kill @e[tag=farther]")
        waitTicks(SETTLE_TICKS)
    }

    /**
     * The box fades in after Fade turns on, and fades out where the husk was once it left the client's world.
     */
    private fun ClientGameTestContext.checkFade(server: TestServerContext, y: Int) {
        onClient {
            setBoxValue("Fade", "InTime", to = "$FADE_MILLIS")
            setBoxValue("Fade", "OutTime", to = "$FADE_MILLIS")
            setBoxValue("Fade", "Enabled", to = "true")
        }
        val appearing = countPixels("esp-2d-fade-in", ::isMagenta, area = ahead)[0]
        waitMillis(FADE_MILLIS + FADE_MARGIN_MILLIS)
        val shown = countPixels("esp-2d-faded-in", ::isMagenta, area = ahead)[0]
        check(appearing * 2 < shown) { "The box did not fade in: $appearing magenta pixels at first, $shown later" }

        server.runCommand("tp @e[type=husk] 4.5 $y 400")
        waitFor({ client -> checkNotNull(client.level).entitiesForRendering().none { it is Husk } }, TIMEOUT_TICKS)
        val leaving = countPixels("esp-2d-fade-out", ::isMagentaTint, area = ahead)[0]
        check(leaving > MIN_FEATURE_PIXELS) { "The box vanished with its husk, only $leaving pixels were left" }

        waitMillis(FADE_MILLIS + FADE_MARGIN_MILLIS)
        val gone = countPixels("esp-2d-faded-out", ::isMagentaTint, area = ahead)[0]
        check(gone == 0) { "The box of the husk that left is still drawn with $gone pixels" }
    }

    private fun setBoxValue(vararg path: String, to: String) = setValue(ModuleESP, "Mode", "2D", *path, to = to)

    private fun ClientGameTestContext.waitMillis(millis: Long) {
        val end = System.currentTimeMillis() + millis
        waitFor({ System.currentTimeMillis() >= end }, TIMEOUT_TICKS)
    }

    private val ahead = { x: Float, _: Float -> x in AHEAD }

    private val beside = { x: Float, _: Float -> x !in AHEAD }

    private fun isBlue(pixel: Int) = ARGB.blue(pixel) > 235 && ARGB.red(pixel) < 20 && ARGB.green(pixel) < 20

    private fun isMagentaTint(pixel: Int) =
        ARGB.red(pixel) - ARGB.green(pixel) > 40 && ARGB.blue(pixel) - ARGB.green(pixel) > 40

    private companion object {
        const val HUSK_DATA = "{NoAI:1b,Silent:1b,PersistenceRequired:1b}"
        const val FARTHER_HUSK_DATA = "{NoAI:1b,Silent:1b,PersistenceRequired:1b,Tags:[\"farther\"]}"

        /**
         * The horizontal part of the screen around the nearest husk, relative to the screen width.
         */
        val AHEAD = 0.4f..0.6f

        const val SETTLE_TICKS = 20
        const val TIMEOUT_TICKS = 2400
        const val MIN_FEATURE_PIXELS = 40
        const val MIN_FILL_RATIO = 4
        const val FADE_MILLIS = 1500L
        const val FADE_MARGIN_MILLIS = 500L
    }

}
