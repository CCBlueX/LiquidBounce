/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2016 - 2022 CCBlueX
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

package net.ccbluex.liquidbounce.render.engine.utils

import net.ccbluex.liquidbounce.utils.math.Mat4
import org.lwjgl.opengl.GL11

/**
 * Pushes an MVP matrix for immediate mode
 */
fun pushMVP(mvpMatrix: Mat4) {
    // Load the MVP matrix
    GL11.glMatrixMode(GL11.GL_PROJECTION)
    GL11.glPushMatrix()
    GL11.glLoadMatrixf(mvpMatrix.toArray())

    // Reset model view matrix
    GL11.glMatrixMode(GL11.GL_MODELVIEW)
    GL11.glPushMatrix()
    GL11.glLoadIdentity()
}

fun popMVP() {
    // Load the MVP matrix
    GL11.glMatrixMode(GL11.GL_PROJECTION)
    GL11.glPopMatrix()

    // Reset model view matrix
    GL11.glMatrixMode(GL11.GL_MODELVIEW)
    GL11.glPopMatrix()
}
