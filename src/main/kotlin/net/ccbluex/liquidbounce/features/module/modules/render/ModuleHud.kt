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
package net.ccbluex.liquidbounce.features.module.modules.render

import net.ccbluex.liquidbounce.config.ConfigSystem
import net.ccbluex.liquidbounce.config.types.group.ToggleableValueGroup
import net.ccbluex.liquidbounce.config.types.group.ValueGroup
import net.ccbluex.liquidbounce.event.EventManager
import net.ccbluex.liquidbounce.event.events.BrowserReadyEvent
import net.ccbluex.liquidbounce.event.events.DisconnectEvent
import net.ccbluex.liquidbounce.event.events.ScreenEvent
import net.ccbluex.liquidbounce.event.events.SpaceSeperatedNamesChangeEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.misc.HideAppearance.isDestructed
import net.ccbluex.liquidbounce.features.misc.HideAppearance.isHidingNow
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleHud.themes
import net.ccbluex.liquidbounce.integration.backend.browser.BrowserSettings
import net.ccbluex.liquidbounce.integration.screen.CustomScreenType
import net.ccbluex.liquidbounce.integration.screen.impl.CustomOverlay
import net.ccbluex.liquidbounce.integration.theme.ThemeManager
import net.ccbluex.liquidbounce.integration.theme.component.components.minimap.MinimapHudComponent
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.inGame
import net.ccbluex.liquidbounce.utils.client.markAsError
import net.minecraft.client.gui.screens.DisconnectedScreen
import net.minecraft.client.gui.screens.LevelLoadingScreen

/**
 * Module HUD
 *
 * The client in-game dashboard.
 */

object ModuleHud : ClientModule("HUD", ModuleCategories.RENDER, state = true, hide = true) {

    override val running
        get() = this.enabled && !isDestructed
    override val baseKey: String
        get() = "${ConfigSystem.KEY_PREFIX}.module.hud"

    private val isVisible: Boolean
        get() = !isHidingNow && inGame

    private var overlay = CustomOverlay(
        screenType = CustomScreenType.HUD,
        browserSettings = BrowserSettings(60, ::reopen)
    )

    init {
        tree(Blur)
    }

    object Blur : ToggleableValueGroup(ModuleHud, "Blur", enabled = true) {
        /**
         * The range in which the blending from not-blurred to blurred occurs.
         */
        val alphaBlendRange by floatRange("AlphaBlendRange", 0.0F..0.75F, 0.0F..1.0F)
    }

    @Suppress("unused")
    private val spaceSeperatedNames by boolean("SpaceSeperatedNames", true).onChange { state ->
        EventManager.callEvent(SpaceSeperatedNamesChangeEvent(state))
        state
    }

    val isBlurEffectActive
        get() = Blur.enabled && !(mc.options.hideGui && mc.screen == null)

    val themes = tree(ValueGroup("Themes"))

    val components = tree(ValueGroup("AdditionalComponents")).apply {
        tree(MinimapHudComponent)
    }

    /**
     * Updates [themes] content
     */
    fun updateThemes() {
        // filterIsInstance then forEach to prevent ConcurrentModificationException
        themes.inner.filterIsInstance<ValueGroup>().forEach {
            themes.drop(it)
        }
        for (theme in ThemeManager.themes) {
            themes.tree(theme.settings)
        }
        themes.walkInit()
        themes.walkKeyPath()
    }

    override fun onEnabled() {
        if (isHidingNow) {
            chat(markAsError(message("hidingAppearance")))
        }

        if (isVisible) {
            overlay.open()
        }
    }

    override fun onDisabled() {
        overlay.close()
    }

    @Suppress("unused")
    private val browserReadyHandler = handler<BrowserReadyEvent> { event ->
        tree(overlay.browserSettings)
    }

    @Suppress("unused")
    private val screenHandler = handler<ScreenEvent> { event ->
        // Close the tab when the HUD is not running, is hiding now, or the player is not in-game
        if (!enabled || !isVisible) {
            overlay.close()
            return@handler
        }

        // Otherwise, open the tab and set its visibility
        overlay.visible = event.screen !is DisconnectedScreen && event.screen !is LevelLoadingScreen
    }

    @Suppress("unused")
    private val disconnectHandler = handler<DisconnectEvent> {
        overlay.close()
    }

    fun reopen() {
        overlay.close()
        if (enabled && isVisible) {
            overlay.open()
        }
    }

}
