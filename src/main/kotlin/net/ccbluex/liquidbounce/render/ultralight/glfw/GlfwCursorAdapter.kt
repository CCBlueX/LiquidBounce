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
 *
 * and
 *
 * Ultralight Java - Java wrapper for the Ultralight web engine
 * Copyright (C) 2020 - 2021 LabyMedia and contributors
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package net.ccbluex.liquidbounce.render.ultralight.glfw

import com.labymedia.ultralight.input.UltralightCursor
import net.ccbluex.liquidbounce.render.ultralight.UltralightEngine
import org.lwjgl.glfw.GLFW

/**
 * Utility class for controlling GLFW cursors.
 */
class GlfwCursorAdapter {

    private val beamCursor = GLFW.glfwCreateStandardCursor(GLFW.GLFW_IBEAM_CURSOR)
    private val crosshairCursor = GLFW.glfwCreateStandardCursor(GLFW.GLFW_CROSSHAIR_CURSOR)
    private val handCursor = GLFW.glfwCreateStandardCursor(GLFW.GLFW_HAND_CURSOR)
    private val hresizeCursor = GLFW.glfwCreateStandardCursor(GLFW.GLFW_HRESIZE_CURSOR)
    private val vresizeCursor = GLFW.glfwCreateStandardCursor(GLFW.GLFW_VRESIZE_CURSOR)

    /**
     * Signals this [GlfwCursorAdapter] that the cursor has been updated and needs to be updated on the GLFW side
     * too.
     *
     * @param cursor The new [UltralightCursor] to display
     */
    fun notifyCursorUpdated(cursor: UltralightCursor?) {
        when (cursor) {
            UltralightCursor.CROSS -> GLFW.glfwSetCursor(UltralightEngine.window, crosshairCursor)
            UltralightCursor.HAND -> GLFW.glfwSetCursor(UltralightEngine.window, handCursor)
            UltralightCursor.I_BEAM -> GLFW.glfwSetCursor(UltralightEngine.window, beamCursor)
            UltralightCursor.EAST_WEST_RESIZE -> GLFW.glfwSetCursor(UltralightEngine.window, hresizeCursor)
            UltralightCursor.NORTH_SOUTH_RESIZE -> GLFW.glfwSetCursor(UltralightEngine.window, vresizeCursor)
            else -> GLFW.glfwSetCursor(UltralightEngine.window, 0)
        }
    }

    /**
     * Frees GLFW resources allocated by this [GlfwCursorAdapter].
     */
    fun cleanup() {
        GLFW.glfwDestroyCursor(vresizeCursor)
        GLFW.glfwDestroyCursor(hresizeCursor)
        GLFW.glfwDestroyCursor(handCursor)
        GLFW.glfwDestroyCursor(crosshairCursor)
        GLFW.glfwDestroyCursor(beamCursor)
    }

    fun unfocus() {
        GLFW.glfwSetCursor(UltralightEngine.window, 0)
    }

}
