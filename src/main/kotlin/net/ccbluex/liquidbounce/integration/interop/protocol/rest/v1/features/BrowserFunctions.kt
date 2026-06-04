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
import net.ccbluex.netty.http.routing.Routing

// GET /api/v1/client/browser
private fun Routing.getBrowserInfo() = get {
    call.respond(JsonObject().apply {
        val internetExplorerScreen = mc.gui.screen() as? InternetExplorerScreen ?: return@apply
        val browser = internetExplorerScreen.browserBrowser ?: return@apply

        addProperty("url", browser.url)
    })
}

// POST /api/v1/client/browser/navigate
private fun Routing.postBrowserNavigate() = post("/navigate") { with(call.receive<Navigate>()) {
    val url = this.url
    val internetExplorerScreen = mc.gui.screen() as? InternetExplorerScreen
        ?: call.badRequest("No browser screen")
    val browser = internetExplorerScreen.browserBrowser
        ?: call.badRequest("No browser tab")

    browser.url = url
    call.respondNoContent()
} }

private data class Navigate(val url: String)

// POST /api/v1/client/browser/close
private fun Routing.postBrowserClose() = post("/close") { withContext(Dispatchers.Minecraft) {
    if (mc.gui.screen() !is InternetExplorerScreen) {
        call.badRequest("No browser screen")
    } else {
        mc.gui.setScreen(null)
        call.respondNoContent()
    }
} }

// POST /api/v1/client/browser/reload
private fun Routing.postBrowserReload() = post("/reload") {
    val internetExplorerScreen = mc.gui.screen() as? InternetExplorerScreen
        ?: call.badRequest("No browser screen")
    val browser = internetExplorerScreen.browserBrowser
        ?: call.badRequest("No browser tab")

    browser.reload()
    call.respondNoContent()
}

// POST /api/v1/client/browser/forceReload
private fun Routing.postBrowserForceReload() = post("/forceReload") {
    val internetExplorerScreen = mc.gui.screen() as? InternetExplorerScreen
        ?: call.badRequest("No browser screen")
    val browser = internetExplorerScreen.browserBrowser
        ?: call.badRequest("No browser tab")

    browser.forceReload()
    call.respondNoContent()
}

// POST /api/v1/client/browser/forward
private fun Routing.postBrowserForward() = post("/forward") {
    val internetExplorerScreen = mc.gui.screen() as? InternetExplorerScreen
        ?: call.badRequest("No browser screen")
    val browser = internetExplorerScreen.browserBrowser
        ?: call.badRequest("No browser tab")

    browser.goForward()
    call.respondNoContent()
}

// POST /api/v1/client/browser/back
private fun Routing.postBrowserBack() = post("/back") {
    val internetExplorerScreen = mc.gui.screen() as? InternetExplorerScreen
        ?: call.badRequest("No browser screen")
    val browser = internetExplorerScreen.browserBrowser
        ?: call.badRequest("No browser tab")

    browser.goBack()
    call.respondNoContent()
}

// POST /api/v1/client/browser/closeTab
private fun Routing.postBrowserCloseTab() = post("/closeTab") {
    val internetExplorerScreen = mc.gui.screen() as? InternetExplorerScreen
        ?: call.badRequest("No browser screen")
    val browser = internetExplorerScreen.browserBrowser
        ?: call.badRequest("No browser tab")
    withContext(Dispatchers.Minecraft) {
        browser.close()
        browserBrowsers.remove(browser)
    }
    call.respondNoContent()
}

internal fun Routing.browserRoutes() = route("/browser") {
    getBrowserInfo()
    postBrowserNavigate()
    postBrowserClose()
    postBrowserReload()
    postBrowserForceReload()
    postBrowserForward()
    postBrowserBack()
    postBrowserCloseTab()
}
