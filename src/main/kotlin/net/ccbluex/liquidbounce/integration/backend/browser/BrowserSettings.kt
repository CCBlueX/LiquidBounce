package net.ccbluex.liquidbounce.integration.backend.browser

import com.mojang.blaze3d.systems.RenderSystem
import net.ccbluex.liquidbounce.config.types.Value
import net.ccbluex.liquidbounce.config.types.nesting.Configurable
import net.ccbluex.liquidbounce.integration.IntegrationListener
import net.ccbluex.liquidbounce.integration.backend.BrowserBackendManager
import net.ccbluex.liquidbounce.integration.backend.BrowserBackendManager.browserBackend
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.render.refreshRate
import kotlin.math.max

object GlobalBrowserSettings : Configurable("GlobalRenderer") {

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

    init {
        if (browserBackend.isAccelerationSupported) {
            accelerated = boolean("Accelerated(BETA)", false).onChanged {
                RenderSystem.recordRenderCall {
                    IntegrationListener.restart()
                    mc.updateWindowTitle()
                }
            }
        }
    }

}

class BrowserSettings(
    fpsLimit: Int = 0,
    update: () -> Unit
) : Configurable("Renderer") {

    /**
     * The maximum frames per second the browser renderer should run at.
     */
    val fps = int("Fps", fpsLimit, 0..max(0, refreshRate), "FPS").onChanged {
        RenderSystem.recordRenderCall {
            update()
        }
    }

    val syncGameFps by boolean("SyncGameFps", true)

    val currentFps: Int
        get() {
            val fpsValue = fps.get()
            return if (fpsValue <= 0) refreshRate else fpsValue
        }

}
