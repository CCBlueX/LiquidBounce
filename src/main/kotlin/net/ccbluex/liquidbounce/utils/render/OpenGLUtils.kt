/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2016 - 2021 CCBlueX
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

package net.ccbluex.liquidbounce.utils.render

import org.lwjgl.opengl.GL11.glGetFloatv

/**
 * Gets a range (i.e. GL_ALIASED_LINE_WIDTH_RANGE) from OpenGL as a kotlin range
 */
fun getGlFloatRange(key: Int): ClosedFloatingPointRange<Float> {
    val floats = floatArrayOf(0.0f, 0.0f)

    glGetFloatv(key, floats)

    return floats[0]..floats[1]
}
