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

import net.ccbluex.liquidbounce.config.types.group.ValueGroup
import net.ccbluex.liquidbounce.config.types.list.ChoiceListValue
import net.ccbluex.liquidbounce.config.types.list.Tagged
import net.ccbluex.liquidbounce.features.addon.AddonApi
import net.ccbluex.liquidbounce.integration.backend.BrowserBackendManager
import net.ccbluex.liquidbounce.integration.backend.BrowserBackendProvider
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.render.refreshRate
import kotlin.math.max

object GlobalBrowserSettings : ValueGroup("GuiRenderer") {

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
     * Applies on the next start.
     */
    lateinit var backendChoice: ChoiceListValue<Tagged>
        private set

    private object AskBackend : Tagged {
        override val tag = "Ask"
    }

    /**
     * Add-ons register their backends after this group is created, so the choice is built once they did.
     */
    internal fun createBackendChoice(backends: List<BrowserBackendProvider>) {
        backendChoice = enumChoice("Backend", AskBackend, linkedSetOf<Tagged>(AskBackend).apply { addAll(backends) })
    }

}

@AddonApi
open class BrowserSettings(
    fpsLimit: Int = 0,
    update: Runnable,
) : ValueGroup("Renderer") {

    /**
     * The maximum frames per second the browser renderer should run at.
     */
    val fps = int("Fps", fpsLimit, 0..max(0, refreshRate.toInt()), "FPS").onChanged {
        mc.execute(update)
    }

    val currentFps: Int
        get() {
            val fpsValue = fps.get()
            return if (fpsValue <= 0) refreshRate.toInt() else fpsValue
        }

}

class IntegrationBrowserSettings(
    fpsLimit: Int = 0,
    update: Runnable,
) : BrowserSettings(fpsLimit, update) {
    val syncGameFps by boolean("SyncGameFps", true)
}
