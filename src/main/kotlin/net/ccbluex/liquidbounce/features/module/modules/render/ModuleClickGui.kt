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

import com.mojang.blaze3d.systems.RenderSystem
import net.ccbluex.liquidbounce.config.types.nesting.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.EventManager
import net.ccbluex.liquidbounce.event.events.*
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.sequenceHandler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.integration.IntegrationListener
import net.ccbluex.liquidbounce.integration.VirtualDisplayScreen
import net.ccbluex.liquidbounce.integration.VirtualScreenType
import net.ccbluex.liquidbounce.integration.backend.browser.Browser
import net.ccbluex.liquidbounce.integration.interop.protocol.rest.v1.game.isTyping
import net.ccbluex.liquidbounce.integration.theme.ThemeManager
import net.ccbluex.liquidbounce.utils.client.asText
import net.ccbluex.liquidbounce.utils.client.inGame
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention.OBJECTION_AGAINST_EVERYTHING
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention.READ_FINAL_STATE
import net.minecraft.client.gui.screen.Screen
import org.lwjgl.glfw.GLFW

/**
 * ClickGUI module
 *
 * Shows you an easy-to-use menu to toggle and configure modules.
 */

object ModuleClickGui :
    ClientModule("ClickGUI", Category.RENDER, bind = GLFW.GLFW_KEY_RIGHT_SHIFT, disableActivation = true) {

    override val running get() = true

    @Suppress("UnusedPrivateProperty")
    private val scale by float("Scale", 1f, 0.5f..2f).onChanged {
        EventManager.callEvent(ClickGuiScaleChangeEvent(it))
        EventManager.callEvent(ClickGuiValueChangeEvent(this))
    }

    @Suppress("UnusedPrivateProperty")
    private val cache by boolean("Cache", true).onChanged { cache ->
        RenderSystem.recordRenderCall {
            if (cache) {
                open()
            } else {
                close()
            }

            if (mc.currentScreen is VirtualDisplayScreen || mc.currentScreen is ClickScreen) {
                onEnabled()
            }
        }
    }

    @Suppress("UnusedPrivateProperty")
    private val searchBarAutoFocus by boolean("SearchBarAutoFocus", true).onChanged {
        EventManager.callEvent(ClickGuiValueChangeEvent(this))
    }

    val isInSearchBar: Boolean
        get() = (mc.currentScreen is VirtualDisplayScreen || mc.currentScreen is ClickScreen) && isTyping

    object Snapping : ToggleableConfigurable(this, "Snapping", true) {

        @Suppress("UnusedPrivateProperty")
        private val gridSize by int("GridSize", 10, 1..100, "px").onChanged {
            EventManager.callEvent(ClickGuiValueChangeEvent(ModuleClickGui))
        }

        init {
            inner.find { it.name == "Enabled" }?.onChanged {
                EventManager.callEvent(ClickGuiValueChangeEvent(ModuleClickGui))
            }
        }
    }

    private var clickGuiBrowser: Browser? = null
    private const val WORLD_CHANGE_SECONDS_UNTIL_RELOAD = 5

    init {
        tree(Snapping)
    }

    override fun onEnabled() {
        // Pretty sure we are not in a game, so we can't open the clickgui
        if (!inGame) {
            return
        }

        mc.setScreen(
            if (clickGuiBrowser == null) {
                VirtualDisplayScreen(VirtualScreenType.CLICK_GUI)
            } else {
                ClickScreen()
            }
        )
        super.onEnabled()
    }

    private fun open() {
        if (clickGuiBrowser != null) {
            return
        }

        clickGuiBrowser = ThemeManager.openInputAwareImmediate(
            VirtualScreenType.CLICK_GUI,
            true,
            priority = 20,
            settings = IntegrationListener.browserSettings
        ) {
            mc.currentScreen is ClickScreen
        }
    }

    private fun close() {
        clickGuiBrowser?.let {
            it.close()
            clickGuiBrowser = null
        }
    }

    fun reload(restart: Boolean = false) {
        if (restart) {
            close()
            open()
            return
        }

        clickGuiBrowser?.reload()
    }

    @Suppress("unused")
    private val gameRenderHandler = handler<GameRenderEvent>(priority = OBJECTION_AGAINST_EVERYTHING) {
        clickGuiBrowser?.visible = mc.currentScreen is ClickScreen
    }

    @Suppress("unused")
    private val browserReadyHandler = handler<BrowserReadyEvent>(priority = READ_FINAL_STATE) {
        tree(IntegrationListener.browserSettings)
        open()
    }

    @Suppress("unused")
    private val worldChangeHandler = sequenceHandler<WorldChangeEvent>(
        priority = OBJECTION_AGAINST_EVERYTHING
    ) { event ->
        if (event.world == null) {
            return@sequenceHandler
        }

        waitSeconds(WORLD_CHANGE_SECONDS_UNTIL_RELOAD)
        if (mc.currentScreen !is ClickScreen) {
            reload()
        }
    }

    @Suppress("unused")
    private val clientLanguageChangedHandler = handler<ClientLanguageChangedEvent> {
        if (mc.currentScreen !is ClickScreen) {
            reload()
        }
    }

    /**
     * An empty screen that acts as a hint when to draw the clickgui
     */
    class ClickScreen : Screen("ClickGUI".asText()) {

        override fun init() {
            super.init()
        }

        override fun close() {
            mc.mouse.lockCursor()
            super.close()
        }

        override fun shouldPause(): Boolean {
            // preventing game pause
            return false
        }
    }

}
