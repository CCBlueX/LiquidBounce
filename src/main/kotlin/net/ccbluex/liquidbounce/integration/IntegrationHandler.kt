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
package net.ccbluex.liquidbounce.integration

import com.mojang.blaze3d.systems.RenderSystem
import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.event.Listenable
import net.ccbluex.liquidbounce.event.events.*
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.misc.HideAppearance
import net.ccbluex.liquidbounce.mcef.progress.MCEFProgressMenu
import net.ccbluex.liquidbounce.utils.client.logger
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.integration.theme.ThemeManager.route
import net.ccbluex.liquidbounce.integration.theme.type.RouteType
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.gui.screen.TitleScreen
import org.lwjgl.glfw.GLFW

object IntegrationHandler : Listenable {

    private lateinit var ref: DrawerReference

    val route: RouteType
        get() = when (val ref = ref) {
            is DrawerReference.Native -> ref.route
            is DrawerReference.Web -> ref.route
        }

    private var browserIsReady = false

    internal val parent: Screen
        get() = mc.currentScreen ?: TitleScreen()

    /**
     * GLFW cursor for the standard cursor
     */
    private val standardCursor = GLFW.glfwCreateStandardCursor(GLFW.GLFW_ARROW_CURSOR)

    @Suppress("unused")
    val handleBrowserReady = handler<BrowserReadyEvent> {
        logger.info("Browser is ready.")

        // Reference will be either a NativeDrawer or a WebDrawer depending on what the user has selected
        ref = DrawerReference.newInputRef(route(null))
        browserIsReady = true
    }

    @Suppress("unused")
    fun virtualOpen(name: String) {
        val type = VirtualScreenType.byName(name) ?: return
        virtualOpen(route(type))
    }

    fun virtualOpen(route: RouteType) {
        apply(route)
    }

    fun virtualClose() {
        apply(route(null))
    }

    /**
     * Apply a new route to the drawer reference
     */
    private fun apply(route: RouteType) {
        // If the reference is already on the same route, we don't need to update it
        if (ref.matches(route)) {
            return
        }

        if (ref.isCompatible(route)) {
            // If the reference is compatible with the route, we can update it
            ref.update(route)
        } else {
            // Otherwise, we close the reference and create a new one
            ref.close()
            ref = DrawerReference.newInputRef(route)
        }
    }

    /**
     * Sync the drawer reference. This might fix desyncs.
     */
    fun sync() {
        logger.info("Reloading integration browser ${ref.javaClass.simpleName}")
        ref.sync()
    }

    /**
     * Restore the original screen if the current screen is a virtual display screen
     */
    fun restoreOriginal() {
        if (mc.currentScreen is VirtualDisplayScreen) {
            mc.setScreen((mc.currentScreen as VirtualDisplayScreen).original)
        }
    }

    /**
     * Handle opening new screens
     */
    @Suppress("unused")
    private val screenHandler = handler<ScreenEvent> { event ->
        // Set to default GLFW cursor
        GLFW.glfwSetCursor(mc.window.handle, standardCursor)

        if (handleScreenSituation(event.screen)) {
            event.cancelEvent()
        }
    }

    // TODO: Can we remove this? It should usually be handled by ScreenEvent already
    @Suppress("unused")
    private val screenRefresher = handler<GameTickEvent> {

        if (browserIsReady && mc.currentScreen !is MCEFProgressMenu) {
            handleScreenSituation(mc.currentScreen)
        }
    }

    /**
     * Refresh integration browser when we change worlds, this can also mean we disconnect from a server
     * and go back to the main menu.
     */
    @Suppress("unused")
    private val worldChangeEvent = handler<WorldChangeEvent> {
        sync()
    }

    private fun handleScreenSituation(screen: Screen?): Boolean {
        if (screen !is VirtualDisplayScreen && HideAppearance.isHidingNow) {
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

        if (screen is VirtualDisplayScreen) {
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

        val route = runCatching {
            route(virtualScreenType)
        }.getOrNull()

        if (route == null) {
            virtualClose()
            return false
        }

        val theme = route.theme

        if (theme.doesSupport(virtualScreenType)) {
            val virtualDisplayScreen = VirtualDisplayScreen(route, original = screen)
            mc.setScreen(virtualDisplayScreen)
            return true
        } else if (theme.doesOverlay(virtualScreenType)) {
            virtualOpen(route)
        } else {
            virtualClose()
        }

        return false
    }

}
