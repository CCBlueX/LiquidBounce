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

import net.ccbluex.liquidbounce.event.EventManager
import net.ccbluex.liquidbounce.event.events.ScreenEvent
import net.ccbluex.liquidbounce.event.events.SpaceSeperatedNamesChangeEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.misc.HideAppearance.isHidingNow
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.block.ChunkScanner
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.inGame
import net.ccbluex.liquidbounce.utils.client.markAsError
import net.ccbluex.liquidbounce.integration.theme.component.ComponentOverlay
import net.ccbluex.liquidbounce.integration.theme.type.native.components.minimap.ChunkRenderer
import net.minecraft.client.gui.screen.DisconnectedScreen

/**
 * Module HUD
 *
 * The client in-game dashboard.
 */

object ModuleHud : Module("HUD", Category.RENDER, state = true, hide = true) {

    override val translationBaseKey: String
        get() = "liquidbounce.module.hud"

    private val blur by boolean("Blur", true)
    @Suppress("unused")
    private val spaceSeperatedNames by boolean("SpaceSeperatedNames", true).onChange {
        EventManager.callEvent(SpaceSeperatedNamesChangeEvent(it))

        it
    }

    val isBlurable
        get() = blur && !(mc.options.hudHidden && mc.currentScreen == null)

    init {
        ChunkRenderer
    }

    val screenHandler = handler<ScreenEvent>(ignoreCondition = true) { event ->
        if (!enabled || !inGame || event.screen is DisconnectedScreen || isHidingNow) {
            ComponentOverlay.hide()
        } else {
            ComponentOverlay.show()
        }
    }

    fun refresh() {
        // Should not happen, but in-case there is already a tab open, close it
        ComponentOverlay.hide()

        // Create a new tab and open it
        ComponentOverlay.show()
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
        ComponentOverlay.hide()

        // Minimap
        ChunkScanner.unsubscribe(ChunkRenderer.MinimapChunkUpdateSubscriber)
        // todo: fix that unloading it and re-enabling breaks it
//        ChunkRenderer.unloadEverything()
    }

}
