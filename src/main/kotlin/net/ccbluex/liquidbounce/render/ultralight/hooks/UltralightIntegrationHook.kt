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
package net.ccbluex.liquidbounce.render.ultralight.hooks

import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.render.ultralight.RenderLayer
import net.ccbluex.liquidbounce.render.ultralight.UltralightEngine

/**
 * A integration bridge between Minecraft and Ultralight
 */
object UltralightIntegrationHook : Listenable {

    val gameRenderHandlerHandler = handler<GameRenderEvent> {
        UltralightEngine.update()
    }

    val screenRenderHandler = handler<ScreenRenderEvent> {
        UltralightEngine.render(RenderLayer.SCREEN_LAYER, it.matrices)
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
