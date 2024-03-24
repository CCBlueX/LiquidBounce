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
package net.ccbluex.liquidbounce.web.integration

import com.mojang.blaze3d.systems.RenderSystem
import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.event.EventManager
import net.ccbluex.liquidbounce.event.Listenable
import net.ccbluex.liquidbounce.event.events.*
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.misc.HideAppearance
import net.ccbluex.liquidbounce.mcef.progress.MCEFProgressMenu
import net.ccbluex.liquidbounce.utils.client.Chronometer
import net.ccbluex.liquidbounce.utils.client.logger
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.web.browser.BrowserManager
import net.ccbluex.liquidbounce.web.theme.Theme
import net.ccbluex.liquidbounce.web.theme.ThemeManager
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.gui.screen.TitleScreen
import org.lwjgl.glfw.GLFW

object IntegrationHandler : Listenable {

    /**
     * This tab is always open and initialized. We keep this tab open to make it possible to draw on the screen,
     * even when no specific tab is open.
     * It also reduces the time required to open a new tab and allows for smooth transitions between tabs.
     *
     * The client tab will be initialized when the browser is ready.
     */
    val clientJcef by lazy {
        ThemeManager.openInputAwareImmediate().preferOnTop()
    }

    var momentaryVirtualScreen: VirtualScreen? = null
        private set

    var runningTheme = ThemeManager.activeTheme
        private set

    /**
     * Acknowledgement is used to detect desyncs between the integration browser and the client.
     * It is reset when the client opens a new screen and confirmed when the integration browser
     * opens the same screen.
     *
     * If the acknowledgement is not confirmed after 500ms, the integration browser will be reloaded.
     */
    val acknowledgement = Acknowledgement()

    private val standardCursor = GLFW.glfwCreateStandardCursor(GLFW.GLFW_ARROW_CURSOR)

    data class VirtualScreen(val type: VirtualScreenType, val openSince: Chronometer = Chronometer())

    class Acknowledgement(
        val since: Chronometer = Chronometer(),
        var confirmed: Boolean = false
    ) {

        @Suppress("unused")
        val isDesynced
            get() = !confirmed && since.hasElapsed(1000)

        fun confirm() {
            confirmed = true
        }

        fun reset() {
            since.reset()
            confirmed = false
        }

    }

    internal val parent: Screen
        get() = mc.currentScreen ?: TitleScreen()

    private var browserIsReady = false

    @Suppress("unused")
    val handleBrowserReady = handler<BrowserReadyEvent> {
        logger.info("Browser is ready.")

        // Fires up the client tab
        clientJcef
        browserIsReady = true
    }

    @Suppress("unused")
    fun virtualOpen(name: String) {
        val type = VirtualScreenType.byName(name) ?: return
        virtualOpen(type = type)
    }

    fun virtualOpen(theme: Theme = ThemeManager.activeTheme, type: VirtualScreenType) {
        // Check if the virtual screen is already open
        if (momentaryVirtualScreen?.type == type) {
            return
        }

        if (runningTheme != theme) {
            runningTheme = theme
            ThemeManager.updateImmediate(clientJcef, type)
        }

        val virtualScreen = VirtualScreen(type).apply { momentaryVirtualScreen = this }
        acknowledgement.reset()
        EventManager.callEvent(
            VirtualScreenEvent(
                virtualScreen.type.routeName,
                VirtualScreenEvent.Action.OPEN
            )
        )
    }

    fun virtualClose() {
        val virtualScreen = momentaryVirtualScreen ?: return

        momentaryVirtualScreen = null
        acknowledgement.reset()
        EventManager.callEvent(
            VirtualScreenEvent(
                virtualScreen.type.routeName,
                VirtualScreenEvent.Action.CLOSE
            )
        )
    }

    fun updateIntegrationBrowser() {
        if (!browserIsReady || BrowserManager.browser?.isInitialized() != true) {
            return
        }

        logger.info(
            "Reloading integration browser ${clientJcef.javaClass.simpleName} " +
                    "to ${ThemeManager.route()}"
        )
        ThemeManager.updateImmediate(clientJcef, momentaryVirtualScreen?.type)
    }

    fun restoreOriginalScreen() {
        if (mc.currentScreen is VrScreen) {
            mc.setScreen((mc.currentScreen as VrScreen).originalScreen)
        }
    }

    /**
     * Handle opening new screens
     */
    @Suppress("unused")
    val screenHandler = handler<ScreenEvent> { event ->
        // Set to default GLFW cursor
        GLFW.glfwSetCursor(mc.window.handle, standardCursor)

        if (handleScreenSituation(event.screen)) {
            event.cancelEvent()
        }
    }

    @Suppress("unused")
    val screenRefresher = handler<GameTickEvent> {
        if (browserIsReady && mc.currentScreen !is MCEFProgressMenu) {
            handleScreenSituation(mc.currentScreen)
        }
    }

    /**
     * Refresh integration browser when we change worlds, this can also mean we disconnect from a server
     * and go back to the main menu.
     */
    @Suppress("unused")
    val worldChangeEvent = handler<WorldChangeEvent> {
        updateIntegrationBrowser()
    }

    private fun handleScreenSituation(screen: Screen?): Boolean {
        if (screen !is VrScreen && HideAppearance.isHidingNow) {
            virtualClose()
            return false
        }

        if (!browserIsReady) {
            if (screen !is MCEFProgressMenu) {
                RenderSystem.recordRenderCall {
                    mc.setScreen(MCEFProgressMenu(LiquidBounce.CLIENT_NAME))
                }
                return true
            }

            return false
        }

        if (screen is VrScreen) {
            return false
        }

        val screen = screen ?: if (mc.world != null) {
            virtualClose()
            return false
        } else {
            TitleScreen()
        }

        val virtualScreenType = VirtualScreenType.recognize(screen)
        if (virtualScreenType == null) {
            virtualClose()
            return false
        }

        val name = virtualScreenType.routeName
        val route = runCatching {
            ThemeManager.route(virtualScreenType, false)
        }.getOrNull()

        if (route == null) {
            virtualClose()
            return false
        }

        val theme = route.theme

        if (theme.doesSupport(name)) {
            val vrScreen = VrScreen(virtualScreenType, theme, originalScreen = screen)
            mc.setScreen(vrScreen)
            return true
        } else if (theme.doesOverlay(name)) {
            virtualOpen(theme, virtualScreenType)
        } else {
            virtualClose()
        }

        return false
    }

}
