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

import net.ccbluex.liquidbounce.config.types.NamedChoice
import net.ccbluex.liquidbounce.integration.theme.component.types.EffectsComponent
import net.ccbluex.liquidbounce.integration.theme.component.types.HotBarComponent
import net.ccbluex.liquidbounce.integration.theme.component.types.IntegratedComponent
import net.ccbluex.liquidbounce.integration.theme.component.types.TargetHUDComponent
import net.ccbluex.liquidbounce.integration.theme.component.types.minimap.MinimapComponent

enum class ComponentType(
    override val choiceName: String,
    private val componentTweaks: Array<FeatureTweak> = emptyArray(),

    val createComponent: () -> Component = { IntegratedComponent(choiceName, componentTweaks) }
) : NamedChoice {

    WATERMARK("Watermark"),
    TAB_GUI("TabGui"),
    ARRAY_LIST("ArrayList"),
    LOGO("Logo"),
    KEY_BINDS("KeyBinds"),
    MOTION_GRAPH("MotionGraph"),
    SESSIONINFO("SessionInfo"),
    PROGRESS("ProgressBar"),
    PLAYERLIST(
        "PlayerListHUD",
        componentTweaks = arrayOf(
            FeatureTweak.DISABLE_PLAYERLIST_HUD,
        )
    ),

    CHAT_HUD(
        "ChatHUD", componentTweaks = arrayOf(
            FeatureTweak.DISABLE_CHAT_HUD,
        )
    ),

    ITEMCOLUMN_HUD(
        "ItemColumnHUD", componentTweaks = arrayOf(
            FeatureTweak.TWEAK_HOTBAR,
            FeatureTweak.DISABLE_EXP_BAR,
            FeatureTweak.DISABLE_HELD_ITEM_TOOL_TIP,
            FeatureTweak.DISABLE_OVERLAY_MESSAGE,
            FeatureTweak.DISABLE_ITEM_ICONS

        )
    ),
    HOTBAR(
        "HotBar",
        createComponent = { HotBarComponent(
            tweaks = arrayOf(
                FeatureTweak.TWEAK_HOTBAR,
                FeatureTweak.DISABLE_EXP_BAR,
                FeatureTweak.DISABLE_HELD_ITEM_TOOL_TIP,
                FeatureTweak.DISABLE_OVERLAY_MESSAGE,
                FeatureTweak.DISABLE_ITEM_ICONS
            )
        ) }
    ),



    TITLE_CONTROL(
        "TitleControl", componentTweaks = arrayOf(
            FeatureTweak.DISABLE_TITLE
        )
    ),
    HEALTH_BAR(
        "HealthBar", componentTweaks = arrayOf(
            FeatureTweak.DISABLE_STATUS_BAR,
            FeatureTweak.DISABLE_EXP_BAR,
            FeatureTweak.DISABLE_HELD_ITEM_TOOL_TIP,
            FeatureTweak.DISABLE_OVERLAY_MESSAGE
        )
    ),
    STATUS(
        "StatusBar", componentTweaks = arrayOf(
            FeatureTweak.DISABLE_HELD_ITEM_TOOL_TIP,
            FeatureTweak.DISABLE_STATUS_BAR,
            FeatureTweak.DISABLE_OVERLAY_MESSAGE

        )
    ),
    MESSAGE(
        "Message", componentTweaks = arrayOf(
            FeatureTweak.DISABLE_HELD_ITEM_TOOL_TIP,
            FeatureTweak.DISABLE_OVERLAY_MESSAGE

        )
    ),
    EFFECTS(
        "Effects",
        createComponent = { EffectsComponent(
                tweaks = arrayOf(
                    FeatureTweak.DISABLE_STATUS_EFFECT_OVERLAY
                )
            )
        }
    ),

    SCOREBOARD(
        "Scoreboard",
        componentTweaks = arrayOf(
            FeatureTweak.DISABLE_SCOREBOARD
        )
    ),
    MINIMAP(
        "Minimap",
        createComponent = { MinimapComponent }
    ),
    TARGET_HUD("TargetHUD",
        createComponent = { TargetHUDComponent()}
    ),

    BLOCK_COUNTER("BlockCounter"),
    ARMOR_ITEMS("ArmorItems"),
    INVENTORY("InventoryContainer"),
    INFORMATION("Information"),
    CRAFTING_INVENTORY("CraftingInput"),
    KEYSTROKES("Keystrokes"),
    ISLAND("Island"),
    NOTIFICATIONS("Notifications"),
    VIGNETTE("Vignette"),
    SILENT_HOTBAR("SilentHand"),
    ;

    companion object {
        fun byName(name: String) = entries.find { it.choiceName == name }
    }
}
