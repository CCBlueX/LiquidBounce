/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2023 CCBlueX
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
import net.ccbluex.liquidbounce.event.events.ScreenEvent
import net.ccbluex.liquidbounce.event.events.VirtualScreenEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.misc.HideClient
import net.ccbluex.liquidbounce.features.module.modules.misc.ModuleHideClient
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.web.browser.BrowserManager
import net.ccbluex.liquidbounce.web.theme.ThemeManager.integrationUrl
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

    data class VirtualScreen(val name: String)

    private val parent: Screen
        get() = mc.currentScreen ?: TitleScreen()

    enum class VirtualScreenType(val assignedName: String, val recognizer: (Screen) -> Boolean,
                                 val showAlong: Boolean = false, private val open: () -> Unit = {}) {

        TITLE("title", { it is TitleScreen }, open = {
            mc.setScreen(TitleScreen())
        }),
        MULTIPLAYER("multiplayer", { it is MultiplayerScreen || it is MultiplayerWarningScreen }, true, open = {
            mc.setScreen(MultiplayerScreen(parent))
        }),
        SINGLEPLAYER("singleplayer", { it is SelectWorldScreen }, true, open = {
            mc.setScreen(SelectWorldScreen(parent))
        }),
        OPTIONS("options", { it is OptionsScreen }, true, open = {
            mc.setScreen(OptionsScreen(parent, mc.options))
        }),
        GAME_MENU("game_menu", { it is GameMenuScreen }, true),
        INVENTORY("inventory", { it is InventoryScreen || it is CreativeInventoryScreen }, true),
        CONTAINER("container", { it is GenericContainerScreen }, true);

        fun open() = RenderSystem.recordRenderCall(open)

    }

    fun virtualOpen(name: String) {
        virtualClose()
        val virtualScreen = VirtualScreen(name).apply { momentaryVirtualScreen = this }
        EventManager.callEvent(VirtualScreenEvent(virtualScreen.name,
            VirtualScreenEvent.Action.OPEN))
    }

    fun virtualClose() {
        EventManager.callEvent(VirtualScreenEvent(momentaryVirtualScreen?.name ?: return,
            VirtualScreenEvent.Action.CLOSE))
        momentaryVirtualScreen = null
    }

    fun updateIntegrationBrowser() {
        clientJcef?.loadUrl(integrationUrl)
    }

    /**
     * Handle opening new screens
     */
    private val screenHandler = handler<ScreenEvent> { event ->
        if (HideClient.isHidingNow || ModuleHideClient.enabled) {
            return@handler
        }

        if (event.screen is VrScreen) {
            return@handler
        }

        val screen = event.screen ?: if (mc.world != null) {
            virtualClose()
            return@handler
        } else {
            TitleScreen()
        }

        val virtualScreenType =  VirtualScreenType.values().find { it.recognizer(screen) }
        if (virtualScreenType == null) {
            virtualClose()
            return@handler
        }

        if (!virtualScreenType.showAlong) {
            val emptyScreen = VrScreen(virtualScreenType.assignedName)
            mc.setScreen(emptyScreen)
            event.cancelEvent()
        } else {
            virtualOpen(virtualScreenType.assignedName)
        }
    }

}
