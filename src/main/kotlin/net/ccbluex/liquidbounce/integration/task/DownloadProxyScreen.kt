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
import java.net.Proxy
import java.net.URI
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

internal class ProxySelectionTimeout(private val started: Long = System.nanoTime()) {
    private var interacted = false

    fun interact() { interacted = true }

    fun hasExpired(now: Long = System.nanoTime()): Boolean =
        !interacted && now - started >= TimeUnit.SECONDS.toNanos(5)

    fun secondsRemaining(now: Long = System.nanoTime()): Long? {
        if (interacted) return null
        val remaining = (TimeUnit.SECONDS.toNanos(5) - (now - started)).coerceAtLeast(0)
        return (remaining + TimeUnit.SECONDS.toNanos(1) - 1) / TimeUnit.SECONDS.toNanos(1)
    }
}

/** Select a proxy before online resources and dependencies start downloading. */
class DownloadProxyScreen(
    private val selection: CompletableDeferred<DownloadProxy.Choice>,
    private val initialChoice: DownloadProxy.Choice = DownloadProxy.choice,
    private val returnScreen: Screen? = null,
    private val saveChoice: ((DownloadProxy.Choice) -> Unit)? = null,
) :
    Screen("Dependency downloads".asPlainText()) {
    private var mode = when (initialChoice) {
        DownloadProxy.Choice.Direct -> 0
        DownloadProxy.Choice.System -> 1
        is DownloadProxy.Choice.Custom -> if (initialChoice.type == Proxy.Type.SOCKS) 3 else 2
    }
    private lateinit var address: EditBox
    private lateinit var continueButton: Button
    private lateinit var modeButton: Button
    private var error = ""
    private var systemDirectConfirmed = false
    private val timeout = ProxySelectionTimeout().also { if (saveChoice != null) it.interact() }
    private val fallback = initialChoice

    override fun init() {
        val x = width / 2 - 110
        val y = height / 2 - 20
        address = EditBox(font, x, y + 24, 220, 20, Component.literal("Proxy host:port"))
        address.setHint(Component.literal("Proxy host:port"))
        (initialChoice as? DownloadProxy.Choice.Custom)?.address?.let {
            val host = it.hostString
            address.value = "${if (':' in host) "[$host]" else host}:${it.port}"
        }
        address.isVisible = mode >= 2
        modeButton = Button.builder(modeLabel()) {
            timeout.interact()
            mode = (mode + 1) % 4
            it.message = modeLabel()
            address.isVisible = mode >= 2
            error = ""
            systemDirectConfirmed = false
        }.bounds(x, y, 220, 20).build()
        addRenderableWidget(modeButton)
        addRenderableWidget(address)
        continueButton = Button.builder("Continue (5s)".asPlainText()) {
            timeout.interact()
            submit()
        }.bounds(x, y + 54, 220, 20).build()
        addRenderableWidget(continueButton)
    }

    private fun submit() {
        val choice = runCatching {
            when (mode) {
                1 -> DownloadProxy.Choice.System
                2, 3 -> DownloadProxy.Choice.Custom(
                    DownloadProxy.parseAddress(address.value),
                    if (mode == 3) Proxy.Type.SOCKS else Proxy.Type.HTTP
                )
                else -> DownloadProxy.Choice.Direct
            }
        }.getOrElse {
            error = it.message ?: "Invalid proxy address."
            return
        }
        if (choice == DownloadProxy.Choice.System && !systemDirectConfirmed) {
            checkSystemProxy(choice)
        } else {
            finish(choice)
        }
    }

    private fun checkSystemProxy(choice: DownloadProxy.Choice) {
        modeButton.active = false
        continueButton.active = false
        error = "Checking system proxy settings..."
        CompletableFuture.supplyAsync {
            DownloadProxy.selector(choice).select(URI("https://github.com"))
        }.whenComplete { proxies, failure ->
            mc.execute {
                if (mc.gui.screen() !== this) return@execute
                modeButton.active = true
                continueButton.active = true
                if (failure != null) {
                    error = "Could not read system proxy settings."
                } else if (proxies.all { it.type() == Proxy.Type.DIRECT }) {
                    systemDirectConfirmed = true
                    error = "github.com is direct; press Continue to accept."
                } else {
                    finish(choice)
                }
            }
        }
    }

    private fun finish(choice: DownloadProxy.Choice) {
        try {
            saveChoice?.invoke(choice)
        } catch (exception: Exception) {
            error = "Could not save proxy settings: ${exception.message}"
            return
        }
        if (selection.complete(choice)) mc.gui.setScreen(returnScreen)
    }

    private fun modeLabel() = when (mode) {
        1 -> "Proxy: system"
        2 -> "Proxy: custom HTTP/HTTPS"
        3 -> "Proxy: custom SOCKS5"
        else -> "Proxy: off"
    }.asPlainText()

    override fun tick() {
        val remaining = timeout.secondsRemaining()
        val label = if (saveChoice != null) "Save for next launch" else "Continue"
        continueButton.message = (if (remaining == null) label else "$label (${remaining}s)").asPlainText()
        if (timeout.hasExpired() && selection.complete(fallback)) mc.gui.setScreen(null)
    }

    override fun extractRenderState(context: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, partialTick: Float) {
        super.extractRenderState(context, mouseX, mouseY, partialTick)
        val y = height / 2 - 20
        context.centeredText(font, title, width / 2, y - 44, -1)
        context.centeredText(font, "Choose a proxy for initial downloads.".asPlainText(), width / 2, y - 28, -1)
        if (error.isNotEmpty()) context.centeredText(font, error.asPlainText(), width / 2, y + 96, 0xFFFF5555.toInt())
    }

    override fun shouldCloseOnEsc() = saveChoice != null

    override fun onClose() {
        if (saveChoice != null) mc.gui.setScreen(returnScreen)
    }
}
