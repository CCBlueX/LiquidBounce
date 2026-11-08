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
import net.ccbluex.liquidbounce.config.ConfigSystem
import net.ccbluex.liquidbounce.features.module.modules.render.customambience.ModuleCustomAmbience
import net.ccbluex.liquidbounce.gametest.fromClient
import net.ccbluex.liquidbounce.gametest.onClient
import net.ccbluex.liquidbounce.gametest.waitForClient
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext
import net.minecraft.client.gui.Hud
import net.minecraft.util.ARGB
import java.nio.file.Files

/**
 * The world particles of [ModuleCustomAmbience] stay where they spawned, and drift around on screen once Wander is on.
 */
class WorldParticlesWanderGameTest : FabricClientGameTest {

    override fun runTest(context: ClientGameTestContext) {
        context.waitForClient()
        val saved = context.fromClient { ConfigSystem.serializeValueGroup(ModuleCustomAmbience) }

        try {
            context.inVisualTestWorld {
                context.onClient { client -> client.gui.hud.setHidden(true) }
                context.setValue("customAmbience.weather", "Sunny")
                context.setValue("customAmbience.fog.enabled", "false")
                context.setValue("$SIMPLE.radius", "4..8")
                context.setValue("$SIMPLE.life", "1..512")
                context.setValue("$SIMPLE.spawnTime", "1")
                context.setValue("$SIMPLE.animCurve", "Linear")
                context.setValue("$SIMPLE.size", "2")
                context.setValue("$SIMPLE.particle", "Orbiz")
                context.setValue("$SIMPLE.color.color", "#FF00FF")
                context.setValue("customAmbience.worldParticles.enabled", "true")
                context.onClient { ModuleCustomAmbience.enabled = true }
                context.waitTicks(SPAWN_TICKS)

                val (stillShown, stillMoved) = context.particlePixels("world_particles_still")
                check(stillShown > MIN_SHOWN_PIXELS) { "particles not visible: $stillShown pixels" }
                check(stillMoved * 10 < stillShown) { "particles move without Wander: $stillMoved of $stillShown" }

                context.setValue("$SIMPLE.wander.distance", "2")
                context.setValue("$SIMPLE.wander.speed", "5")
                context.setValue("$SIMPLE.wander.enabled", "true")
                val (wanderShown, wanderMoved) = context.particlePixels("world_particles_wander")
                check(wanderMoved * 3 > wanderShown) { "particles do not wander: $wanderMoved of $wanderShown" }
            }
        } finally {
            context.onClient { client ->
                client.gui.hud.setHidden(false)
                ModuleCustomAmbience.enabled = false
                ConfigSystem.deserializeValueGroup(ModuleCustomAmbience, saved)
            }
        }
    }

    private fun Hud.setHidden(hidden: Boolean) {
        if (isHidden != hidden) {
            toggle()
        }
    }

    /**
     * The particle pixels on screen now and how many of them are gone a few ticks later.
     */
    private fun ClientGameTestContext.particlePixels(name: String): Pair<Int, Int> {
        val before = particleMask("${name}_before")
        waitTicks(MOVE_TICKS)
        val after = particleMask("${name}_after")
        return before.count { it } to before.indices.count { before[it] && !after[it] }
    }

    private fun ClientGameTestContext.particleMask(name: String): BooleanArray {
        waitTicks(1)
        return NativeImage.read(Files.newInputStream(takeScreenshot(name))).use { image ->
            BooleanArray(image.width * image.height) { index ->
                val pixel = image.getPixel(index % image.width, index / image.width)
                ARGB.red(pixel) > 150 && ARGB.green(pixel) < 100 && ARGB.blue(pixel) > 150
            }
        }
    }

    private companion object {
        const val SIMPLE = "customAmbience.worldParticles.mode.simple"
        const val SPAWN_TICKS = 60
        const val MOVE_TICKS = 20
        const val MIN_SHOWN_PIXELS = 200
    }

}
