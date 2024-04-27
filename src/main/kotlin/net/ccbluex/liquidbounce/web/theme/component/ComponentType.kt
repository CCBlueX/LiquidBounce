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

import net.ccbluex.liquidbounce.config.NamedChoice
import net.ccbluex.liquidbounce.web.theme.component.types.IntegratedComponent
import net.ccbluex.liquidbounce.web.theme.component.types.minimap.MinimapComponent

enum class ComponentType(
    override val choiceName: String,
    val tweaks: Array<FeatureTweak> = emptyArray(),
    val createComponent: () -> Component = { IntegratedComponent(choiceName, tweaks) }
) : NamedChoice {

    WATERMARK("Watermark"),
    TAB_GUI("TabGui"),
    ARRAY_LIST("ArrayList"),
    NOTIFICATIONS("Notifications"),
    HOTBAR("Hotbar", tweaks = arrayOf(
        FeatureTweak.TWEAK_HOTBAR,
        FeatureTweak.DISABLE_STATUS_BAR,
        FeatureTweak.DISABLE_EXP_BAR,
        FeatureTweak.DISABLE_HELD_ITEM_TOOL_TIP,
        FeatureTweak.DISABLE_OVERLAY_MESSAGE
    )),
    SCOREBOARD("Scoreboard", tweaks = arrayOf(
        FeatureTweak.DISABLE_SCOREBOARD
    )),
    MINIMAP("Minimap", createComponent = { MinimapComponent }),
    TARGET_HUD("TargetHud"),
    TACO("Taco"),
    KEYSTROKES("Keystrokes");

    companion object {

        fun byName(name: String) = entries.find { it.choiceName == name }

    }

}
