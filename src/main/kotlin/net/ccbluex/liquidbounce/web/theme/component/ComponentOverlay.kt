/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2024 CCBlueX
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
 *
 */

package net.ccbluex.liquidbounce.web.theme.component

import net.ccbluex.liquidbounce.event.EventManager
import net.ccbluex.liquidbounce.event.Listenable
import net.ccbluex.liquidbounce.event.events.ComponentsUpdate
import net.ccbluex.liquidbounce.features.misc.HideAppearance
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleHud
import net.ccbluex.liquidbounce.utils.client.logger
import net.ccbluex.liquidbounce.web.browser.BrowserManager
import net.ccbluex.liquidbounce.web.browser.supports.tab.ITab
import net.ccbluex.liquidbounce.web.integration.VirtualScreenType
import net.ccbluex.liquidbounce.web.theme.ThemeManager.route
import net.ccbluex.liquidbounce.web.theme.type.web.components.IntegratedComponent
import net.ccbluex.liquidbounce.web.theme.type.RouteType
import net.ccbluex.liquidbounce.web.theme.type.Theme

var components: MutableList<Component> = mutableListOf()

object ComponentOverlay : Listenable {

    private val webOverlayMap = mutableMapOf<Theme, ITab>()

    fun show() {
        if (webOverlayMap.isNotEmpty()) {
            return
        }

        val browser = BrowserManager.browser ?: error("Browser is not initialized")

        components.filterIsInstance<IntegratedComponent>().forEach { component ->
            val theme = component.theme

            if (!theme.doesAccept(VirtualScreenType.HUD)) {
                logger.warn("${component.name} is not compatible with the ${theme.name} theme")
                return@forEach
            }

            // Check if the web overlay is already open
            if (webOverlayMap.containsKey(theme)) {
                return@forEach
            }

            val route = route(VirtualScreenType.HUD)
            if (route is RouteType.Web) {
                webOverlayMap[theme] = browser.createTab(route.url, frameRate = 60)
            }
        }
    }

    fun hide() {
        if (webOverlayMap.isEmpty()) {
            return
        }

        webOverlayMap.forEach { (_, tab) -> tab.closeTab() }
    }

    @JvmStatic
    fun isTweakEnabled(tweak: ComponentTweak) = handleEvents() && !HideAppearance.isHidingNow &&
        components.filterIsInstance<IntegratedComponent>().any { it.enabled && it.tweaks.contains(tweak) }

    @JvmStatic
    fun getComponentWithTweak(tweak: ComponentTweak): IntegratedComponent? {
        if (!handleEvents() || HideAppearance.isHidingNow) {
            return null
        }

        return components.filterIsInstance<IntegratedComponent>()
            .find { it.enabled && it.tweaks.contains(tweak) }
    }

    fun fireComponentsUpdate() = EventManager.callEvent(ComponentsUpdate(components))

    override fun parent() = ModuleHud

}
