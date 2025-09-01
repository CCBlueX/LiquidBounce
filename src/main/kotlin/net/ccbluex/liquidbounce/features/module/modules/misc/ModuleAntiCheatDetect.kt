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

import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.utils.client.ServerObserver
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.regular
import net.ccbluex.liquidbounce.utils.client.variable

/**
 * Module Anti Cheat Detect
 *
 * Attempts to detect the anti-cheat used by the server.
 */
object ModuleAntiCheatDetect : ClientModule("AntiCheatDetect", Category.MISC) {

    init {
        doNotIncludeAlways()
    }

    override fun onEnabled() {
        alertAboutAntiCheat()
        super.onEnabled()
    }

    /**
     * Called by [ServerObserver] when enough transactions have been received.
     */
    fun completed() {
        if (enabled) {
            alertAboutAntiCheat()
        }
    }

    private fun alertAboutAntiCheat() {
        val antiCheat = ServerObserver.guessAntiCheat(mc.currentServerEntry?.address) ?: return
        chat(regular(message("detected", variable(antiCheat))))
    }

}
