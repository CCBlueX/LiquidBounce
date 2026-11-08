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
import net.ccbluex.liquidbounce.config.types.list.MultiChoiceListValue
import net.ccbluex.liquidbounce.config.types.list.Tagged
import net.ccbluex.liquidbounce.features.module.modules.render.esp.ModuleESP
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext
import net.minecraft.client.gui.Hud
import net.minecraft.util.ARGB

/**
 * [ModuleESP] draws a boat, an end crystal and an arrow in the object colour, each only while its kind is selected
 * in Objects: as markers of the Legacy2D mode and as outlines of the glow mode.
 */
class EspObjectTargetsGameTest : FabricClientGameTest {

    override fun runTest(context: ClientGameTestContext) {
        context.waitForClient()
        val saved = context.fromClient { ConfigSystem.serializeValueGroup(ModuleESP) }

        try {
            context.worldBuilder().create().use { world ->
                buildStage(world.server)
                world.connection.waitForChunksRender()
                context.waitTicks(SETTLE_TICKS)

                context.onClient { client ->
                    client.gui.hud.setHidden(true)
                    setValue(ModuleESP, "Mode", to = "Legacy2D")
                    setValue(ModuleESP, "ColorMode", to = "Static")
                    setValue(ModuleESP, "ObjectColor", to = "#FF00FF")
                    selectObjects()
                    ModuleESP.enabled = true
                }
                val unmarked = context.countPerColumn("esp-objects-none", ::isMagenta)
                check(unmarked.all { it == 0 }) { "ESP marks objects without a selected kind: ${unmarked.toList()}" }

                for ((column, kind) in KINDS.withIndex()) {
                    context.onClient { selectObjects(kind) }
                    val marked = context.countPerColumn("esp-objects-${kind.lowercase()}", ::isMagenta)
                    check(marked[column] > 0 && marked.sum() == marked[column]) {
                        "$kind marks the columns ${marked.toList()} instead of only column $column"
                    }
                }

                context.onClient {
                    setValue(ModuleESP, "Mode", to = "Glow")
                    setValue(ModuleESP, "ObjectColor", to = "#00FF00")
                    selectObjects()
                }
                val plain = context.countPerColumn("esp-objects-plain", ::isGreenGlow)
                context.onClient { selectObjects(*KINDS) }
                context.waitTicks(SETTLE_TICKS)
                val glowing = context.countPerColumn("esp-objects-glow", ::isGreenGlow)
                for ((column, kind) in KINDS.withIndex()) {
                    check(glowing[column] >= plain[column] + MIN_GLOW_PIXELS) {
                        "$kind does not glow: ${glowing[column]} green pixels, ${plain[column]} without objects"
                    }
                }
            }
        } finally {
            context.onClient { client ->
                client.gui.hud.setHidden(false)
                ModuleESP.enabled = false
                ConfigSystem.deserializeValueGroup(ModuleESP, saved)
            }
        }
    }

    /**
     * A boat, an end crystal and an arrow from left to right in front of a black wall, seen from the player.
     */
    private fun buildStage(server: TestServerContext) {
        val y = server.groundY()
        server.runCommand("gamemode creative @a")
        server.runCommand("tp @a 0.5 $y 0.5 -90 0")

        server.runCommand("fill -4 $y -16 16 ${y + 12} 16 air")
        server.runCommand("fill -4 ${y - 1} -16 16 ${y - 1} 16 black_concrete")
        server.runCommand("fill 16 $y -16 16 ${y + 12} 16 black_concrete")
        server.runCommand("kill @e[type=!player]")

        server.runCommand("summon oak_boat 8.5 $y -5.5")
        server.runCommand("summon end_crystal 8.5 $y 0.5 {ShowBottom:0b}")
        server.runCommand("summon arrow 8.5 ${y + 1} 6.5 {NoGravity:1b,pickup:0b}")
    }

    @Suppress("UNCHECKED_CAST")
    private fun selectObjects(vararg kinds: String) {
        val objects = findValue(ModuleESP, "Objects") as MultiChoiceListValue<Tagged>
        for (kind in objects.choices) {
            if ((kind.tag in kinds) != (kind in objects)) {
                objects.toggle(kind)
            }
        }
    }

    /**
     * The pixels matching [predicate] in the left, middle and right part of the screen, one per staged object.
     */
    private fun ClientGameTestContext.countPerColumn(name: String, predicate: (Int) -> Boolean) =
        IntArray(COLUMN_ENDS.size) { column ->
            val start = COLUMN_ENDS.getOrElse(column - 1) { 0f }
            countPixels("$name-$column", predicate) { x, _ -> x >= start && x < COLUMN_ENDS[column] }[0]
        }

    private fun isGreenGlow(pixel: Int) = ARGB.green(pixel) > 150 && ARGB.red(pixel) < 60 && ARGB.blue(pixel) < 60

    private fun Hud.setHidden(hidden: Boolean) {
        if (isHidden != hidden) {
            toggle()
        }
    }

    private companion object {
        val KINDS = arrayOf("Vehicles", "EndCrystals", "Projectiles")
        val COLUMN_ENDS = floatArrayOf(0.4f, 0.6f, 1f)

        const val SETTLE_TICKS = 20
        const val MIN_GLOW_PIXELS = 20
    }

}
