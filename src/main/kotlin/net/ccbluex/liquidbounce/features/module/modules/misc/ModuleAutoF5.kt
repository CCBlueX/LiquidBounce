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


package net.ccbluex.liquidbounce.features.module.modules.misc

import net.ccbluex.liquidbounce.config.types.NamedChoice
import net.ccbluex.liquidbounce.event.events.PerspectiveEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.ModuleKillAura
import net.ccbluex.liquidbounce.features.module.modules.player.cheststealer.features.FeatureSilentScreen
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.ModuleScaffold
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen
import net.minecraft.client.gui.screen.ingame.InventoryScreen
import net.minecraft.client.option.Perspective

/**
 * Automatically goes into F5 mode when opening the inventory
 */
object ModuleAutoF5 : ClientModule("AutoF5", Category.MISC) {

    private val openOn by multiEnumChoice<OpenOn>("OpenOn")

    @Suppress("unused")
    private val perspectiveHandler = handler<PerspectiveEvent> { event ->
        val screen = mc.currentScreen

        if (!FeatureSilentScreen.shouldHide &&
            (
                (OpenOn.CHEST in openOn && screen is GenericContainerScreen)
                    || (OpenOn.INVENTORY_SCREEN in openOn && screen is InventoryScreen)
                    || (OpenOn.SCAFFOLD in openOn && ModuleScaffold.enabled)
                    || (OpenOn.KILL_AURA in openOn && ModuleKillAura.targetTracker.target != null)
                )
        ) {
            event.perspective = Perspective.THIRD_PERSON_BACK
        }
    }

    private enum class OpenOn(override val choiceName: String) : NamedChoice {
        INVENTORY_SCREEN("InventoryScreen"),
        CHEST("Chest"),
        SCAFFOLD("Scaffold"),
        KILL_AURA("KillAura")
    }

}
