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
package net.ccbluex.liquidbounce.integration.screen.impl

import net.ccbluex.liquidbounce.event.EventManager
import net.ccbluex.liquidbounce.event.events.BrowserUrlChangeEvent
import net.ccbluex.liquidbounce.integration.backend.BrowserBackendManager
import net.ccbluex.liquidbounce.integration.backend.browser.Browser
import net.ccbluex.liquidbounce.integration.backend.browser.BrowserViewport
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.text.PlainText
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component

val browserBrowsers = mutableListOf<Browser>()

class InternetExplorerScreen(val url: String, title: Component = PlainText.EMPTY) : Screen(title) {

    // todo: implement multi-tab support and tab switching
    var selectedIndex = 0
    private var recentUrl = url

    val browserBrowser: Browser?
        get() = browserBrowsers.getOrNull(selectedIndex)

    override fun init() {
        val viewport = BrowserViewport(
            20,
            20,
            (width - 20) * mc.window.guiScale,
            (height - 50) * mc.window.guiScale
        )

        if (browserBrowsers.isEmpty()) {
            val browser = BrowserBackendManager.backend ?: return
            browser.createBrowser(url, viewport, priority = 20) { mc.screen == this }
                .also { browserBrowsers.add(it) }
            return
        }

        // Update the position of all tabs
        browserBrowsers.forEach { browser -> browser.viewport = viewport }
    }

    override fun render(context: GuiGraphics, mouseX: Int, mouseY: Int, delta: Float) {
        browserBrowser?.let { browser ->
            val currentUrl = browser.url

            if (recentUrl != currentUrl) {
                EventManager.callEvent(BrowserUrlChangeEvent(selectedIndex, currentUrl))
                recentUrl = currentUrl
            }
        }

        // render nothing
    }

    override fun isPauseScreen() = false

    override fun onClose() {
        // Close all tabs
        browserBrowsers.removeIf {
            it.close()
            true
        }

        super.onClose()
    }

    override fun shouldCloseOnEsc() = true

}
