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

package net.ccbluex.liquidbounce.web.hooks

import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.web.WebController

class UlWindowHooks(webController: WebController) : Listenable {

    val gameRenderHandlerHandler = handler<GameRenderEvent> {
        webController.update()
        webController.render()
    }

    val windowResizeHandler = handler<WindowResizeEvent> {
        webController.resize(it.width, it.height)
    }

    val windowMoveHandler = handler<WindowMoveEvent> {
        webController.move(it.x, it.y)
    }

    val windowFocusHandler = handler<WindowFocusEvent> {

    }

    val mouseButtonHandler = handler<MouseButtonEvent> {

    }

    val mouseScrollHandler = handler<MouseScrollEvent> {

    }

    val mouseCursorHandler = handler<MouseCursorEvent> {

    }

    val keyboardKeyHandler = handler<KeyboardKeyEvent> {

    }

    val keyboardCharHandler = handler<KeyboardCharEvent> {

    }

}
