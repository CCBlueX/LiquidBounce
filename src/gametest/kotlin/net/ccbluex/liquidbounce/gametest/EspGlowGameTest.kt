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

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.config.ConfigSystem
import net.ccbluex.liquidbounce.features.module.modules.render.esp.ModuleESP
import net.ccbluex.liquidbounce.features.module.modules.render.esp.glow.GlowPostEffect
import net.ccbluex.liquidbounce.features.module.modules.render.esp.glow.ShaderGlow
import net.ccbluex.liquidbounce.features.module.modules.render.esp.modes.EspGlowMode
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext
import net.minecraft.client.gui.Hud
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.world.level.levelgen.Heightmap

/**
 * Every glow style of [ModuleESP] has to compile and draw, and the shader styles must stay out of the game's pipeline
 * and post effect lists, where resource packs and servers could find them. Leaves a screenshot of every style in front
 * of a few mobs, some behind a wall.
 */
class EspGlowGameTest : FabricClientGameTest {

    override fun runTest(context: ClientGameTestContext) {
        context.waitForClient()
        val saved = context.fromClient { ConfigSystem.serializeValueGroup(ModuleESP) }

        try {
            context.worldBuilder().create().use { world ->
                val y = world.server.computeOnServer<Int, RuntimeException> {
                    it.overworld().getHeight(Heightmap.Types.MOTION_BLOCKING, 0, 0)
                }
                buildStage(world.server, y)
                context.waitTicks(SETTLE_TICKS)

                context.onClient { client ->
                    client.gui.hud.setHidden(true)
                    ModuleESP.enabled = true
                    ModuleESP.modes.setByString(EspGlowMode.tag)
                }

                for (style in context.fromClient { EspGlowMode.styles.modes.toList() }) {
                    context.onClient { EspGlowMode.styles.setByString(style.tag) }
                    context.waitTicks(STYLE_TICKS)
                    context.takeScreenshot("esp-glow-${style.tag.lowercase()}")

                    if (style is ShaderGlow) {
                        check(context.fromClient { GlowPostEffect.compiles(style) }) { "${style.tag} did not compile" }
                    }
                }

                val registered = context.fromClient { client ->
                    val ours = EspGlowMode.styles.modes.filterIsInstance<ShaderGlow>().map { it.pipeline } +
                        GlowPostEffect.FieldRows + GlowPostEffect.FieldColumns
                    val pipelines = (RenderPipelines.requiredPipelines() + RenderPipelines.optionalPipelines())
                        .filter { it in ours }
                        .map { it.location }
                    val namespace = LiquidBounce.CLIENT_NAME.lowercase()
                    pipelines + client.shaderManager.availablePostEffects.toList().filter { it.namespace == namespace }
                }
                check(registered.isEmpty()) { "glow shaders in the game's lists: $registered" }
            }
        } finally {
            context.onClient { client ->
                client.gui.hud.setHidden(false)
                ModuleESP.enabled = false
                ConfigSystem.deserializeValueGroup(ModuleESP, saved)
            }
        }
    }

    private fun Hud.setHidden(hidden: Boolean) {
        if (isHidden != hidden) {
            toggle()
        }
    }

    /**
     * Mobs in front of the player at 0 [y] 0 looking south, the two on the right behind a stone wall.
     */
    private fun buildStage(server: TestServerContext, y: Int) {
        server.runCommand("gamerule advance_time false")
        server.runCommand("gamerule spawn_mobs false")
        server.runCommand("time set noon")
        server.runCommand("weather clear")
        server.runCommand("difficulty easy")
        server.runCommand("gamemode creative @a")

        server.runCommand("fill 1 $y 4 5 ${y + 1} 4 stone")
        for ((type, x, z) in MOBS) {
            server.runCommand(
                "summon $type $x $y $z {NoAI:1b,Silent:1b,PersistenceRequired:1b,Rotation:[180f,0f]}"
            )
        }

        server.runCommand("tp @a 0.5 $y 0.5 0 12")
    }

    private companion object {
        val MOBS = listOf(
            Triple("creeper", -3.5, 7.5),
            Triple("husk", -1.0, 6.5),
            Triple("witch", 1.5, 8.0),
            Triple("spider", 4.0, 7.5),
        )

        const val SETTLE_TICKS = 40
        const val STYLE_TICKS = 10
    }

}
