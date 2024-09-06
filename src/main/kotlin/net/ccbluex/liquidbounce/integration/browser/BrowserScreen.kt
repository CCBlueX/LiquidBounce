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
 */
package net.ccbluex.liquidbounce.integration.browser

import net.ccbluex.liquidbounce.event.EventManager
import net.ccbluex.liquidbounce.event.events.BrowserUrlChangeEvent
import net.ccbluex.liquidbounce.utils.client.asText
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.render.refreshRate
import net.ccbluex.liquidbounce.integration.browser.supports.tab.ITab
import net.ccbluex.liquidbounce.integration.browser.supports.tab.TabPosition
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.Screen
import net.minecraft.text.Text

var browserTabs = mutableListOf<ITab>()

class BrowserScreen(val url: String, title: Text = "".asText()) : Screen(title) {

    // todo: implement multi-tab support and tab switching
    var selectedIndex = 0
    private var recentUrl = url

    val browserTab: ITab?
        get() = browserTabs.getOrNull(selectedIndex)

    override fun init() {
        val position = TabPosition(
            20,
            20,
            ((width - 20) * mc.window.scaleFactor).toInt(),
            ((height - 50) * mc.window.scaleFactor).toInt()
        )

        if (browserTabs.isEmpty()) {
            val browser = BrowserManager.browser ?: return

            browser.createInputAwareTab(url, position, refreshRate) { mc.currentScreen == this }
                .preferOnTop()
                .also { browserTabs.add(it) }
            return
        }

        // Update the position of all tabs
        browserTabs.forEach { it.position = position }
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        browserTab?.let { tab ->
            val currentUrl = tab.getUrl()

            if (recentUrl != currentUrl) {
                EventManager.callEvent(BrowserUrlChangeEvent(selectedIndex, currentUrl))
                recentUrl = currentUrl
            }
        }

        // render nothing
    }

    override fun shouldPause() = false

    override fun close() {
        // Close all tabs
        browserTabs.removeIf {
            it.closeTab()
            true
        }

        super.close()
    }

    override fun shouldCloseOnEsc() = true

}
