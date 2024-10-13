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
import net.ccbluex.liquidbounce.web.theme.ThemeManager
import net.ccbluex.liquidbounce.web.theme.component.types.IntegratedComponent
import net.ccbluex.liquidbounce.web.theme.component.types.TextComponent

val components: MutableList<Component> = mutableListOf()
val customComponents: MutableList<Component> = mutableListOf(
    TextComponent("hello! :)", enabled = false)
)

object ComponentOverlay : Listenable {

    @JvmStatic
    fun isTweakEnabled(tweak: FeatureTweak) = handleEvents() && !HideAppearance.isHidingNow &&
        components.filterIsInstance<IntegratedComponent>().any { it.enabled && it.tweaks.contains(tweak) }

    @JvmStatic
    fun getComponentWithTweak(tweak: FeatureTweak): IntegratedComponent? {
        if (!handleEvents() || HideAppearance.isHidingNow) {
            return null
        }

        return components.filterIsInstance<IntegratedComponent>()
            .find { it.enabled && it.tweaks.contains(tweak) }
    }

    fun insertComponents() {
        val componentList = ThemeManager.activeTheme.parseComponents()

        // todo: fix custom components being removed
        components.clear()
        components += componentList

        logger.info("Inserted ${components.size} components")
    }

    fun fireComponentsUpdate() = EventManager.callEvent(ComponentsUpdate(components + customComponents))

    override fun parent() = ModuleHud

}
