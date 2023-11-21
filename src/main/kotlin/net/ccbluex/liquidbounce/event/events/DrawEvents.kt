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
 *
 */

package net.ccbluex.liquidbounce.event.events

import net.ccbluex.liquidbounce.event.Event
import net.ccbluex.liquidbounce.utils.client.Nameable
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.util.math.MatrixStack

@Nameable("gameRender")
class GameRenderEvent : Event()

@Nameable("worldRender")
class WorldRenderEvent(val matrixStack: MatrixStack, val partialTicks: Float) : Event()

@Nameable("overlayRender")
class OverlayRenderEvent(val context: DrawContext, val tickDelta: Float) : Event()

@Nameable("screenRender")
class ScreenRenderEvent(val screen: Screen?, val context: DrawContext, val mouseX: Int, val mouseY: Int,
                        val delta: Float) : Event()
