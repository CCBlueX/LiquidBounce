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
import net.ccbluex.liquidbounce.features.module.modules.render.esp.ModuleESP
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext
import net.minecraft.util.ARGB
import org.joml.Vector2i
import java.nio.file.Files

/**
 * The 2D mode of [ModuleESP] around a husk: the outline as a box, as corners, as brackets and rounded, the health
 * bar on each side of the box, the gradient from the target color at the top to a second color at the bottom, and
 * the glow around the box.
 */
class Esp2DBoxOptionsGameTest : FabricClientGameTest {

    override fun runTest(context: ClientGameTestContext) {
        context.waitForClient()
        val saved = context.fromClient { ConfigSystem.serializeValueGroup(ModuleESP) }

        try {
            context.worldBuilder().create().use { world ->
                buildStage(world.server)
                world.connection.waitForChunksRender()
                context.waitTicks(SETTLE_TICKS)

                context.onClient {
                    setValue(ModuleESP, "Mode", to = "2D")
                    setValue(ModuleESP, "ColorMode", to = "Static")
                    setValue(ModuleESP, "ColorMode", "Static", "Color", to = "#FF00FF")
                    setBoxValue("Outline", "Thickness", to = "3")
                    ModuleESP.enabled = true
                }
                context.waitTicks(SETTLE_TICKS)

                val box = context.checkOutlines()
                context.checkHealthBar(box)
                context.checkGradient(box)
                context.checkGlow(box)
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
    private fun buildStage(server: TestServerContext) {
        val y = server.groundY()
        server.runCommand("gamerule spawn_mobs false")
        server.runCommand("difficulty easy")
        server.runCommand("gamemode creative @a")
        server.runCommand("tp @a 0.5 $y 0.5 -90 9")

        server.runCommand("fill -4 $y -16 10 ${y + 12} 16 air")
        server.runCommand("fill -4 ${y - 1} -16 10 ${y - 1} 16 black_concrete")
        server.runCommand("fill 10 $y -16 10 ${y + 12} 16 black_concrete")
        server.runCommand("kill @e[type=!player]")

        server.runCommand("summon husk 4.5 $y 0.5 {NoAI:1b,Silent:1b,PersistenceRequired:1b}")
    }

    /**
     * Checks every shape of the outline and returns the area of the whole box.
     */
    private fun ClientGameTestContext.checkOutlines(): Area {
        val whole = findPixels("esp-2d-box", ::isMagenta)
        val box = whole.area()
        check(box.right - box.left > MIN_BOX_WIDTH) { "The box of the husk only covers $box" }
        checkOutline("box", whole, box, TOP, SIDE, CORNER)

        onClient { setBoxValue("Corner", "Enabled", to = "true") }
        checkOutline("corners", findPixels("esp-2d-corners", ::isMagenta), box, CORNER)

        onClient { setBoxValue("Corner", "Brackets", to = "true") }
        checkOutline("brackets", findPixels("esp-2d-brackets", ::isMagenta), box, SIDE, CORNER)

        onClient {
            setBoxValue("Corner", "Enabled", to = "false")
            setBoxValue("CornerRadius", to = "16")
        }
        checkOutline("rounded box", findPixels("esp-2d-rounded", ::isMagenta), box, TOP, SIDE)

        onClient { setBoxValue("CornerRadius", to = "0") }
        return box
    }

    /**
     * Fails unless the [outline] covers exactly the [expected] of these parts of the [box]: the middle of its top
     * edge, the middle of its left side, and its top left corner.
     */
    private fun checkOutline(shape: String, outline: List<Vector2i>, box: Area, vararg expected: String) {
        val parts = mapOf(
            TOP to Area(box.centerX - 2, box.top, box.centerX + 2, box.top + 2),
            SIDE to Area(box.left, box.centerY - 2, box.left + 2, box.centerY + 2),
            CORNER to Area(box.left, box.top, box.left + 3, box.top + 3),
        )
        val drawn = parts.filterValues { part -> outline.any { it in part } }.keys

        check(drawn == expected.toSet()) { "The $shape draws $drawn of its outline instead of ${expected.toList()}" }
    }

    private fun ClientGameTestContext.checkHealthBar(box: Area) {
        val sides = mapOf(
            "Left" to Area(0, box.top, box.left - 1, box.bottom),
            "Right" to Area(box.right + 1, box.top, Int.MAX_VALUE, box.bottom),
            "Top" to Area(box.left, 0, box.right, box.top - 1),
            "Bottom" to Area(box.left, box.bottom + 1, box.right, Int.MAX_VALUE),
        )

        for ((position, side) in sides) {
            onClient { setBoxValue("HealthBar", "Position", to = position) }
            val bar = findPixels("esp-2d-health-${position.lowercase()}", ::isGreen)
            check(bar.all { it in side }) { "The health bar at $position covers ${bar.area()} next to the box $box" }
        }

        onClient { setBoxValue("HealthBar", "Enabled", to = "false") }
    }

    private fun ClientGameTestContext.checkGradient(box: Area) {
        onClient {
            setBoxValue("Gradient", "Enabled", to = "true")
            setBoxValue("Gradient", "SecondColor", to = "#00FF00")
            setBoxValue("Gradient", "Speed", to = "0")
        }
        val magenta = findPixels("esp-2d-gradient-top", ::isMagenta).area()
        val green = findPixels("esp-2d-gradient-bottom", ::isGreen).area()
        check(magenta.bottom < box.centerY && green.top > box.centerY) {
            "The gradient is magenta in $magenta and green in $green of the box $box"
        }

        onClient { setBoxValue("Gradient", "Enabled", to = "false") }
    }

    private fun ClientGameTestContext.checkGlow(box: Area) {
        val surroundings = Area(
            box.left - GLOW_SEARCH_PIXELS,
            box.top - GLOW_SEARCH_PIXELS,
            box.right + GLOW_SEARCH_PIXELS,
            box.bottom + GLOW_SEARCH_PIXELS,
        )

        val plain = findPixels("esp-2d-plain", ::isMagentaTint).filter { it in surroundings }.area()
        onClient {
            setBoxValue("Glow", "Enabled", to = "true")
            setBoxValue("Glow", "Opacity", to = "1")
        }
        val glowing = findPixels("esp-2d-glow", ::isMagentaTint).filter { it in surroundings }.area()

        check(
            glowing.left <= plain.left - MIN_GLOW_PIXELS && glowing.right >= plain.right + MIN_GLOW_PIXELS &&
                glowing.top <= plain.top - MIN_GLOW_PIXELS && glowing.bottom >= plain.bottom + MIN_GLOW_PIXELS
        ) { "The glow covers $glowing around the box $plain" }
    }

    private fun setBoxValue(vararg path: String, to: String) = setValue(ModuleESP, "Mode", "2D", *path, to = to)

    /**
     * Takes a screenshot after the changes settled and returns the positions of its pixels matching [predicate].
     */
    private fun ClientGameTestContext.findPixels(name: String, predicate: (Int) -> Boolean): List<Vector2i> {
        waitTicks(2)
        val pixels = ArrayList<Vector2i>()
        Files.newInputStream(takeScreenshot(name)).use { NativeImage.read(it) }.use { image ->
            for (x in 0 until image.width) {
                for (y in 0 until image.height) {
                    if (predicate(image.getPixel(x, y))) {
                        pixels += Vector2i(x, y)
                    }
                }
            }
        }

        check(pixels.isNotEmpty()) { "The screenshot $name has none of the expected pixels" }
        return pixels
    }

    private fun List<Vector2i>.area() = Area(minOf { it.x }, minOf { it.y }, maxOf { it.x }, maxOf { it.y })

    private fun isMagentaTint(pixel: Int) =
        ARGB.red(pixel) - ARGB.green(pixel) > 40 && ARGB.blue(pixel) - ARGB.green(pixel) > 40

    private data class Area(val left: Int, val top: Int, val right: Int, val bottom: Int) {
        val centerX get() = (left + right) / 2
        val centerY get() = (top + bottom) / 2

        operator fun contains(pixel: Vector2i) = pixel.x in left..right && pixel.y in top..bottom
    }

    private companion object {
        const val TOP = "top"
        const val SIDE = "side"
        const val CORNER = "corner"

        const val SETTLE_TICKS = 20
        const val MIN_BOX_WIDTH = 40
        const val GLOW_SEARCH_PIXELS = 20
        const val MIN_GLOW_PIXELS = 3
    }

}
