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

import net.ccbluex.gametest.harness.GameTestScope
import net.ccbluex.gametest.harness.SingleplayerGameTest
import net.ccbluex.gametest.harness.action.screenshot
import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.features.module.modules.render.esp.ModuleESP
import net.ccbluex.liquidbounce.features.module.modules.render.esp.modes.EspGlowMode
import net.ccbluex.liquidbounce.render.effect.PostStyle
import net.ccbluex.liquidbounce.render.effect.StyleChain
import net.minecraft.client.gui.Hud
import net.minecraft.client.renderer.RenderPipelines

private val MOBS = listOf(
    Triple("creeper", -3.5, 7.5),
    Triple("husk", -1.0, 6.5),
    Triple("witch", 1.5, 8.0),
    Triple("spider", 4.0, 7.5),
)

private const val SETTLE_TICKS = 40
private const val STYLE_TICKS = 10

/**
 * Every glow style of [ModuleESP] has to compile and draw, and the shader styles must stay out of the game's pipeline
 * and post effect lists, where resource packs and servers could find them. Leaves a screenshot of every style in front
 * of a few mobs, some behind a wall.
 */
class EspGlowGameTest : SingleplayerGameTest({
    context.restoring(ModuleESP) {
        try {
            buildStage(origin.y)
            ticks(SETTLE_TICKS)

            client { client ->
                client.gui.hud.setHidden(true)
                ModuleESP.enabled = true
                ModuleESP.modes.setByString(EspGlowMode.tag)
            }

            for (style in client { EspGlowMode.styles.modes.toList() }) {
                client { EspGlowMode.styles.setByString(style.tag) }
                ticks(STYLE_TICKS)
                screenshot("esp-glow-${style.tag.lowercase()}")

                if (style is PostStyle) {
                    check(client { StyleChain.compiles(style) }) { "${style.tag} did not compile" }
                }
            }

            val registered = client { client ->
                val ours = EspGlowMode.styles.modes.filterIsInstance<PostStyle>().map { it.pipeline } +
                    StyleChain.FieldRows + StyleChain.FieldColumns
                val pipelines = (RenderPipelines.requiredPipelines() + RenderPipelines.optionalPipelines())
                    .filter { it in ours }
                    .map { it.location }
                val namespace = LiquidBounce.CLIENT_NAME.lowercase()
                pipelines + client.shaderManager.availablePostEffects.toList().filter { it.namespace == namespace }
            }
            check(registered.isEmpty()) { "glow shaders in the game's lists: $registered" }
        } finally {
            client { it.gui.hud.setHidden(false) }
        }
    }
})

private fun Hud.setHidden(hidden: Boolean) {
    if (isHidden != hidden) {
        toggle()
    }
}

/**
 * Mobs in front of the player at 0 [y] 0 looking south, the two on the right behind a stone wall.
 */
private fun GameTestScope.buildStage(y: Int) {
    run("time set noon")
    run("weather clear")
    run("difficulty easy")
    run("gamemode creative $playerName")

    run("fill 1 $y 4 5 ${y + 1} 4 stone")
    for ((type, x, z) in MOBS) {
        run("summon $type $x $y $z {NoAI:1b,Silent:1b,PersistenceRequired:1b,Rotation:[180f,0f]}")
    }

    run("tp $playerName 0.5 $y 0.5 0 12")
}
