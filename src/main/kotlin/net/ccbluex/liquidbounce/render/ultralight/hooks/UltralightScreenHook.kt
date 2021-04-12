/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2016 - 2021 CCBlueX
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
package net.ccbluex.liquidbounce.render.ultralight.hooks

import net.ccbluex.liquidbounce.event.GameTickEvent
import net.ccbluex.liquidbounce.event.Listenable
import net.ccbluex.liquidbounce.event.ScreenEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.render.screen.EmptyScreen
import net.ccbluex.liquidbounce.render.ultralight.UltralightEngine
import net.ccbluex.liquidbounce.render.ultralight.js.bindings.QueuedScreen
import net.ccbluex.liquidbounce.render.ultralight.js.bindings.UltralightJsUi
import net.ccbluex.liquidbounce.render.ultralight.theme.ThemeManager
import net.ccbluex.liquidbounce.utils.mc

object UltralightScreenHook : Listenable {

    var nextScreen: QueuedScreen? = null

    /**
     * Check queue every game tick
     */
    val tickHandler = handler<GameTickEvent> { event ->
        val (screen, parent) = nextScreen ?: return@handler
        // Making it null before opening is very important to make sure it doesn't repeat any further
        nextScreen = null

        // Open screen with parent
        screen.open(parent)
    }

    val screenHandler = handler<ScreenEvent> { event ->
        val name = UltralightJsUi.get(event.screen)?.name ?: return@handler
        val page = ThemeManager.page(name) ?: return@handler

        val emptyScreen = EmptyScreen()
        UltralightEngine.newScreenView(emptyScreen).apply {
            loadPage(page)
        }
        mc.openScreen(emptyScreen)
        event.cancelEvent()
    }

}
