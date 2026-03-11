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

package net.ccbluex.liquidbounce.event.events

import com.mojang.blaze3d.platform.InputConstants
import net.ccbluex.liquidbounce.annotations.Tag
import net.ccbluex.liquidbounce.event.CancellableEvent
import net.ccbluex.liquidbounce.event.Event
import net.ccbluex.liquidbounce.integration.interop.protocol.event.WebSocketEvent
import net.minecraft.client.gui.screens.Screen

@Tag("windowResize")
class WindowResizeEvent(val width: Int, val height: Int) : Event()

@Tag("frameBufferResize")
class FramebufferResizeEvent(val width: Int, val height: Int) : Event()

@Tag("mouseButton")
class MouseButtonEvent(
    val key: InputConstants.Key,
    val button: Int,
    val action: Int,
    val mods: Int,
    val screen: Screen? = null
) : Event(), WebSocketEvent

@Tag("mouseScroll")
class MouseScrollEvent(val horizontal: Double, val vertical: Double) : Event()

@Tag("mouseScrollInHotbar")
class MouseScrollInHotbarEvent(val speed: Int) : CancellableEvent()

@Tag("mouseCursor")
class MouseCursorEvent(val x: Double, val y: Double) : Event()

@Tag("keyboardKey")
class KeyboardKeyEvent(
    val key: InputConstants.Key,
    val keyCode: Int,
    val scanCode: Int,
    val action: Int,
    val mods: Int,
    val screen: Screen? = null
) : Event(), WebSocketEvent

@Tag("keyboardChar")
class KeyboardCharEvent(val codePoint: Int, val modifiers: Int) : Event(), WebSocketEvent
