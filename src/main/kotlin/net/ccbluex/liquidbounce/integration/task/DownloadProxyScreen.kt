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
package net.ccbluex.liquidbounce.integration.task

import kotlinx.coroutines.CompletableDeferred
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.text.asPlainText
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.components.EditBox
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component
import java.lang.management.ManagementFactory

/** Select a proxy before online resources and dependencies start downloading. */
class DownloadProxyScreen(private val selection: CompletableDeferred<DownloadProxy.Choice>) :
    Screen("Dependency downloads".asPlainText()) {
    private var mode = 0
    private lateinit var address: EditBox
    private var error = ""

    override fun init() {
        val x = width / 2 - 110
        val y = height / 2 - 20
        address = EditBox(font, x, y + 24, 220, 20, Component.literal("HTTP proxy host:port"))
        address.setHint(Component.literal("HTTP proxy host:port"))
        address.isVisible = mode == 2
        addRenderableWidget(Button.builder(modeLabel()) {
            mode = (mode + 1) % 3
            it.message = modeLabel()
            address.isVisible = mode == 2
            error = ""
        }.bounds(x, y, 220, 20).build())
        addRenderableWidget(address)
        addRenderableWidget(Button.builder("Continue".asPlainText()) {
            val choice = runCatching {
                when (mode) {
                    1 -> {
                        require(ManagementFactory.getRuntimeMXBean().inputArguments.any {
                            it.equals("-Djava.net.useSystemProxies=true", ignoreCase = true)
                        }) { "Enable system proxies in the launcher and restart." }
                        DownloadProxy.Choice.System
                    }
                    2 -> DownloadProxy.Choice.Custom(DownloadProxy.parseAddress(address.value))
                    else -> DownloadProxy.Choice.Direct
                }
            }.getOrElse {
                error = it.message ?: "Invalid proxy address."
                return@builder
            }
            if (selection.complete(choice)) mc.gui.setScreen(null)
        }.bounds(x, y + 54, 220, 20).build())
    }

    private fun modeLabel() = when (mode) {
        1 -> "Proxy: system"
        2 -> "Proxy: custom HTTP"
        else -> "Proxy: off"
    }.asPlainText()

    override fun extractRenderState(context: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, partialTick: Float) {
        super.extractRenderState(context, mouseX, mouseY, partialTick)
        val y = height / 2 - 20
        context.centeredText(font, title, width / 2, y - 44, -1)
        context.centeredText(font, "Choose a proxy for initial downloads.".asPlainText(), width / 2, y - 28, -1)
        if (mode == 1) {
            context.centeredText(
                font, "Launcher: -Djava.net.useSystemProxies=true".asPlainText(), width / 2, y + 80, -1
            )
        }
        if (error.isNotEmpty()) context.centeredText(font, error.asPlainText(), width / 2, y + 96, 0xFFFF5555.toInt())
    }

    override fun shouldCloseOnEsc() = false
}
