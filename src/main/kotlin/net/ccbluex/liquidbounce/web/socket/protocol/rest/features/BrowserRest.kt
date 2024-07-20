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
 *
 */
package net.ccbluex.liquidbounce.web.socket.protocol.rest.features

import com.google.gson.JsonObject
import net.ccbluex.liquidbounce.config.util.decode
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.web.integration.BrowserScreen
import net.ccbluex.liquidbounce.web.integration.browserTabs
import net.ccbluex.liquidbounce.web.socket.netty.httpBadRequest
import net.ccbluex.liquidbounce.web.socket.netty.httpOk
import net.ccbluex.liquidbounce.web.socket.netty.rest.RestNode

internal fun RestNode.browserRest() {
    get("/browser") {
        httpOk(JsonObject().apply {
            val browserScreen = mc.currentScreen as? BrowserScreen ?: return@apply
            val browserTab = browserScreen.browserTab ?: return@apply

            addProperty("url", browserTab.getUrl())
        })
    }

    // todo: fix for some reason I was not able to add it to the get above
    post("/browser/navigate") {
        data class Navigate(val url: String)
        val navigate = decode<Navigate>(it.content)

        val browserScreen = mc.currentScreen as? BrowserScreen
            ?: return@post httpBadRequest("No browser screen")
        val browserTab = browserScreen.browserTab
            ?: return@post httpBadRequest("No browser tab")

        browserTab.loadUrl(navigate.url)
        httpOk(JsonObject())
    }

    post("/browser/close") {
        val browserScreen = mc.currentScreen as? BrowserScreen
            ?: return@post httpBadRequest("No browser screen")
        mc.setScreen(null)
        httpOk(JsonObject())
    }

    post("/browser/reload") {
        val browserScreen = mc.currentScreen as? BrowserScreen
            ?: return@post httpBadRequest("No browser screen")
        val browserTab = browserScreen.browserTab
            ?: return@post httpBadRequest("No browser tab")

        browserTab.reload()
        httpOk(JsonObject())
    }

    post("/browser/forceReload") {
        val browserScreen = mc.currentScreen as? BrowserScreen
            ?: return@post httpBadRequest("No browser screen")
        val browserTab = browserScreen.browserTab
            ?: return@post httpBadRequest("No browser tab")

        browserTab.forceReload()
        httpOk(JsonObject())
    }

    post("/browser/forward") {
        val browserScreen = mc.currentScreen as? BrowserScreen
            ?: return@post httpBadRequest("No browser screen")
        val browserTab = browserScreen.browserTab
            ?: return@post httpBadRequest("No browser tab")

        browserTab.goForward()
        httpOk(JsonObject())
    }

    post("/browser/back") {
        val browserScreen = mc.currentScreen as? BrowserScreen
            ?: return@post httpBadRequest("No browser screen")
        val browserTab = browserScreen.browserTab
            ?: return@post httpBadRequest("No browser tab")

        browserTab.goBack()
        httpOk(JsonObject())
    }

    post("/browser/close") {
        val browserScreen = mc.currentScreen as? BrowserScreen
            ?: return@post httpBadRequest("No browser screen")
        val browserTab = browserScreen.browserTab
            ?: return@post httpBadRequest("No browser tab")

        browserTab.closeTab()
        browserTabs.remove(browserTab)
        httpOk(JsonObject())
    }
}
