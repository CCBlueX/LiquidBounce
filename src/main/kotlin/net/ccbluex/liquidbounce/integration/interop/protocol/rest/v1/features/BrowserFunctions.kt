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
package net.ccbluex.liquidbounce.integration.interop.protocol.rest.v1.features

import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.ccbluex.liquidbounce.integration.screen.impl.InternetExplorerScreen
import net.ccbluex.liquidbounce.integration.screen.impl.browserBrowsers
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.kotlin.Minecraft
import net.ccbluex.netty.http.routing.RoutingContext

// GET /api/v1/client/browser
fun RoutingContext.getBrowserInfo() {
    respond(JsonObject().apply {
        val internetExplorerScreen = mc.screen as? InternetExplorerScreen ?: return@apply
        val browser = internetExplorerScreen.browserBrowser ?: return@apply

        addProperty("url", browser.url)
    })
}

// POST /api/v1/client/browser/navigate
fun RoutingContext.postBrowserNavigate() = with(receive<Navigate>()) {
    val url = this.url
    val internetExplorerScreen = mc.screen as? InternetExplorerScreen
        ?: badRequest("No browser screen")
    val browser = internetExplorerScreen.browserBrowser
        ?: badRequest("No browser tab")

    browser.url = url
    respondNoContent()
}

private data class Navigate(val url: String)

// POST /api/v1/client/browser/close
suspend fun RoutingContext.postBrowserClose() = withContext(Dispatchers.Minecraft) {
    if (mc.screen !is InternetExplorerScreen) {
        badRequest("No browser screen")
    } else {
        mc.setScreen(null)
        respondNoContent()
    }
}

// POST /api/v1/client/browser/reload
fun RoutingContext.postBrowserReload() {
    val internetExplorerScreen = mc.screen as? InternetExplorerScreen
        ?: badRequest("No browser screen")
    val browser = internetExplorerScreen.browserBrowser
        ?: badRequest("No browser tab")

    browser.reload()
    respondNoContent()
}

// POST /api/v1/client/browser/forceReload
fun RoutingContext.postBrowserForceReload() {
    val internetExplorerScreen = mc.screen as? InternetExplorerScreen
        ?: badRequest("No browser screen")
    val browser = internetExplorerScreen.browserBrowser
        ?: badRequest("No browser tab")

    browser.forceReload()
    respondNoContent()
}

// POST /api/v1/client/browser/forward
fun RoutingContext.postBrowserForward() {
    val internetExplorerScreen = mc.screen as? InternetExplorerScreen
        ?: badRequest("No browser screen")
    val browser = internetExplorerScreen.browserBrowser
        ?: badRequest("No browser tab")

    browser.goForward()
    respondNoContent()
}

// POST /api/v1/client/browser/back
fun RoutingContext.postBrowserBack() {
    val internetExplorerScreen = mc.screen as? InternetExplorerScreen
        ?: badRequest("No browser screen")
    val browser = internetExplorerScreen.browserBrowser
        ?: badRequest("No browser tab")

    browser.goBack()
    respondNoContent()
}

// POST /api/v1/client/browser/closeTab
suspend fun RoutingContext.postBrowserCloseTab() {
    val internetExplorerScreen = mc.screen as? InternetExplorerScreen
        ?: badRequest("No browser screen")
    val browser = internetExplorerScreen.browserBrowser
        ?: badRequest("No browser tab")
    withContext(Dispatchers.Minecraft) {
        browser.close()
        browserBrowsers.remove(browser)
    }
    respondNoContent()
}
