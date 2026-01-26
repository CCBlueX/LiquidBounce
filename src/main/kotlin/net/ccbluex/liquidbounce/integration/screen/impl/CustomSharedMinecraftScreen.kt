/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2026 CCBlueX
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

package net.ccbluex.liquidbounce.integration.screen.impl

import net.ccbluex.liquidbounce.integration.screen.CustomScreenType
import net.ccbluex.liquidbounce.integration.screen.ScreenManager
import net.ccbluex.liquidbounce.integration.theme.Theme
import net.ccbluex.liquidbounce.integration.theme.ThemeManager
import net.ccbluex.liquidbounce.utils.client.asPlainText
import net.ccbluex.liquidbounce.utils.client.mc
import net.minecraft.client.gui.screens.Screen

class CustomSharedMinecraftScreen(
    val screenType: CustomScreenType,
    private val theme: Theme = ThemeManager.getScreenLocation(screenType).theme,
    val originalScreen: Screen? = null,
    val parentScreen: Screen? = mc.screen
) : Screen("VS-${screenType.routeName.uppercase()}".asPlainText()) {

    override fun init() {
        ScreenManager.openScreen(theme, screenType)
    }

    override fun onClose() {
        if (parentScreen is CustomSharedMinecraftScreen) {
            mc.setScreen(parentScreen)
        } else {
            ScreenManager.closeScreen()
            mc.mouseHandler.grabMouse()
            super.onClose()
        }
    }

    override fun isPauseScreen() = false

}
