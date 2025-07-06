/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2025 CCBlueX
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
 *
 */
package net.ccbluex.liquidbounce.integration.browser

import net.ccbluex.liquidbounce.config.types.Configurable
import net.ccbluex.liquidbounce.config.types.Value
import net.ccbluex.liquidbounce.utils.render.refreshRate
import kotlin.math.max

/**
 * Settings for the browser renderer.
 */
class BrowserRendererSettings(
    // The maximum frames per second the browser renderer should run at.
    // When set to 0, it will run at the [refreshRate] of the monitor.
    fpsLimit: Int = 0
) : Configurable("Renderer") {

    /**
     * The maximum frames per second the browser renderer should run at.
     */
    val fps = int("Fps", fpsLimit, 0..max(0, refreshRate), "FPS")
    val currentFps: Int
        get() {
            val fpsValue = fps.get()
            return if (fpsValue <= 0) refreshRate else fpsValue
        }

    /**
     * Uses hardware acceleration for rendering the browser.
     */
    var accelerated: Value<Boolean>? = null
        private set

    /**
     * Quality setting that controls the rendering resolution.
     * 1.0 = full resolution, 0.5 = half-resolution (better performance), etc.
     */
    val quality = float("Quality", 0.5f, 0.5f..2f)

    init {
        if (BrowserManager.browser?.isAccelerationSupported == true) {
            accelerated = boolean("Accelerated", true)
        }
    }

}
