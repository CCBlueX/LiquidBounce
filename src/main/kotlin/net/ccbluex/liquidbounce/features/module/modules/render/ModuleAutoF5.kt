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

package net.ccbluex.liquidbounce.features.module.modules.render

import net.ccbluex.liquidbounce.event.events.ScreenEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen
import net.minecraft.client.gui.screen.ingame.InventoryScreen
import net.minecraft.client.option.Perspective

/**
 * Automatically goes into F5 mode when opening the inventory
 */
object ModuleAutoF5 : Module("AutoF5", Category.RENDER) {

    var previousPerspective: Perspective? = null

    val screenHandler = handler<ScreenEvent> {
        val screen = it.screen

        if (screen is GenericContainerScreen || screen is InventoryScreen) {
            previousPerspective = mc.options.perspective
            mc.options.perspective = Perspective.THIRD_PERSON_BACK
        } else if (previousPerspective != null) {
            mc.options.perspective = previousPerspective
        }
    }

}
