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
package net.ccbluex.liquidbounce.gametest

import com.mojang.blaze3d.platform.InputConstants
import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.events.ScreenEvent
import net.ccbluex.liquidbounce.integration.interop.ClientInteropServer
import net.ccbluex.liquidbounce.integration.interop.middleware.AuthConfig
import net.ccbluex.liquidbounce.integration.screen.CustomScreenType
import net.ccbluex.liquidbounce.integration.screen.impl.CustomSharedMinecraftScreen
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.gui.screens.TitleScreen
import net.minecraft.network.chat.Component
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

private const val NO_CONTENT = 204
private const val FORBIDDEN = 403

/**
 * The page of the open theme screen can keep Esc, so Esc closes a dialog, menu or search of the page and not the
 * screen. The screen gets Esc back when the page returns it and whenever the screen changes.
 */
class ThemeEscapeGameTest : FabricClientGameTest {

    private val http = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build()

    override fun runTest(context: ClientGameTestContext) {
        context.waitForClient()
        val screen = context.fromClient { CustomSharedMinecraftScreen(CustomScreenType.CLICK_GUI) }

        context.setScreen { screen }
        check(context.keepEscape("multiplayer", true) == FORBIDDEN) { "A screen that is not open could keep Esc" }
        check(context.keepEscape("clickgui", true) == NO_CONTENT) { "The open screen could not keep Esc" }
        check(!context.escapeCloses(screen)) { "Esc closed the screen while its page kept Esc" }

        context.openCancelledScreen()
        check(!context.escapeCloses(screen)) { "Esc closed the screen after a cancelled screen change" }

        check(context.keepEscape("clickgui", false) == NO_CONTENT) { "The open screen could not return Esc" }
        check(context.escapeCloses(screen)) { "Esc did not close the screen after its page returned Esc" }

        context.setScreen { screen }
        context.keepEscape("clickgui", true)
        context.setScreen { TitleScreen() }
        context.setScreen { screen }
        check(context.escapeCloses(screen)) { "The page still kept Esc after its screen was replaced" }
    }

    /**
     * Sends what a page sends to keep or return Esc.
     *
     * @return the HTTP status
     */
    private fun ClientGameTestContext.keepEscape(screenName: String, keep: Boolean): Int {
        val url = "${ClientInteropServer.url}/api/v1/client/screen/escape" +
            "?${AuthConfig.AUTH_CODE_PARAM}=${ClientInteropServer.AUTH_CODE}"
        val request = HttpRequest.newBuilder(URI(url))
            .header("Content-Type", "application/json")
            .PUT(HttpRequest.BodyPublishers.ofString("""{"name": "$screenName", "keep": $keep}"""))
            .build()
        val response = http.sendAsync(request, HttpResponse.BodyHandlers.discarding())

        // The client answers on its own thread, which only runs while the test waits
        waitFor { response.isDone }
        return response.join().statusCode()
    }

    private fun ClientGameTestContext.escapeCloses(screen: Screen): Boolean {
        input.pressKey(InputConstants.getKey("key.keyboard.escape"))
        waitTicks(2)
        return fromClient { it.gui.screen() !== screen }
    }

    /**
     * Opens a screen that a listener cancels, so the current screen stays.
     */
    private fun ClientGameTestContext.openCancelledScreen() {
        val cancelled = fromClient<Screen> { object : Screen(Component.empty()) {} }

        object : EventListener {}
            .on(ScreenEvent::class.java) { if (it.screen === cancelled) it.cancelEvent() }
            .use { onClient { it.gui.setScreen(cancelled) } }
    }

}
