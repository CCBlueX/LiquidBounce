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

package net.ccbluex.liquidbounce.features.module.modules.render.crosshair

import net.ccbluex.liquidbounce.config.types.group.ValueGroup
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.minecraft.util.Mth

class CrosshairColorSettings : ValueGroup("Color") {
    val syncColors by boolean("Sync", true)
    val firstColor by color("FirstColor", Color4b(0, 0, 255, 255))
    val secondColor by color("SecondColor", Color4b(0, 0, 255, 255))
    val spinSpeed by float("SpinSpeed", 4f, -10f..10f)

    fun getCurrentStepColor(
        firstColor: Color4b,
        secondColor: Color4b,
        syncColors: Boolean,
        spinSpeed: Float,
        angle: Float,
    ): Color4b {
        val first = firstColor
        val second = if (!syncColors) secondColor else firstColor

        return getColorByAngle(angle, first, second, spinSpeed)
    }

    private fun getColorByAngle(
        angle: Float,
        color1: Color4b,
        color2: Color4b,
        speed: Float,
    ): Color4b {
        val timeOffset =
            if (speed != 0f) {
                ((System.currentTimeMillis().toDouble() / 10000.0) * speed.toDouble() % 1.0) * Mth.TWO_PI
            } else {
                0.0
            }

        val progress = (Mth.sin(angle + timeOffset) * 0.5 + 0.5)

        return color1.interpolateTo(color2, progress)
    }
}
