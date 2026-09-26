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

import net.ccbluex.liquidbounce.integration.backend.BrowserBackendManager
import net.ccbluex.liquidbounce.integration.backend.browser.Browser
import net.ccbluex.liquidbounce.integration.backend.browser.BrowserViewport
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.text.PlainText
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.Screen
import net.raphimc.minecraftauth.msa.service.impl.ExternalBrowserMsaAuthService

/**
 * Hosts the Microsoft login page in a CEF browser.
 *
 * MinecraftAuth ships a JavaFX web view for this, but we do not ship JavaFX, so
 * [ExternalBrowserMsaAuthService] lets the login run in the browser the client already has. The screen
 * reports every URL the page navigates to, and the service picks the auth code out of the redirect.
 *
 * The browser is incognito, so the Microsoft session never reaches the client's cookie store. Without
 * that, signing in would leave the account logged in for anyone who opens a browser afterwards, and
 * adding a second account would silently reuse the first one's session instead of asking who to sign
 * in as.
 *
 * The screen does not close itself on success - the service's close callback does, once the auth code has
 * been exchanged for a token.
 */
class MicrosoftLoginScreen(
    private val url: String,
    private val service: ExternalBrowserMsaAuthService,
    private val parent: Screen?,
) : Screen(PlainText.EMPTY) {

    private var browser: Browser? = null
    private var recentUrl = url

    override fun init() {
        val viewport = BrowserViewport(
            20,
            20,
            (width - 20) * mc.window.guiScale,
            (height - 50) * mc.window.guiScale
        )

        val browser = browser
        if (browser != null) {
            browser.viewport = viewport
            return
        }

        val backend = BrowserBackendManager.backend ?: return
        this.browser = backend.createBrowser(url, viewport, priority = 20, incognito = true) {
            mc.gui.screen() == this
        }
    }

    override fun extractRenderState(context: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        val currentUrl = browser?.url ?: return

        if (currentUrl == recentUrl || currentUrl.isEmpty()) {
            return
        }
        recentUrl = currentUrl

        service.handleNavigation(currentUrl)
    }

    override fun isPauseScreen() = false

    override fun shouldCloseOnEsc() = true

    override fun onClose() {
        mc.gui.setScreen(parent)
    }

    /**
     * Cleanup lives here rather than in [onClose] because the screen is also dismissed programmatically
     * once the sign-in finishes, and that path only goes through [removed].
     */
    override fun removed() {
        // No-op once the auth code has been captured, so this only cancels a login the user walked away
        // from.
        service.cancel()

        browser?.close()
        browser = null

        super.removed()
    }

}
