package net.ccbluex.liquidbounce.integration.backend.browser

import com.mojang.blaze3d.systems.RenderSystem
import net.ccbluex.liquidbounce.config.types.Configurable
import net.ccbluex.liquidbounce.config.types.Value
import net.ccbluex.liquidbounce.integration.backend.BrowserBackendManager
import net.ccbluex.liquidbounce.integration.backend.BrowserBackendManager.browserBackend
import net.ccbluex.liquidbounce.utils.client.logger
import net.ccbluex.liquidbounce.utils.render.refreshRate
import kotlin.math.max

/**
 * Settings for the browser renderer.
 */
class BrowserSettings(
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
    val quality by float("Quality", 1f, 0.5f..1f)

    init {
        if (browserBackend.isAccelerationSupported) {
            accelerated = boolean("Accelerated", true)
        }

        for (value in this.inner) {
            value.onChanged {
                update()
            }
        }
    }

    private fun update() = RenderSystem.recordRenderCall {
        for (browser in browserBackend.browsers) {
            try {
                browser.viewport = browser.viewport
            } catch (e: Exception) {
                logger.error("Failed to update tab of '${browser.url}'", e)
            }
        }
    }

}
