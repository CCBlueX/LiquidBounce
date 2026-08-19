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

package net.ccbluex.liquidbounce.features.module.modules.render.customambience.worldeffects.modes

import net.ccbluex.liquidbounce.features.module.modules.render.customambience.worldeffects.WorldParticles.coords
import net.ccbluex.liquidbounce.features.module.modules.render.customambience.worldeffects.WorldParticlesColorSettings
import net.ccbluex.liquidbounce.features.module.modules.render.customambience.worldeffects.WorldParticlesMode
import net.ccbluex.liquidbounce.render.AnchorPoint
import net.ccbluex.liquidbounce.render.BuiltinParticle
import net.ccbluex.liquidbounce.render.WorldRenderEnvironment
import net.ccbluex.liquidbounce.render.drawSquareTexture
import net.ccbluex.liquidbounce.render.withPush
import net.ccbluex.liquidbounce.utils.math.times
import net.minecraft.util.Mth
import net.minecraft.world.phys.Vec3
import kotlin.math.cbrt
import kotlin.math.pow
import kotlin.math.sqrt
import kotlin.random.Random

object WorldParticlesSimple : WorldParticlesMode("Simple") {

    private val color = WorldParticlesColorSettings()
    private val builtinParticles by enumChoice("Particle", BuiltinParticle.SPARK)

    init {
        tree(color)
    }

    override fun createParticleCoord(currentTime: Long) {
        if (coords.size >= count) return

        lastSpawnTime = currentTime

        val u = Random.nextDouble(-1.0, 1.0)
        val theta = Random.nextDouble(0.0, 2 * Math.PI)
        val s = sqrt(1.0 - u * u)

        val coord = Vec3(s * Mth.cos(theta), u , s * Mth.sin(theta))
        val rMinCb = radius.first.toDouble().pow(3)
        val rMaxCb = radius.last.toDouble().pow(3)
        val distance = cbrt(Random.nextDouble(rMinCb, rMaxCb))

        coords.add(player.position().add(coord * distance), lifetime.last)
    }

    override fun WorldRenderEnvironment.drawWorldParticle(progress: Float, age: Float) {
        poseStack.withPush {
            drawSquareTexture(
                builtinParticles.texture,
                size * progress,
                color.color.argb,
                AnchorPoint.CENTER,
                !canBeCovered
            )
        }
    }

}
