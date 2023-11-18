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
package net.ccbluex.liquidbounce.base.ultralight.hooks

import net.ccbluex.liquidbounce.base.ultralight.RenderLayer
import net.ccbluex.liquidbounce.base.ultralight.UltralightEngine
import net.ccbluex.liquidbounce.event.Listenable
import net.ccbluex.liquidbounce.event.events.*
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.utils.client.mc

/**
 * A integration bridge between Minecraft and Ultralight
 */
object UltralightIntegrationHook : Listenable {

    val gameRenderHandlerHandler = handler<GameRenderEvent> {
        UltralightEngine.update()
    }

    val screenRenderHandler = handler<ScreenRenderEvent> {
        UltralightEngine.render(RenderLayer.SCREEN_LAYER, it.context)
        UltralightEngine.render(RenderLayer.SPLASH_LAYER, it.context)
    }

    val overlayRenderHandler = handler<OverlayRenderEvent> {
        UltralightEngine.render(RenderLayer.OVERLAY_LAYER, it.context)
    }

    val windowResizeWHandler = handler<WindowResizeEvent> {
        UltralightEngine.resize(it.width.toLong(), it.height.toLong())
    }

    val windowFocusHandler = handler<WindowFocusEvent> {
        UltralightEngine.inputAdapter.focusCallback(mc.window.handle, it.focused)
    }

    val mouseButtonHandler = handler<MouseButtonEvent> {
        UltralightEngine.inputAdapter.mouseButtonCallback(mc.window.handle, it.button, it.action, it.mods)
    }

    val mouseScrollHandler = handler<MouseScrollEvent> {
        UltralightEngine.inputAdapter.scrollCallback(mc.window.handle, it.horizontal, it.vertical)
    }

    val mouseCursorHandler = handler<MouseCursorEvent> {
        UltralightEngine.inputAdapter.cursorPosCallback(mc.window.handle, it.x, it.y)
    }

    val keyboardKeyHandler = handler<KeyboardKeyEvent> {
        UltralightEngine.inputAdapter.keyCallback(mc.window.handle, it.keyCode, it.scanCode, it.action, it.mods)
    }

    val keyboardCharHandler = handler<KeyboardCharEvent> {
        UltralightEngine.inputAdapter.charCallback(mc.window.handle, it.codePoint)
    }

}
