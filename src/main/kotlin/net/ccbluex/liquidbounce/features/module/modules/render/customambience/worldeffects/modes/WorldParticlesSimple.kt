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

import net.ccbluex.liquidbounce.config.types.group.ToggleableValueGroup
import net.ccbluex.liquidbounce.config.types.list.Tagged
import net.ccbluex.liquidbounce.features.module.modules.render.customambience.worldeffects.WorldParticlesColorSettings
import net.ccbluex.liquidbounce.features.module.modules.render.customambience.worldeffects.WorldParticlesMode
import net.ccbluex.liquidbounce.render.AnchorPoint
import net.ccbluex.liquidbounce.render.BuiltinParticle
import net.ccbluex.liquidbounce.render.WorldRenderEnvironment
import net.ccbluex.liquidbounce.render.drawSquareTexture
import net.ccbluex.liquidbounce.render.withPush

object WorldParticlesSimple : WorldParticlesMode("Simple") {

    private val color = WorldParticlesColorSettings()
    private val builtinParticles by enumChoice("Particle", BuiltinParticle.SPARK)

    object Ymotion : ToggleableValueGroup(this, "YMotion", false) {
        val motion by float("Motion", 2f, -10f..10f)
        val animBy by enumChoice("AnimBy", AnimBy.AGE)
    }

    init {
        tree(Ymotion)
        tree(color)
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

    @Suppress("unused")
    enum class AnimBy(
        override val tag: String,
    ) : Tagged {
        PROGRESS("Progress"),
        AGE("Age");
    }

}
