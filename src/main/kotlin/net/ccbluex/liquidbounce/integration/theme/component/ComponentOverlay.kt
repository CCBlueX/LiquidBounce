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
 *
 *
 */

package net.ccbluex.liquidbounce.integration.theme.component

import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.EventManager
import net.ccbluex.liquidbounce.event.events.ComponentsUpdate
import net.ccbluex.liquidbounce.features.misc.HideAppearance
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleHud
import net.ccbluex.liquidbounce.integration.theme.ThemeManager
import net.ccbluex.liquidbounce.integration.theme.component.types.IntegratedComponent
import net.ccbluex.liquidbounce.integration.theme.component.types.TextComponent
import net.ccbluex.liquidbounce.utils.client.logger
import net.ccbluex.liquidbounce.utils.render.Alignment
import net.ccbluex.liquidbounce.utils.render.Alignment.ScreenAxisX
import net.ccbluex.liquidbounce.utils.render.Alignment.ScreenAxisY

val components: MutableList<Component> = mutableListOf()
val customComponents: MutableList<Component> = mutableListOf(
    TextComponent(
        "Ping: {ping}, XYZ {position.x}, {position.y}, {position.z} |" +
            " Nether XZ: {netherPosition.x}, {netherPosition.z}",
        enabled = false,
        alignment = Alignment(ScreenAxisX.LEFT, 4, ScreenAxisY.BOTTOM, 22)
    )
)

object ComponentOverlay : EventListener {

    @JvmStatic
    fun isTweakEnabled(tweak: FeatureTweak) = this.running && !HideAppearance.isHidingNow &&
        components.filterIsInstance<IntegratedComponent>().any { it.enabled && it.tweaks.contains(tweak) }

    @JvmStatic
    fun getComponentWithTweak(tweak: FeatureTweak): IntegratedComponent? {
        if (!running || HideAppearance.isHidingNow) {
            return null
        }

        return components.filterIsInstance<IntegratedComponent>()
            .find { it.enabled && it.tweaks.contains(tweak) }
    }

    fun insertDefaultComponents() {
        val componentList = ThemeManager.activeTheme.parseComponents()

        // todo: fix custom components being removed
        components.clear()
        components += componentList

        logger.info("Inserted ${components.size} components")
    }

    fun fireComponentsUpdate() = EventManager.callEvent(ComponentsUpdate(components + customComponents))

    override fun parent() = ModuleHud

}
