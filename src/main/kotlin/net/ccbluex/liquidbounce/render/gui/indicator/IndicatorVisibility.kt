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

import net.ccbluex.liquidbounce.config.types.list.Tagged

/**
 * Where an indicator is drawn.
 *
 * [OFFSCREEN] covers targets outside the viewport as well as targets behind the camera, which have no projection to
 * test at all.
 */
enum class IndicatorVisibility(override val tag: String) : Tagged {
    IN_VIEW("InView"),
    OFFSCREEN("Offscreen"),
}
