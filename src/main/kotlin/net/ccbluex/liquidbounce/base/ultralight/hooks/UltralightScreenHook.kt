/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2023 CCBlueX
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
package net.ccbluex.liquidbounce.base.ultralight.hooks

import com.cinemamod.mcef.MCEF
import net.ccbluex.liquidbounce.base.ultralight.ScreenViewOverlay
import net.ccbluex.liquidbounce.base.ultralight.UltralightEngine
import net.ccbluex.liquidbounce.base.ultralight.js.bindings.UltralightJsPages
import net.ccbluex.liquidbounce.base.ultralight.theme.ThemeManager
import net.ccbluex.liquidbounce.event.Listenable
import net.ccbluex.liquidbounce.event.ScreenEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.misc.HideClient
import net.ccbluex.liquidbounce.features.module.modules.misc.ModuleHideClient
import net.ccbluex.liquidbounce.mcef.BasicBrowser
import net.ccbluex.liquidbounce.render.screen.EmptyScreen
import net.ccbluex.liquidbounce.utils.client.mc
import net.minecraft.client.gui.screen.TitleScreen
import net.minecraft.text.Text

object UltralightScreenHook : Listenable {

    /**
     * Handle opening new screens
     */
    val screenHandler = handler<ScreenEvent> { event ->
        UltralightEngine.cursorAdapter.unfocus()

        val activeView = UltralightEngine.inputAwareOverlay
        if (activeView is ScreenViewOverlay) {
            if (activeView.context.events._fireViewClose()) {
                UltralightEngine.removeView(activeView)
            }
        }

        if (HideClient.isHidingNow || ModuleHideClient.enabled) {
            return@handler
        }

        if (!MCEF.isInitialized()) {
            MCEF.initialize()
        }

        val screen = event.screen
        if (screen is TitleScreen) {
            println("opening basic browser")
            mc.setScreen(BasicBrowser(Text.literal("Browser")))
            println("Screen: $screen")
            event.cancelEvent()
        }

    }

}
