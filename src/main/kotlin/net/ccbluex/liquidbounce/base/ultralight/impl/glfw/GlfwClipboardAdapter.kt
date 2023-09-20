/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2023 CCBlueX
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
package net.ccbluex.liquidbounce.base.ultralight.impl.glfw

import com.labymedia.ultralight.plugin.clipboard.UltralightClipboard
import org.lwjgl.glfw.GLFW

/**
 * Clipboard using GLFW
 */
class GlfwClipboardAdapter : UltralightClipboard {

    /**
     * Ultralight calls this when the clipboard is requested as a string.
     *
     * @return The clipboard content as a string
     */
    override fun readPlainText() = GLFW.glfwGetClipboardString(0)!!

    /**
     * Ultralight calls this when the clipboard content should be overwritten.
     *
     * @param text The plain text to write to the clipboard
     */
    override fun writePlainText(text: String) {
        GLFW.glfwSetClipboardString(0, text)
    }

    /**
     * Ultralight calls this when the clipboard should be cleared.
     */
    override fun clear() {
        GLFW.glfwSetClipboardString(0, "")
    }

}
