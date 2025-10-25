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
package net.ccbluex.liquidbounce.features.module.modules.render

import net.ccbluex.liquidbounce.event.events.PerspectiveEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.utils.inventory.CheckScreenHandlerTypeConfigurable
import net.ccbluex.liquidbounce.utils.inventory.CheckScreenTitleConfigurable
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.client.option.Perspective

/**
 * Automatically goes into F5 mode when opening the inventory
 */
object ModuleAutoF5 : ClientModule("AutoF5", Category.RENDER) {

    private val checkScreenHandlerType = tree(CheckScreenHandlerTypeConfigurable(this))
    private val checkScreenTitle = tree(CheckScreenTitleConfigurable(this))

    @Suppress("unused")
    private val perspectiveHandler = handler<PerspectiveEvent> { event ->
        val screen = mc.currentScreen

        if (screen is HandledScreen<*> && checkScreenHandlerType.isValid(screen) && checkScreenTitle.isValid(screen)) {
            event.perspective = Perspective.THIRD_PERSON_BACK
        }
    }

}
