/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2024 CCBlueX
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
 */

package net.ccbluex.liquidbounce.event.events

import net.ccbluex.liquidbounce.event.CancellableEvent
import net.ccbluex.liquidbounce.event.Event
import net.ccbluex.liquidbounce.utils.client.Nameable
import net.ccbluex.liquidbounce.integration.interop.protocol.event.WebSocketEvent

@Nameable("windowResize")
@WebSocketEvent
class WindowResizeEvent(val width: Int, val height: Int) : Event()

@Nameable("frameBufferResize")
@WebSocketEvent
class FrameBufferResizeEvent(val width: Int, val height: Int) : Event()

@Nameable("mouseButton")
@WebSocketEvent
class MouseButtonEvent(val button: Int, val action: Int, val mods: Int) : Event()

@Nameable("mouseScroll")
@WebSocketEvent
class MouseScrollEvent(val horizontal: Double, val vertical: Double) : Event()

@Nameable("mouseScrollInHotbar")
class MouseScrollInHotbarEvent(val speed: Int) : CancellableEvent()

@Nameable("mouseCursor")
@WebSocketEvent
class MouseCursorEvent(val x: Double, val y: Double) : Event()

@Nameable("keyboardKey")
@WebSocketEvent
class KeyboardKeyEvent(val keyCode: Int, val scanCode: Int, val action: Int, val mods: Int) : Event()

@Nameable("keyboardChar")
@WebSocketEvent
class KeyboardCharEvent(val codePoint: Int, val modifiers: Int) : Event()
