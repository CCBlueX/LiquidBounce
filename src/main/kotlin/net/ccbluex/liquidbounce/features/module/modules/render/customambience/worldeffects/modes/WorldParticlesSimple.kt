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

import net.ccbluex.fastutil.enumSetOf
import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.config.types.list.Tagged
import net.ccbluex.liquidbounce.features.module.modules.render.customambience.worldeffects.WorldParticlesColorSettings
import net.ccbluex.liquidbounce.features.module.modules.render.customambience.worldeffects.WorldParticlesMode
import net.ccbluex.liquidbounce.render.AnchorPoint
import net.ccbluex.liquidbounce.render.WorldRenderEnvironment
import net.ccbluex.liquidbounce.render.drawSquareTexture
import net.ccbluex.liquidbounce.utils.render.asTexture
import net.ccbluex.liquidbounce.utils.render.readNativeImage

object WorldParticlesSimple : WorldParticlesMode("Simple") {

    private val color = WorldParticlesColorSettings()
    private val builtinEffects by enumChoice("Particle", BuiltinEffect.SPARK)

    init {
        tree(color)
    }

    override fun WorldRenderEnvironment.drawWorldParticle(progress: Float, age: Float) {
        drawSquareTexture(
            builtinEffects.texture,
            size * progress,
            color.color.argb,
            AnchorPoint.CENTER,
            !canBeCovered
        )
    }

    // Pasted from ModuleParticles
    @Suppress("UNUSED")
    private enum class BuiltinEffect(
        override val tag: String,
        fileName: String,
    ) : Tagged {
        ORBIZ("Orbiz", "glow"),
        STAR("Star", "star"),
        DOLLAR("Dollar", "dollar"),
        CROWN("Crown", "crown"),
        HEART("Heart", "heart"),
        LIGHTNING("Lightning", "lightning"),
        LINE("Line", "line"),
        POINT("Point", "point"),
        RHOMBUS("Rhombus", "rhombus"),
        SNOWFLAKE("Snowflake", "snowflake"),
        SPARK("Spark", "spark");

        val image = LiquidBounce.resource("particles/$fileName.png").readNativeImage()
        val texture = this.image.asTexture { "Builtin Effects $tag" }
    }

}
