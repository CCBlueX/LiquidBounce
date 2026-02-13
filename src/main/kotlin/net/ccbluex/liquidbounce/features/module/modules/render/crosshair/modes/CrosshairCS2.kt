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
import net.ccbluex.liquidbounce.render.drawQuad
import net.minecraft.util.Mth

object CrosshairCS2 : CrosshairMode("CS2") {

    private object CrosshairSettings : ValueGroup("Crosshair") {
        val length by float("Length", 5f, 1f..20f)
        val thickness by float("Thickness", 1f, 0.5f..5f)
        val gap by float("Gap", 2f, 0f..10f)
        val dynamicMultiplier by float("DynamicMultiplier", 1f, 0f..10f)
    }

    private val color = CrosshairColorSettings()

    init {
        tree(CrosshairSettings)
        tree(color)
    }

    override fun OverlayRenderEvent.drawCrosshair() {
        val multiplier = dynamicCrosshair(CrosshairSettings.dynamicMultiplier)
        val length = CrosshairSettings.length
        val thickness = CrosshairSettings.thickness
        val gap = CrosshairSettings.gap + multiplier

        val argb = color.getCurrentStepColor(
            color.firstColor,
            color.secondColor,
            color.syncColors,
            color.spinSpeed,
            0f
        )

        context.drawQuad(
            -thickness / 2f, -gap - length,
            thickness / 2f, -gap,
            fillColor = argb
        )

        context.drawQuad(
            -thickness / 2f, gap,
            thickness / 2f, gap + length,
            fillColor = argb
        )

        context.drawQuad(
            -gap - length, -thickness / 2f,
            -gap, thickness / 2f,
            fillColor = argb
        )

        context.drawQuad(
            gap, -thickness / 2f,
            gap + length, thickness / 2f,
            fillColor = argb
        )
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
