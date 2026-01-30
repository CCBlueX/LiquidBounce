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

package net.ccbluex.liquidbounce.render.engine.font

import net.ccbluex.liquidbounce.config.types.list.Tagged

enum class HorizontalAnchor(override val tag: String) : Tagged {
    START("Start") {
        override fun anchorToDrawX(x: Float, width: Float, scale: Float): Float =
            x
    },
    CENTER("Center") {
        override fun anchorToDrawX(x: Float, width: Float, scale: Float): Float =
            x - width * scale * 0.5f
    },
    END("End") {
        override fun anchorToDrawX(x: Float, width: Float, scale: Float): Float =
            x - width * scale
    };

    /**
     * @param x Anchor X position
     * @param width Unscaled text width
     * @param scale Render scale
     * @return Draw (top-left) X position
     */
    abstract fun anchorToDrawX(x: Float, width: Float, scale: Float): Float
}
