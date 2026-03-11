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

package net.ccbluex.liquidbounce.features.module.modules.render.crosshair.modes

import net.ccbluex.liquidbounce.config.types.group.ValueGroup
import net.ccbluex.liquidbounce.event.events.OverlayRenderEvent
import net.ccbluex.liquidbounce.features.module.modules.render.crosshair.CrosshairColorSettings
import net.ccbluex.liquidbounce.features.module.modules.render.crosshair.CrosshairMode
import net.ccbluex.liquidbounce.render.drawCircle
import net.minecraft.util.Mth

object CrosshairCircle : CrosshairMode("Circle") {
    private object Radius : ValueGroup("Radius") {
        val radius by floatRange("Range", 3f..5f, 0f..25f)
        val dynamicRadiusMultiplier by float("DynamicRadiusMultiplier", 1f, 0f..5f)
    }
    private val color = CrosshairColorSettings()

    init {
        tree(Radius)
        tree(color)
    }

    override fun OverlayRenderEvent.drawCrosshair() {
        val multiplier = dynamicCrosshair(Radius.dynamicRadiusMultiplier)
        val innerRadius = Radius.radius.start + multiplier
        val outerRadius = Radius.radius.endInclusive + multiplier

        context.drawCircle(
            x = 0f, y = 0f,
            outerRadius,
            innerRadius,
        ) { angle ->
            color.getCurrentStepColor(
                color.firstColor,
                color.secondColor,
                color.syncColors,
                color.spinSpeed,
                angle,
            ).argb
        }
    }

    private fun OverlayRenderEvent.dynamicCrosshair(multiplier: Float): Float {
        return if (Mth.equal(0f, multiplier)) {
            0f
        } else {
            val cooldown = player.getAttackStrengthScale(tickDelta)
            multiplier * (1f - cooldown)
        }
    }
}
