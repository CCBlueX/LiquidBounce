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
 */
package net.ccbluex.liquidbounce.features.module.modules.render

import net.ccbluex.liquidbounce.config.types.nesting.Configurable
import net.ccbluex.liquidbounce.event.EventManager
import net.ccbluex.liquidbounce.event.events.BrowserReadyEvent
import net.ccbluex.liquidbounce.event.events.DisconnectEvent
import net.ccbluex.liquidbounce.event.events.ScreenEvent
import net.ccbluex.liquidbounce.event.events.SpaceSeperatedNamesChangeEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.misc.HideAppearance.isDestructed
import net.ccbluex.liquidbounce.features.misc.HideAppearance.isHidingNow
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleHud.themes
import net.ccbluex.liquidbounce.integration.VirtualScreenType
import net.ccbluex.liquidbounce.integration.backend.browser.Browser
import net.ccbluex.liquidbounce.integration.backend.browser.BrowserSettings
import net.ccbluex.liquidbounce.integration.backend.browser.GlobalBrowserSettings
import net.ccbluex.liquidbounce.integration.theme.ThemeManager
import net.ccbluex.liquidbounce.integration.theme.component.components.minimap.MinimapComponent
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.inGame
import net.ccbluex.liquidbounce.utils.client.markAsError
import net.minecraft.client.gui.screen.DisconnectedScreen
import net.minecraft.client.gui.screen.DownloadingTerrainScreen

/**
 * Module HUD
 *
 * The client in-game dashboard.
 */

object ModuleHud : ClientModule("HUD", Category.RENDER, state = true, hide = true) {

    override val running
        get() = this.enabled && !isDestructed

    private val visible: Boolean
        get() = !isHidingNow && inGame

    override val baseKey: String
        get() = "liquidbounce.module.hud"
    private var browserBrowser: Browser? = null

    private val blur by boolean("Blur", true)

    @Suppress("unused")
    private val spaceSeperatedNames by boolean("SpaceSeperatedNames", true).onChange { state ->
        EventManager.callEvent(SpaceSeperatedNamesChangeEvent(state))
        state
    }

    val centeredCrosshair by boolean("CenteredCrosshair", false)

    val isBlurEffectActive
        get() = blur && !(mc.options.hudHidden && mc.currentScreen == null)

    private var browserSettings: BrowserSettings? = null

    val themes = tree(Configurable("Themes"))

    val components = tree(Configurable("AdditionalComponents")).apply {
        tree(MinimapComponent)
    }

    /**
     * Updates [themes] content
     */
    fun updateThemes() {
        themes.inner.clear()
        for (theme in ThemeManager.themes) {
            themes.tree(theme.settings)
        }
        themes.initConfigurable()
        themes.walkKeyPath()
    }

    override fun onEnabled() {
        if (isHidingNow) {
            chat(markAsError(message("hidingAppearance")))
        }

        if (visible) {
            open()
        }
    }

    override fun onDisabled() {
        // Closes tab entirely
        close()
    }

    @Suppress("unused")
    private val browserReadyHandler = handler<BrowserReadyEvent> { event ->
        tree(GlobalBrowserSettings)
        browserSettings = tree(BrowserSettings(60, ::reopen))
    }

    @Suppress("unused")
    private val screenHandler = handler<ScreenEvent> { event ->
        // Close the tab when the HUD is not running, is hiding now, or the player is not in-game
        if (!enabled || !visible) {
            close()
            return@handler
        }

        // Otherwise, open the tab and set its visibility
        val browserTab = open()
        browserTab.visible = event.screen !is DisconnectedScreen && event.screen !is DownloadingTerrainScreen
    }

    @Suppress("unused")
    private val disconnectHandler = handler<DisconnectEvent> {
        close()
    }

    private fun open(): Browser {
        browserBrowser?.let { return it }

        return ThemeManager.openImmediate(
            VirtualScreenType.HUD,
            true,
            browserSettings!!
        ).also { browser ->
            browserBrowser = browser
        }
    }

    private fun close() {
        browserBrowser?.let {
            it.close()
            browserBrowser = null
        }
    }

    fun reopen() {
        close()
        if (enabled && visible) {
            open()
        }
    }

}
