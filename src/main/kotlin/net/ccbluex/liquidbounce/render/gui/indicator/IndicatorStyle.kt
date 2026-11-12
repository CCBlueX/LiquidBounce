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

package net.ccbluex.liquidbounce.render.gui.indicator

import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.minecraft.client.gui.GuiGraphicsExtractor

/**
 * Shape of an indicator's arrow.
 *
 * The pose is already translated so that the arrow's tip, which sits at the end of its local +Y axis, lands on the
 * anchor, and rotated so that +Y points away from the screen center.
 */
interface IndicatorStyle {

    /**
     * Distance from the arrow's base to its tip, along local +Y. The arrow grows inwards from its anchor by this
     * much, and its tag is placed just past it.
     */
    val length: Float

    fun extract(ctx: GuiGraphicsExtractor, color: Color4b)

}
