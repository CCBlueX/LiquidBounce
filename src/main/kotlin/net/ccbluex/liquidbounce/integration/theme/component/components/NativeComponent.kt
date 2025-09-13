/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2025 CCBlueX
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

package net.ccbluex.liquidbounce.integration.theme.component.components

import net.ccbluex.liquidbounce.integration.theme.component.Component
import net.ccbluex.liquidbounce.integration.theme.component.ComponentTweak
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.render.Alignment

abstract class NativeComponent(
    name: String,
    enabled: Boolean,
    alignment: Alignment,
    tweaks: Array<ComponentTweak> = emptyArray()
) : Component(name, enabled, alignment, tweaks)

fun applyAdaptiveScale(
    size: Float,
    baseW: Float,
    baseH: Float,
    alignment: Alignment,
    block: (scale: Float, cx: Float, cy: Float) -> Unit
) {
    val window = mc.window
    val s = size.coerceAtLeast(0.1f)
    val scale = (window.scaledWidth.coerceAtMost(window.scaledHeight)) / 500f * s

    val bounds = alignment.getBounds(baseW * scale, baseH * scale)
    val cx = bounds.xMin + (baseW * scale) / 2f
    val cy = bounds.yMin + (baseH * scale) / 2f

    block(scale, cx, cy)
}
