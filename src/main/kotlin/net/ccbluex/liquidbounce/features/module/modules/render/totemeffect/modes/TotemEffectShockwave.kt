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

package net.ccbluex.liquidbounce.features.module.modules.render.totemeffect.modes

import net.ccbluex.liquidbounce.features.module.modules.render.totemeffect.ModuleTotemEffect.TotemPopSnapshot
import net.ccbluex.liquidbounce.features.module.modules.render.totemeffect.TotemEffectColorSettings
import net.ccbluex.liquidbounce.features.module.modules.render.totemeffect.TotemEffectMode
import net.ccbluex.liquidbounce.render.WorldRenderEnvironment
import net.ccbluex.liquidbounce.render.drawGradientCircle
import net.ccbluex.liquidbounce.render.withPositionRelativeToCamera

internal object TotemEffectShockwave : TotemEffectMode("Shockwave") {

    private val radius by floatRange("radius", 2f..2.2f, 0.1f..10f)
    private val color = TotemEffectColorSettings()

    init {
        tree(color)
    }

    override fun WorldRenderEnvironment
        .drawTotemEffect(progress: Float, age: Float, entity: TotemPopSnapshot, fade: Float) {
        val inner = color.innerColor
        val outer = if (color.sync) inner else color.outerColor
        val alpha = ((1f - fade).coerceIn(0f, 1f) * 255f).toInt()

        withPositionRelativeToCamera(entity.pos.add(0.0, entity.bbHeight / 2.0, 0.0)) {
            drawGradientCircle(
                outerRadius = radius.endInclusive * progress,
                innerRadius = radius.start * progress,
                outerColor = outer.alpha(alpha),
                innerColor = inner.alpha(alpha),
                noDepthTest = !canBeCovered
            )
        }
    }

}
