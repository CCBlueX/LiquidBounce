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

import net.ccbluex.liquidbounce.event.events.WorldRenderEvent
import net.ccbluex.liquidbounce.features.module.modules.render.totemeffect.TotemEffectColorSettings
import net.ccbluex.liquidbounce.features.module.modules.render.totemeffect.TotemEffectMode
import net.ccbluex.liquidbounce.render.WorldRenderEnvironment
import net.ccbluex.liquidbounce.render.drawGradientCircle
import net.minecraft.world.phys.Vec3

internal object TotemEffectShockwave : TotemEffectMode("Shockwave") {

    private val color = TotemEffectColorSettings()

    init {
        tree(color)
    }

    override fun WorldRenderEnvironment.drawTotemEffect(progress: Float, pos: Vec3, event: WorldRenderEvent) {
        val inner = color.innerColor
        val outer = if (color.sync) inner else color.outerColor

        val fadeProgress = if (progress >= fade) (progress - fade) / (1f - fade) else 0f
        val alpha = ((1f - fadeProgress).coerceIn(0f, 1f) * 255f).toInt()

        drawGradientCircle(
            radius.endInclusive * progress,
            radius.start * progress,
            inner.alpha(alpha),
            outer.alpha(alpha),
            noDepthTest = !canBeCovered
        )
    }

}
