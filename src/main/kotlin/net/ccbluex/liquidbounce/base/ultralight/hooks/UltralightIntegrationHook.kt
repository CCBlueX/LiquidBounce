/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2016 - 2023 CCBlueX
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
package net.ccbluex.liquidbounce.base.ultralight.hooks

import net.ccbluex.liquidbounce.base.ultralight.RenderLayer
import net.ccbluex.liquidbounce.base.ultralight.UltralightEngine
import net.ccbluex.liquidbounce.event.*
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL12
import org.lwjgl.opengl.GL33.glGetInteger

/**
 * A integration bridge between Minecraft and Ultralight
 */
object UltralightIntegrationHook : Listenable {

    val screenRenderHandler = handler<ScreenRenderEvent> {
        UltralightEngine.update()

        // Render the web content
        UltralightEngine.window.updateWebContent()

        UltralightEngine.window.bindTexture()
        UltralightEngine.window.postAndWait(Runnable {
            val tex = glGetInteger(GL11.GL_TEXTURE_BINDING_2D)
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0)

            val texWidth = UltralightEngine.window.width
            val texHeight = UltralightEngine.window.height
            val winWidth = UltralightEngine.window.width
            val winHeight = UltralightEngine.window.height

            GL12.glDisable(GL12.GL_SCISSOR_TEST)
            GL12.glEnable(GL12.GL_BLEND)
            GL12.glBlendFunc(GL12.GL_SRC_ALPHA, GL12.GL_ONE_MINUS_SRC_ALPHA)


        })


        UltralightEngine.window.swapBuffers()
    }

    val overlayRenderHandler = handler<OverlayRenderEvent> {
        UltralightEngine.render(RenderLayer.OVERLAY_LAYER, it.matrices)
    }

    val windowResizeWHandler = handler<WindowResizeEvent> {
        UltralightEngine.resize(it.width.toLong(), it.height.toLong())
    }

    val windowFocusHandler = handler<WindowFocusEvent> {
        UltralightEngine.inputAdapter.focusCallback(it.window, it.focused)
    }

    val mouseButtonHandler = handler<MouseButtonEvent> {
        UltralightEngine.inputAdapter.mouseButtonCallback(it.window, it.button, it.action, it.mods)
    }

    val mouseScrollHandler = handler<MouseScrollEvent> {
        UltralightEngine.inputAdapter.scrollCallback(it.window, it.horizontal, it.vertical)
    }

    val mouseCursorHandler = handler<MouseCursorEvent> {
        UltralightEngine.inputAdapter.cursorPosCallback(it.window, it.x, it.y)
    }

    val keyboardKeyHandler = handler<KeyboardKeyEvent> {
        UltralightEngine.inputAdapter.keyCallback(it.window, it.keyCode, it.scancode, it.action, it.mods)
    }

    val keyboardCharHandler = handler<KeyboardCharEvent> {
        UltralightEngine.inputAdapter.charCallback(it.window, it.codepoint)
    }

}
