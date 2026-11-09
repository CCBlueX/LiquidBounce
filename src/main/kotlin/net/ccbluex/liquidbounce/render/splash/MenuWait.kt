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
package net.ccbluex.liquidbounce.render.splash

import net.ccbluex.liquidbounce.integration.screen.ScreenManager
import net.ccbluex.liquidbounce.integration.screen.impl.CustomSharedMinecraftScreen
import net.ccbluex.liquidbounce.utils.client.mc
import net.minecraft.util.Util

/**
 * Holds the splash until the menu behind it can be seen: a theme screen's page has to open the screen first,
 * or the splash fades into an empty page.
 */
class MenuWait {

    private val start = Util.getMillis()
    private var readySince = -1L

    /**
     * True once the menu has shown for a moment, or after a timeout for pages that never answer.
     */
    fun isOver(): Boolean {
        val now = Util.getMillis()
        if (now - start >= TIMEOUT) {
            return true
        }

        val ready = mc.gui.screen() !is CustomSharedMinecraftScreen || ScreenManager.screenAcknowledgement.confirmed
        if (!ready) {
            readySince = -1L
            return false
        }

        if (readySince == -1L) {
            readySince = now
        }
        return now - readySince >= SETTLE
    }

    private companion object {
        /** The page draws its first frames after it opened the screen. */
        const val SETTLE = 250L
        const val TIMEOUT = 6000L
    }

}
