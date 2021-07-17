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

package net.ccbluex.liquidbounce.render.ultralight.js.bindings

import net.ccbluex.liquidbounce.render.screen.EmptyScreen
import net.ccbluex.liquidbounce.render.ultralight.UltralightEngine
import net.ccbluex.liquidbounce.render.ultralight.theme.ThemeManager
import net.ccbluex.liquidbounce.utils.client.mc
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.gui.screen.TitleScreen
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen
import net.minecraft.client.gui.screen.option.LanguageOptionsScreen
import net.minecraft.client.gui.screen.option.OptionsScreen
import net.minecraft.client.gui.screen.world.SelectWorldScreen
import net.minecraft.client.realms.gui.screen.RealmsMainScreen

/**
 * Referenced by JS as `ui`
 */
object UltralightJsUi {

    // A collection of minecraft screens
    private val _jsScreens = arrayOf(
        JsScreen("title", TitleScreen::class.java) { mc.setScreen(TitleScreen()) },
        JsScreen("singleplayer", SelectWorldScreen::class.java) { mc.setScreen(SelectWorldScreen(it)) },
        JsScreen("multiplayer", MultiplayerScreen::class.java) { mc.setScreen(MultiplayerScreen(it)) },
        JsScreen("options", OptionsScreen::class.java) { mc.setScreen(OptionsScreen(it, mc.options)) },
        JsScreen("language_options", LanguageOptionsScreen::class.java) { mc.setScreen(LanguageOptionsScreen(it, mc.options, mc.languageManager)) },
        JsScreen("multiplayer_realms", RealmsMainScreen::class.java) { mc.setScreen(RealmsMainScreen(it)) }
    )

    fun get(name: String) = _jsScreens.find { it.name == name }
        ?: JsScreen("ultralight", EmptyScreen::class.java) {
            val page = ThemeManager.page(name) ?: error("unknown custom page")
            val emptyScreen = EmptyScreen()
            UltralightEngine.newScreenView(emptyScreen, mc.currentScreen).apply {
                loadPage(page)
            }
            mc.setScreen(emptyScreen)
        }

    fun get(screen: Screen?) = get(screen?.javaClass)

    fun get(clazz: Class<*>?) = _jsScreens.find { it.clazz == clazz }

    fun open(name: String, parent: Screen?) {
        get(name).open(parent)
    }

}

/**
 * A wrapper to make opening screens easier
 */
class JsScreen(val name: String, val clazz: Class<*>, private val execOpen: (Screen?) -> Unit) {
    fun open(parent: Screen?) = execOpen(parent)
}
