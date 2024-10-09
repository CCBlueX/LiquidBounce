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

package net.ccbluex.liquidbounce.integration.theme.component

import net.ccbluex.liquidbounce.event.EventManager
import net.ccbluex.liquidbounce.event.Listenable
import net.ccbluex.liquidbounce.event.events.ComponentsUpdate
import net.ccbluex.liquidbounce.features.misc.HideAppearance
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleHud
import net.ccbluex.liquidbounce.utils.client.logger
import net.ccbluex.liquidbounce.integration.DrawerReference
import net.ccbluex.liquidbounce.integration.VirtualScreenType
import net.ccbluex.liquidbounce.integration.theme.ThemeManager.activeComponents
import net.ccbluex.liquidbounce.integration.theme.type.Theme

object ComponentOverlay : Listenable {

    private val drawerReferenceMap = mutableMapOf<Theme, DrawerReference>()

    fun show() {
        if (drawerReferenceMap.isNotEmpty()) {
            return
        }

        activeComponents.forEach { component ->
            val theme = component.theme

            if (!theme.doesAccept(VirtualScreenType.HUD)) {
                logger.warn("${component.name} is not compatible with the ${theme.name} theme")
                return@forEach
            }

            // Check if the web overlay is already open
            if (drawerReferenceMap.containsKey(theme)) {
                return@forEach
            }

            val route = theme.route(VirtualScreenType.HUD)
            drawerReferenceMap[theme] = DrawerReference.newComponentRef(route)
        }
    }

    fun hide() {
        if (drawerReferenceMap.isEmpty()) {
            return
        }

        drawerReferenceMap.forEach { (_, ref) -> ref.close() }
        drawerReferenceMap.clear()
    }

    @JvmStatic
    fun isTweakEnabled(tweak: ComponentTweak) = handleEvents() && !HideAppearance.isHidingNow &&
        activeComponents.any { it.enabled && it.tweaks.contains(tweak) }

    @JvmStatic
    fun getComponentWithTweak(tweak: ComponentTweak): Component? {
        if (!handleEvents() || HideAppearance.isHidingNow) {
            return null
        }

        return activeComponents.find { it.enabled && it.tweaks.contains(tweak) }
    }

    fun fireComponentsUpdate() = EventManager.callEvent(ComponentsUpdate(activeComponents))

    override fun parent() = ModuleHud

}
