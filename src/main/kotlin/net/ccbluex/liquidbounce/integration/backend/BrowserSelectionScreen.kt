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
package net.ccbluex.liquidbounce.integration.backend

import kotlinx.coroutines.CompletableDeferred
import net.ccbluex.liquidbounce.utils.client.mc
import net.minecraft.ChatFormatting
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component

private const val BUTTON_WIDTH = 220
private const val BUTTON_HEIGHT = 20
private const val ENTRY_HEIGHT = 44

/**
 * Asks which browser backend to use, before any is loaded.
 */
class BrowserSelectionScreen(
    private val backends: List<BrowserBackendProvider>,
    private val selection: CompletableDeferred<BrowserBackendProvider>,
) : Screen(Component.literal("Choose a browser")) {

    private val subtitle = Component.literal("LiquidBounce shows its interface in a browser. Pick the one to use.")
    private val hint = Component.literal("Hold Shift while the client starts to choose again.")
        .withStyle(ChatFormatting.GRAY)

    private val top
        get() = height / 2 - backends.size * ENTRY_HEIGHT / 2

    override fun init() {
        val x = width / 2 - BUTTON_WIDTH / 2
        for ((index, backend) in backends.withIndex()) {
            addRenderableWidget(
                Button.builder(Component.literal(backend.name)) {
                    selection.complete(backend)
                    mc.gui.setScreen(null)
                }
                    .bounds(x, top + index * ENTRY_HEIGHT, BUTTON_WIDTH, BUTTON_HEIGHT)
                    .build()
            )
        }
    }

    override fun extractRenderState(context: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, partialTick: Float) {
        super.extractRenderState(context, mouseX, mouseY, partialTick)

        val cx = width / 2
        context.centeredText(font, title.copy().withStyle(ChatFormatting.GOLD), cx, top - 44, -1)
        context.centeredText(font, subtitle, cx, top - 28, -1)

        for ((index, backend) in backends.withIndex()) {
            val y = top + index * ENTRY_HEIGHT + BUTTON_HEIGHT + 4
            context.centeredText(font, Component.literal(backend.description).withStyle(ChatFormatting.GRAY), cx, y, -1)
        }

        context.centeredText(font, hint, cx, top + backends.size * ENTRY_HEIGHT + 8, -1)
    }

    override fun shouldCloseOnEsc() = false

}
