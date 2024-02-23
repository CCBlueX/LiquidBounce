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

import net.ccbluex.liquidbounce.config.Configurable
import net.ccbluex.liquidbounce.config.Value
import net.ccbluex.liquidbounce.event.EventManager
import net.ccbluex.liquidbounce.event.Listenable
import net.ccbluex.liquidbounce.event.events.ComponentsUpdate
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleHud
import net.ccbluex.liquidbounce.web.theme.component.types.IntegratedComponent
import net.ccbluex.liquidbounce.web.theme.component.types.MinimapComponent

// todo: serialize with metadata.json
var components: MutableList<Component> = mutableListOf(
    // TextComponent() demo
    //HtmlComponent() demo
    IntegratedComponent("Watermark"),
    IntegratedComponent("TabGui"),
    IntegratedComponent("ArrayList"),
    IntegratedComponent("Notifications"),
//    IntegratedComponent("Hotbar", tweaks = arrayOf(
//        FeatureTweak.TWEAK_HOTBAR,
//        FeatureTweak.DISABLE_STATUS_BAR,
//        FeatureTweak.DISABLE_EXP_BAR,
//        FeatureTweak.DISABLE_HELD_ITEM_TOOL_TIP
//    )),
//    IntegratedComponent("Scoreboard", tweaks = arrayOf(
//        FeatureTweak.DISABLE_SCOREBOARD
//    )),
    MinimapComponent(),
)

object ComponentOverlay : Configurable("Components", components as MutableList<Value<*>>), Listenable {

    @JvmStatic
    fun isTweakEnabled(tweak: FeatureTweak) = handleEvents() && components.filterIsInstance<IntegratedComponent>()
        .any { it.enabled && it.tweaks.contains(tweak) }

    fun defaultComponents() {

    }

    fun updateComponents() = EventManager.callEvent(ComponentsUpdate(components))

    override fun parent() = ModuleHud

}
