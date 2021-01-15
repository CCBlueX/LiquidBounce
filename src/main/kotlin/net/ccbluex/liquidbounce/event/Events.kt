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
package net.ccbluex.liquidbounce.event

import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.util.InputUtil
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.network.Packet

class EntityTickEvent : Event()

class ChatSendEvent(val message: String) : CancellableEvent()

/**
 * Calls when a frame is rendered and the modules are supposed to enqueue their render tasks
 */
class LiquidBounceRenderEvent : Event()

/**
 * Called when the Minecraft HUD is rendered (at the beginning). Please don't use it to do anything with the render engine.
 */
class RenderHudEvent(val matrixStack: MatrixStack, val tickDelta: Float) : Event()

class PacketEvent(val packet: Packet<*>) : CancellableEvent()

class KeyEvent(val key: InputUtil.Key, val action: Int, val mods: Int) : Event()

class SessionEvent : Event()

class ScreenEvent(val screen: Screen?) : Event()
