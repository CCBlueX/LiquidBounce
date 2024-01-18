/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2024 CCBlueX
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

import net.ccbluex.liquidbounce.event.events.KeyboardKeyEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.misc.HideClient
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.client.Chronometer
import net.ccbluex.liquidbounce.utils.client.inGame
import net.ccbluex.liquidbounce.web.integration.IntegrationHandler
import org.lwjgl.glfw.GLFW

/**
 * Hides client
 */
object ModuleHideClient : Module("HideClient", Category.MISC) {

    val shiftChronometer = Chronometer()

    override fun enable() {
        HideClient.isHidingNow = true

        // This will cause a refresh of the current screen
        IntegrationHandler.restoreOriginalScreen()
        super.enable()
    }

    override fun disable() {
        HideClient.isHidingNow = false

        // This will cause a refresh of the current screen
        if (mc.currentScreen != null) {
            mc.setScreen(mc.currentScreen)
        }
        super.disable()
    }

    val keyHandler = handler<KeyboardKeyEvent>(ignoreCondition = true) {
        val keyCode = it.keyCode
        val modifier = it.mods

        if (inGame) {
            return@handler
        }

        if (keyCode == GLFW.GLFW_KEY_LEFT_SHIFT && modifier == GLFW.GLFW_MOD_CONTROL) {
            if (!shiftChronometer.hasElapsed(400L)) {
                enabled = !enabled

                // Since we are not in a game, we have to manually enable/disable the module
                if (enabled) {
                    enable()
                } else {
                    disable()
                    IntegrationHandler.updateIntegrationBrowser()
                }
            }

            shiftChronometer.reset()
        }
    }

}
