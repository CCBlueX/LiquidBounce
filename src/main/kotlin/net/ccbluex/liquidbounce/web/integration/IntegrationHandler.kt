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
import net.ccbluex.liquidbounce.event.EventManager
import net.ccbluex.liquidbounce.event.Listenable
import net.ccbluex.liquidbounce.event.events.*
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.misc.HideClient
import net.ccbluex.liquidbounce.features.module.modules.misc.ModuleHideClient
import net.ccbluex.liquidbounce.mcef.MCEFDownloaderMenu
import net.ccbluex.liquidbounce.utils.client.*
import net.ccbluex.liquidbounce.web.browser.BrowserManager
import net.ccbluex.liquidbounce.web.theme.ThemeManager.integrationUrl
import net.minecraft.client.gui.screen.DisconnectedScreen
import net.minecraft.client.gui.screen.GameMenuScreen
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.gui.screen.TitleScreen
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen
import net.minecraft.client.gui.screen.ingame.InventoryScreen
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen
import net.minecraft.client.gui.screen.multiplayer.MultiplayerWarningScreen
import net.minecraft.client.gui.screen.option.OptionsScreen
import net.minecraft.client.gui.screen.world.SelectWorldScreen
import net.minecraft.client.realms.gui.screen.RealmsMainScreen
import net.minecraft.client.util.NarratorManager
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
        BrowserManager.browser?.createInputAwareTab(integrationUrl) { mc.currentScreen != null }
            ?.preferOnTop()
    }

    var momentaryVirtualScreen: VirtualScreen? = null
        private set
    val acknowledgement = Acknowledgement()

    private val standardCursor = GLFW.glfwCreateStandardCursor(GLFW.GLFW_ARROW_CURSOR)

    data class VirtualScreen(val name: String)

    data class Acknowledgement(val since: Chronometer = Chronometer(),
                                var confirmed: Boolean = false) {

        fun confirm() {
            confirmed = true
        }

        fun reset() {
            since.reset()
            confirmed = false
        }

    }

    private val parent: Screen
        get() = mc.currentScreen ?: TitleScreen()

    enum class VirtualScreenType(val assignedName: String, val recognizer: (Screen) -> Boolean,
                                 val showAlong: Boolean = false, private val open: () -> Unit = {}) {

        TITLE("title",
            {
                // todo: Do not simply replace any Lunar Screen with the title screen, if not in a world
                it is TitleScreen || (it.javaClass.name.startsWith("com.moonsworth.lunar.") && mc.world == null)
            },
            open = {
                mc.setScreen(TitleScreen())
            }),
        MULTIPLAYER("multiplayer", { it is MultiplayerScreen || it is MultiplayerWarningScreen }, true, open = {
            mc.setScreen(MultiplayerScreen(parent))
        }),
        MULTIPLAYER_REALMS("multiplayer_realms", { it is RealmsMainScreen }, true, open = {
            mc.setScreen(RealmsMainScreen(parent))
        }),
        SINGLEPLAYER("singleplayer", { it is SelectWorldScreen }, true, open = {
            mc.setScreen(SelectWorldScreen(parent))
        }),
        OPTIONS("options", { it is OptionsScreen }, true, open = {
            mc.setScreen(OptionsScreen(parent, mc.options))
        }),
        GAME_MENU("game_menu", { it is GameMenuScreen }, true),
        INVENTORY("inventory", { it is InventoryScreen || it is CreativeInventoryScreen }, true),
        CONTAINER("container", { it is GenericContainerScreen }, true),
        DISCONNECTED("disconnected", { it is DisconnectedScreen }, true);

        fun open() = RenderSystem.recordRenderCall(open)

    }

    private var browserIsReady = false

    val handleBrowserReady = handler<BrowserReadyEvent> {
        logger.info("Browser is ready.")

        // Fires up the client tab
        clientJcef
        browserIsReady = true
    }

    fun virtualOpen(name: String) {
        // Check if the virtual screen is already open
        if (momentaryVirtualScreen?.name == name) {
            return
        }

        val virtualScreen = VirtualScreen(name).apply { momentaryVirtualScreen = this }
        acknowledgement.reset()
        EventManager.callEvent(VirtualScreenEvent(virtualScreen.name,
            VirtualScreenEvent.Action.OPEN))
    }

    fun virtualClose() {
        val virtualScreen = momentaryVirtualScreen ?: return

        momentaryVirtualScreen = null
        acknowledgement.reset()
        EventManager.callEvent(VirtualScreenEvent(virtualScreen.name,
            VirtualScreenEvent.Action.CLOSE))
    }

    fun updateIntegrationBrowser() {
        if (!browserIsReady || BrowserManager.browser?.isInitialized() != true) {
            return
        }

        logger.info("Reloading integration browser ${clientJcef?.javaClass?.simpleName} to URL $integrationUrl")
        clientJcef?.loadUrl(integrationUrl)
    }

    fun restoreOriginalScreen() {
        if (mc.currentScreen is VrScreen) {
            mc.setScreen((mc.currentScreen as VrScreen).originalScreen)
        }
    }

    /**
     * Handle opening new screens
     */
    private val screenHandler = handler<ScreenEvent> { event ->
        // Set to default GLFW cursor
        GLFW.glfwSetCursor(mc.window.handle, standardCursor)

        if (handleScreenSituation(event.screen)) {
            event.cancelEvent()
        }
    }

    val desyncCheck = handler<GameTickEvent> {
        if (browserIsReady && mc.currentScreen !is MCEFDownloaderMenu) {
            handleScreenSituation(mc.currentScreen)

            if (!acknowledgement.confirmed && acknowledgement.since.hasElapsed(500)) {
                logger.warn("Integration desync detected. $acknowledgement: $integrationUrl -> ${clientJcef?.getUrl()}")
                chat("Integration desync detected. It should now be fixed.")
                acknowledgement.since.reset()
                updateIntegrationBrowser()
            }
        }
    }

    /**
     * Refresh integration browser when we change worlds, this can also mean we disconnect from a server
     * and go back to the main menu.
     */
    val worldChangeEvent = handler<WorldChangeEvent> {
        updateIntegrationBrowser()
    }

    private fun handleScreenSituation(screen: Screen?): Boolean {
        // Check for the Game narrator
        if (HideClient.isHidingNow || ModuleHideClient.enabled) {
            virtualClose()
            return false
        }

        if (!browserIsReady) {
            if (screen !is MCEFDownloaderMenu) {
                RenderSystem.recordRenderCall {
                    mc.setScreen(MCEFDownloaderMenu())
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

        val virtualScreenType =  VirtualScreenType.values().find { it.recognizer(screen) }
        if (virtualScreenType == null) {
            virtualClose()
            return false
        }

        if (!virtualScreenType.showAlong) {
            val vrScreen = VrScreen(virtualScreenType.assignedName, originalScreen = screen)
            mc.setScreen(vrScreen)
            return true
        } else {
            virtualOpen(virtualScreenType.assignedName)
        }

        return false
    }

}
