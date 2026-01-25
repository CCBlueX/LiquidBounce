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

package net.ccbluex.liquidbounce.integration.theme.component

import net.ccbluex.liquidbounce.event.EventManager
import net.ccbluex.liquidbounce.event.events.ComponentsUpdateEvent
import net.ccbluex.liquidbounce.features.misc.HideAppearance
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleHud
import net.ccbluex.liquidbounce.integration.theme.ThemeManager
import net.ccbluex.liquidbounce.integration.theme.component.components.minimap.MinimapHudComponent

object HudComponentManager {

    val nativeComponents = listOf(MinimapHudComponent)

    val components: List<HudComponent>
        get() = nativeComponents + (ThemeManager.theme?.components ?: emptyList())

    @JvmStatic
    fun isTweakEnabled(tweak: HudComponentTweak) = ModuleHud.running && !HideAppearance.isHidingNow &&
        components.any { component ->
            component.enabled && component.tweaks.contains(tweak)
        }

    @JvmStatic
    fun getComponentWithTweak(tweak: HudComponentTweak): HudComponent? {
        if (!ModuleHud.running || HideAppearance.isHidingNow) {
            return null
        }

        return components.find { component ->
            component.enabled && component.tweaks.contains(tweak)
        }
    }

    fun getComponents(id: String?): List<HudComponent> {
        if (id == null) {
            return components
        }

        val theme = ThemeManager.themes.find { it.metadata.id == id } ?: return emptyList()
        return theme.components
    }

    fun updateComponents() {
        // Might be necessary later on.
        // EventManager.callEvent(ComponentsUpdate(null, components))
        val theme = ThemeManager.theme ?: return
        EventManager.callEvent(ComponentsUpdateEvent(theme.metadata.id, theme.components))
    }

}
