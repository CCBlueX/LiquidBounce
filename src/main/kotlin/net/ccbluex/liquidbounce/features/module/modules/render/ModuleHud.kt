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
 */
package net.ccbluex.liquidbounce.features.module.modules.render

import net.ccbluex.liquidbounce.config.Configurable
import net.ccbluex.liquidbounce.config.Value
import net.ccbluex.liquidbounce.event.EventManager
import net.ccbluex.liquidbounce.event.events.*
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.misc.HideAppearance.isHidingNow
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.block.ChunkScanner
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.inGame
import net.ccbluex.liquidbounce.utils.client.markAsError
import net.ccbluex.liquidbounce.web.browser.supports.tab.ITab
import net.ccbluex.liquidbounce.web.integration.VirtualScreenType
import net.ccbluex.liquidbounce.web.theme.ThemeManager
import net.ccbluex.liquidbounce.web.theme.component.components
import net.ccbluex.liquidbounce.web.theme.component.customComponents
import net.ccbluex.liquidbounce.web.theme.component.types.minimap.ChunkRenderer
import net.minecraft.client.gui.screen.DisconnectedScreen

/**
 * Module HUD
 *
 * The client in-game dashboard.
 */

object ModuleHud : Module("HUD", Category.RENDER, state = true, hide = true) {

    private var browserTab: ITab? = null

    override val translationBaseKey: String
        get() = "liquidbounce.module.hud"

    private val blur by boolean("Blur", true)

    @Suppress("unused")
    private val spaceSeperatedNames by boolean("SpaceSeperatedNames", true).onChange {
        EventManager.callEvent(SpaceSeperatedNamesChangeEvent(it))

        it
    }

    @Suppress("unused")
    val tickHandler = handler<PlayerTickEvent> {
        EventManager.callEvent(PlayerArmorInventory(player.inventory.armor))
        EventManager.callEvent(PlayerMainInventory(player.inventory.main))
    }

    val isBlurable
        get() = blur && !(mc.options.hudHidden && mc.currentScreen == null)

    init {
        tree(Configurable("In-built", components as MutableList<Value<*>>))
        tree(Configurable("Custom", customComponents as MutableList<Value<*>>))
    }

    val screenHandler = handler<ScreenEvent>(ignoreCondition = true) {
        if (!enabled || !inGame || it.screen is DisconnectedScreen || isHidingNow) {
            browserTab?.closeTab()
            browserTab = null
        } else if (browserTab == null) {
            browserTab = ThemeManager.openImmediate(VirtualScreenType.HUD, true)
        }
    }

    fun refresh() {
        // Should not happen, but in-case there is already a tab open, close it
        browserTab?.closeTab()

        // Create a new tab and open it
        browserTab = ThemeManager.openImmediate(VirtualScreenType.HUD, true)
    }

    override fun enable() {
        if (isHidingNow) {
            chat(markAsError(message("hidingAppearance")))
        }

        refresh()

        // Minimap
        ChunkScanner.subscribe(ChunkRenderer.MinimapChunkUpdateSubscriber)
    }

    override fun disable() {
        // Closes tab entirely
        browserTab?.closeTab()
        browserTab = null

        // Minimap
        ChunkScanner.unsubscribe(ChunkRenderer.MinimapChunkUpdateSubscriber)
        ChunkRenderer.unloadEverything()
    }

}
