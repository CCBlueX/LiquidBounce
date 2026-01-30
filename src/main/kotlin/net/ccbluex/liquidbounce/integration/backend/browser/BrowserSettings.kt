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

package net.ccbluex.liquidbounce.integration.backend.browser

import net.ccbluex.liquidbounce.config.types.Value
import net.ccbluex.liquidbounce.config.types.group.ValueGroup
import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.events.BrowserReadyEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.integration.backend.BrowserAccelerationFlags
import net.ccbluex.liquidbounce.integration.backend.BrowserBackendManager
import net.ccbluex.liquidbounce.integration.backend.BrowserBackendManager.backend
import net.ccbluex.liquidbounce.integration.screen.ScreenManager
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.render.refreshRate
import kotlin.math.max

object GlobalBrowserSettings : ValueGroup("GuiRenderer"), EventListener {

    /**
     * Quality setting that controls the rendering resolution.
     * 1.0 = full resolution, 0.5 = half-resolution (better performance), etc.
     *
     * Unfortunately, this is a global setting that applies to all browsers,
     * as CEF is not letting us set a custom zoom level per browser.
     */
    val quality by float("Quality", 1f, 0.5f..1f).onChanged {
        BrowserBackendManager.forceUpdate()
    }

    /**
     * Uses GPU acceleration for rendering the browser.
     */
    var accelerated: Value<Boolean>? = null
        private set

    @Suppress("unused")
    private val browserReadyHandler = handler<BrowserReadyEvent> { event ->
        val accelerationFlags = backend?.accelerationFlags ?: BrowserAccelerationFlags.UNSUPPORTED

        if (!BrowserBackendManager.disableAcceleration && accelerationFlags.isSupported) {
            accelerated = if (accelerationFlags.isBeta) {
                boolean("AcceleratedPaint(BETA)", false)
            } else {
                boolean("AcceleratedPaint", true)
            }.onChanged {
                mc.execute {
                    ScreenManager.restart()
                    mc.updateTitle()
                }
            }
        }
    }

}

open class BrowserSettings(
    fpsLimit: Int = 0,
    update: Runnable,
) : ValueGroup("Renderer") {

    /**
     * The maximum frames per second the browser renderer should run at.
     */
    val fps = int("Fps", fpsLimit, 0..max(0, refreshRate), "FPS").onChanged {
        mc.execute(update)
    }

    val currentFps: Int
        get() {
            val fpsValue = fps.get()
            return if (fpsValue <= 0) refreshRate else fpsValue
        }

}

class IntegrationBrowserSettings(
    fpsLimit: Int = 0,
    update: Runnable,
) : BrowserSettings(fpsLimit, update) {
    val syncGameFps by boolean("SyncGameFps", true)
}
