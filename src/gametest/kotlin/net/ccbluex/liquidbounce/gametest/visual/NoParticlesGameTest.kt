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

import net.ccbluex.liquidbounce.features.module.modules.render.ModuleAntiBlind
import net.ccbluex.liquidbounce.gametest.fromClient
import net.ccbluex.liquidbounce.gametest.onClient
import net.ccbluex.liquidbounce.gametest.waitForClient
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext
import net.minecraft.client.Minecraft
import net.minecraft.core.particles.ParticleTypes

/**
 * The hidden particles of [ModuleAntiBlind].
 */
class NoParticlesGameTest : FabricClientGameTest {

    override fun runTest(context: ClientGameTestContext) {
        context.waitForClient()
        context.inVisualTestWorld { world ->
            context.onClient { ModuleAntiBlind.enabled = true }

            context.spawnParticles(world)
            val shown = context.fromClient { client -> particleCount(client) }
            check(shown > 0) { "the particles show nothing to hide" }

            context.onClient { client -> client.particleEngine.clearParticles() }
            context.setValue("antiBlind.noParticles", "true")
            context.spawnParticles(world)
            val hidden = context.fromClient { client -> particleCount(client) }
            check(hidden == 0) { "$hidden particles despite NoParticles" }

            context.setValue("antiBlind.noParticles", "false")
            context.onClient { ModuleAntiBlind.enabled = false }
        }
    }

    /**
     * Particles sent by the server and ones emitted around the player, like a critical hit's.
     */
    private fun ClientGameTestContext.spawnParticles(world: TestSingleplayerContext) {
        world.server.runCommand(PARTICLE_COMMAND)
        onClient { client -> client.particleEngine.createTrackingEmitter(client.player(), ParticleTypes.CRIT) }
        waitTicks(3)
    }

    /**
     * The total at the end of the debug text, like `T 42`.
     */
    private fun particleCount(client: Minecraft) =
        client.particleEngine.countParticles().substringAfterLast("T ").trim().toInt()

    private companion object {
        const val PARTICLE_COMMAND = "execute at @p run particle minecraft:flame ~ ~1 ~3 0.5 0.5 0.5 0 200 force"
    }

}
