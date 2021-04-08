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

package net.ccbluex.liquidbounce.render.ultralight.bindings

import net.ccbluex.liquidbounce.render.ultralight.UltralightScreen
import net.ccbluex.liquidbounce.render.ultralight.UltralightScreenHook
import net.ccbluex.liquidbounce.render.ultralight.theme.ThemeManager
import net.ccbluex.liquidbounce.utils.mc
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.gui.screen.TitleScreen
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen
import net.minecraft.client.gui.screen.world.SelectWorldScreen

/**
 * A utility to access minecraft screens from ultralight views
 */
object UltralightJsUi {

    // A collection of minecraft screens
    private val screens = arrayOf(
        JsScreen("title", TitleScreen::class.java) { mc.openScreen(TitleScreen()) },
        JsScreen("singleplayer", SelectWorldScreen::class.java) { mc.openScreen(SelectWorldScreen(it)) },
        JsScreen("multiplayer", MultiplayerScreen::class.java) { mc.openScreen(MultiplayerScreen(it)) }
    )

    fun get(name: String) = screens.find { it.name == name }
        ?: JsScreen("ultralight", UltralightScreen::class.java) { mc.openScreen(UltralightScreen(ThemeManager.page(name) ?: error("unknown page"))) }


    fun get(screen: Screen?) = get(screen?.javaClass)

    fun get(clazz: Class<*>?) = screens.find { it.clazz == clazz }

    fun open(name: String, parent: Screen?) {
        UltralightScreenHook.nextScreen = QueuedScreen(get(name), parent)
    }

}

/**
 * A wrapper to make opening screens easier
 */
class JsScreen(val name: String, val clazz: Class<*>, private val execOpen: (Screen?) -> Unit) {
    fun open(parent: Screen?) = execOpen(parent)
}

/**
 * Queue opening screens to prevent freezing the game by locking the UI thread
 */
data class QueuedScreen(val jsScreen: JsScreen, val parent: Screen?)
