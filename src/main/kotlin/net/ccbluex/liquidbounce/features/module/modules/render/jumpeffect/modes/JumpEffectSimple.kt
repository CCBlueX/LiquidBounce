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

package net.ccbluex.liquidbounce.features.module.modules.render.jumpeffect.modes

import net.ccbluex.liquidbounce.features.module.modules.render.jumpeffect.JumpEffectColorSettings
import net.ccbluex.liquidbounce.features.module.modules.render.jumpeffect.JumpEffectMode
import net.ccbluex.liquidbounce.render.WorldRenderEnvironment
import net.ccbluex.liquidbounce.render.drawGradientCircle

internal object JumpEffectSimple : JumpEffectMode("Simple") {

    private val colors = JumpEffectColorSettings()

    init {
        tree(colors)
    }

    override fun WorldRenderEnvironment.drawJumpEffect(progress: Float, age: Float) {
        drawGradientCircle(
            endRadius.endInclusive * progress,
            endRadius.start * progress,
            animateColor(colors.outerColor, age),
            animateColor(colors.innerColor, age),
            noDepthTest = !canBeCovered
        )
    }
}
