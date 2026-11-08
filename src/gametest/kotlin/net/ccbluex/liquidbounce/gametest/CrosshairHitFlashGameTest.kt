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
import net.minecraft.world.entity.monster.zombie.Husk

/**
 * Hitting the husk under the crosshair makes the crosshair of [ModuleCrosshair] flash, and the flash fades again.
 */
class CrosshairHitFlashGameTest : FabricClientGameTest {

    override fun runTest(context: ClientGameTestContext) {
        context.waitForClient()
        val saved = context.fromClient { ConfigSystem.serializeValueGroup(ModuleCrosshair) }

        try {
            context.worldBuilder().create().use { world ->
                val server = world.server
                val y = server.groundY()
                server.runCommand("gamerule spawn_mobs false")
                server.runCommand("kill @e[type=!player]")
                server.runCommand("fill -12 $y -12 24 ${y + 10} 12 air")
                server.runCommand("fill -12 ${y - 1} -12 24 ${y - 1} 12 smooth_stone")
                server.runCommand("gamemode creative @a")
                server.runCommand("tp @a 0.5 $y 0.5 -90 0")
                server.runCommand("summon husk 2.5 $y 0.5 {NoAI:1b,PersistenceRequired:1b,Silent:1b}")
                world.connection.waitForChunksRender()

                context.onClient {
                    ModuleCrosshair.HitFlash.enabled = true
                    ModuleCrosshair.enabled = true
                }
                context.waitFor({ it.crosshairPickEntity is Husk }, TIMEOUT_TICKS)
                check(context.fromClient { ModuleCrosshair.HitFlash.progress } == 0f) {
                    "The hit flash runs before any hit"
                }

                context.input.pressKey { it.keyAttack }
                context.waitFor({ ModuleCrosshair.HitFlash.progress > 0f }, TIMEOUT_TICKS)
                context.takeScreenshot("crosshair-hit-flash")
                context.waitFor({ ModuleCrosshair.HitFlash.progress == 0f }, TIMEOUT_TICKS)
            }
        } finally {
            context.onClient {
                ModuleCrosshair.enabled = false
                ConfigSystem.deserializeValueGroup(ModuleCrosshair, saved)
            }
        }
    }

    private companion object {
        const val TIMEOUT_TICKS = 60
    }

}
