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
package net.ccbluex.liquidbounce.features.misc

import net.ccbluex.liquidbounce.event.EventManager
import net.ccbluex.liquidbounce.event.EventManager.callEvent
import net.ccbluex.liquidbounce.event.Listenable
import net.ccbluex.liquidbounce.event.events.ClientShutdownEvent
import net.ccbluex.liquidbounce.event.events.KeyboardKeyEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.ModuleManager
import net.ccbluex.liquidbounce.utils.client.Chronometer
import net.ccbluex.liquidbounce.utils.client.inGame
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.web.integration.IntegrationHandler
import net.minecraft.SharedConstants
import net.minecraft.client.util.Icons
import org.lwjgl.glfw.GLFW

/**
 * Hides client appearance
 *
 * using 2x CRTL + SHIFT to hide and unhide the client
 */
object HideAppearance : Listenable {

    val shiftChronometer = Chronometer()

    var isHidingNow = false
        set(value) {
            field = value
            updateClient()
        }
    var isDestructed = false

    private fun updateClient() {
        if (isHidingNow) {
            IntegrationHandler.restoreOriginalScreen()
        } else {
            IntegrationHandler.updateIntegrationBrowser()
        }
    }

    @Suppress("unused")
    val keyHandler = handler<KeyboardKeyEvent>(ignoreCondition = true) {
        val keyCode = it.keyCode
        val modifier = it.mods

        if (inGame) {
            return@handler
        }

        if (keyCode == GLFW.GLFW_KEY_LEFT_SHIFT && modifier == GLFW.GLFW_MOD_CONTROL) {
            if (!shiftChronometer.hasElapsed(400L)) {
                isHidingNow = !isHidingNow
            }

            shiftChronometer.reset()
        }
    }

    /**
     * Attempt to destruct the client
     */
    fun destructClient() {
        isHidingNow = true
        isDestructed = true

        mc.updateWindowTitle()
        mc.window.setIcon(
            mc.defaultResourcePack,
            if (SharedConstants.getGameVersion().isStable) Icons.RELEASE else Icons.SNAPSHOT)

        callEvent(ClientShutdownEvent())
        EventManager.unregisterAll()

        // Disable all modules
        // Be careful to not trigger ConfigManager saving, but this should be prevented by [isDestructed]
        // and unregistering all events
        for (module in ModuleManager) {
            module.enabled = false
        }
        ModuleManager.clear()

        // History clear
        mc.inGameHud.chatHud.clear(true)

        // Clear log folder
        mc.runDirectory.resolve("logs").listFiles()?.forEach {
            runCatching {
                it.delete()
            }.onFailure {
                // We do not want to log anything here
            }
        }
    }

}
