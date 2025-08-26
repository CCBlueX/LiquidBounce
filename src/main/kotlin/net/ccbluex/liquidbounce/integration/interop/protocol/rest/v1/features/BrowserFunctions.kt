/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2025 CCBlueX
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
package net.ccbluex.liquidbounce.integration.interop.protocol.rest.v1.features

import com.google.gson.JsonObject
import net.ccbluex.liquidbounce.config.gson.util.emptyJsonObject
import net.ccbluex.liquidbounce.integration.BrowserScreen
import net.ccbluex.liquidbounce.integration.browserBrowsers
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.netty.http.model.RequestObject
import net.ccbluex.netty.http.util.httpBadRequest
import net.ccbluex.netty.http.util.httpOk

// GET /api/v1/client/browser
@Suppress("UNUSED_PARAMETER")
fun getBrowserInfo(requestObject: RequestObject) = httpOk(JsonObject().apply {
    val browserScreen = mc.currentScreen as? BrowserScreen ?: return@apply
    val browser = browserScreen.browserBrowser ?: return@apply

    addProperty("url", browser.url)
})

// POST /api/v1/client/browser/navigate
@Suppress("UNUSED_PARAMETER")
fun postBrowserNavigate(requestObject: RequestObject) = with(requestObject.asJson<Navigate>()) {
    val url = this.url
    val browserScreen = mc.currentScreen as? BrowserScreen
        ?: return@with httpBadRequest("No browser screen")
    val browser = browserScreen.browserBrowser
        ?: return@with httpBadRequest("No browser tab")

    browser.url = url
    httpOk(emptyJsonObject())
}

private data class Navigate(val url: String)

// POST /api/v1/client/browser/close
@Suppress("UNUSED_PARAMETER")
fun postBrowserClose(requestObject: RequestObject) = with(requestObject) {
    val browserScreen = mc.currentScreen as? BrowserScreen
        ?: return@with httpBadRequest("No browser screen")
    mc.setScreen(null)
    httpOk(emptyJsonObject())
}

// POST /api/v1/client/browser/reload
@Suppress("UNUSED_PARAMETER")
fun postBrowserReload(requestObject: RequestObject) = with(requestObject) {
    val browserScreen = mc.currentScreen as? BrowserScreen
        ?: return@with httpBadRequest("No browser screen")
    val browser = browserScreen.browserBrowser
        ?: return@with httpBadRequest("No browser tab")

    browser.reload()
    httpOk(emptyJsonObject())
}

// POST /api/v1/client/browser/forceReload
@Suppress("UNUSED_PARAMETER")
fun postBrowserForceReload(requestObject: RequestObject) = with(requestObject) {
    val browserScreen = mc.currentScreen as? BrowserScreen
        ?: return@with httpBadRequest("No browser screen")
    val browser = browserScreen.browserBrowser
        ?: return@with httpBadRequest("No browser tab")

    browser.forceReload()
    httpOk(emptyJsonObject())
}

// POST /api/v1/client/browser/forward
@Suppress("UNUSED_PARAMETER")
fun postBrowserForward(requestObject: RequestObject) = with(requestObject) {
    val browserScreen = mc.currentScreen as? BrowserScreen
        ?: return@with httpBadRequest("No browser screen")
    val browser = browserScreen.browserBrowser
        ?: return@with httpBadRequest("No browser tab")

    browser.goForward()
    httpOk(emptyJsonObject())
}

// POST /api/v1/client/browser/back
@Suppress("UNUSED_PARAMETER")
fun postBrowserBack(requestObject: RequestObject) = with(requestObject) {
    val browserScreen = mc.currentScreen as? BrowserScreen
        ?: return@with httpBadRequest("No browser screen")
    val browser = browserScreen.browserBrowser
        ?: return@with httpBadRequest("No browser tab")

    browser.goBack()
    httpOk(emptyJsonObject())
}

// POST /api/v1/client/browser/closeTab
@Suppress("UNUSED_PARAMETER")
fun postBrowserCloseTab(requestObject: RequestObject) = with(requestObject) {
    val browserScreen = mc.currentScreen as? BrowserScreen
        ?: return@with httpBadRequest("No browser screen")
    val browser = browserScreen.browserBrowser
        ?: return@with httpBadRequest("No browser tab")

    browser.close()
    browserBrowsers.remove(browser)
    httpOk(emptyJsonObject())
}
