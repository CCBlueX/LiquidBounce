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
package net.ccbluex.liquidbounce.base.ultralight

import com.labymedia.ultralight.UltralightJava
import com.labymedia.ultralight.UltralightPlatform
import com.labymedia.ultralight.UltralightRenderer
import com.labymedia.ultralight.config.FontHinting
import com.labymedia.ultralight.config.UltralightConfig
import com.labymedia.ultralight.gpu.UltralightGPUDriverNativeUtil
import com.labymedia.ultralight.os.OperatingSystem
import com.labymedia.ultralight.plugin.logging.UltralightLogLevel
import net.ccbluex.liquidbounce.base.ultralight.hooks.UltralightIntegrationHook
import net.ccbluex.liquidbounce.base.ultralight.hooks.UltralightScreenHook
import net.ccbluex.liquidbounce.base.ultralight.impl.BrowserFileSystem
import net.ccbluex.liquidbounce.base.ultralight.impl.glfw.GlfwClipboardAdapter
import net.ccbluex.liquidbounce.base.ultralight.impl.glfw.GlfwCursorAdapter
import net.ccbluex.liquidbounce.base.ultralight.impl.glfw.GlfwInputAdapter
import net.ccbluex.liquidbounce.base.ultralight.impl.renderer.CpuViewRenderer
import net.ccbluex.liquidbounce.base.ultralight.js.bindings.UltralightStorage
import net.ccbluex.liquidbounce.utils.client.ThreadLock
import net.ccbluex.liquidbounce.utils.client.logger
import net.ccbluex.liquidbounce.utils.client.mc
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.Screen

object UltralightEngine {

    val window = mc.window.handle
    var platform = ThreadLock<UltralightPlatform>()
    var renderer = ThreadLock<UltralightRenderer>()

    lateinit var clipboardAdapter: GlfwClipboardAdapter
    lateinit var cursorAdapter: GlfwCursorAdapter
    lateinit var inputAdapter: GlfwInputAdapter

    val inputAwareOverlay: ViewOverlay?
        get() = viewOverlays.find { it is ScreenViewOverlay && mc.currentScreen == it.screen && it.state == ViewOverlayState.VISIBLE }
    private val viewOverlays = mutableListOf<ViewOverlay>()

    val resources = UltralightResources()

    /**
     * Frame limiter
     */
    private const val MAX_FRAME_RATE = 144
    private var lastRenderTime = 0.0

    /**
     * Initializes the platform
     */
    fun init() {
        logger.info("Loading ultralight...")
        initNatives()

        // Setup platform
        logger.debug("Setting up ultralight platform")
        platform.lock(UltralightPlatform.instance())
        platform.get().setConfig(
            UltralightConfig()
                .animationTimerDelay(1.0 / MAX_FRAME_RATE)
                .scrollTimerDelay(1.0 / MAX_FRAME_RATE)
                .cachePath(resources.cacheRoot.absolutePath)
                .fontHinting(FontHinting.SMOOTH)
                .forceRepaint(false)
        )
        platform.get().usePlatformFontLoader()
        platform.get().setFileSystem(BrowserFileSystem())
        platform.get().setClipboard(GlfwClipboardAdapter())
        platform.get().setLogger { level, message ->
            @Suppress("WHEN_ENUM_CAN_BE_NULL_IN_JAVA")
            when (level) {
                UltralightLogLevel.ERROR -> logger.error("[Ul] $message")
                UltralightLogLevel.WARNING -> logger.warn("[Ul] $message")
                UltralightLogLevel.INFO -> logger.info("[Ul] $message")
            }
        }

        // Setup renderer
        logger.info("Setting up ultralight renderer")

        val ulRenderer = UltralightRenderer.create()
        ulRenderer.logMemoryUsage()
        renderer.lock(ulRenderer)

        // Setup hooks
        UltralightIntegrationHook
        UltralightScreenHook

        UltralightStorage

        // Setup GLFW adapters
        clipboardAdapter = GlfwClipboardAdapter()
        cursorAdapter = GlfwCursorAdapter()
        inputAdapter = GlfwInputAdapter()

        logger.info("Successfully loaded ultralight!")
    }

    /**
     * Initializes the natives, this is required for ultralight to work.
     *
     * This will download the required natives and resources and load them.
     */
    private fun initNatives() {
        // Check resources
        logger.info("Checking resources...")
        resources.downloadResources()

        // Load natives from native directory inside root folder
        logger.info("Loading ultralight natives")
        val natives = resources.binRoot.toPath()
        logger.info("Native path: $natives")

        val libs = listOf(
            "glib-2.0-0",
            "gobject-2.0-0",
            "gmodule-2.0-0",
            "gio-2.0-0",
            "gstreamer-full-1.0",
            "gthread-2.0-0"
        )
        logger.debug("Libraries: $libs")

        val os = OperatingSystem.get()
        for (lib in libs) {
            logger.debug("Loading library $lib")
            System.load(natives.resolve(os.mapLibraryName(lib)).toAbsolutePath().toString())
        }

        logger.debug("Loading UltralightJava")
        UltralightJava.load(natives)
        logger.debug("Loading UltralightGPUDriver")
        UltralightGPUDriverNativeUtil.load(natives)
    }

    fun shutdown() {
        cursorAdapter.cleanup()
    }

    fun update() {
        viewOverlays
            .forEach(ViewOverlay::update)
        renderer.get().update()
    }

    fun render(layer: RenderLayer, context: DrawContext) {
        frameLimitedRender()

        viewOverlays
            .filter { it.layer == layer && it.state != ViewOverlayState.HIDDEN }
            .forEach {
                it.render(context)
            }
    }

    private fun frameLimitedRender() {
        val frameTime = 1.0 / MAX_FRAME_RATE
        val time = System.nanoTime() / 1e9
        val delta = time - lastRenderTime

        if (delta < frameTime) {
            return
        }

        renderer.get().render()
        lastRenderTime = time
    }

    fun resize(width: Long, height: Long) {
        viewOverlays.forEach { it.resize(width, height) }
    }

    fun newSplashView() =
        ViewOverlay(RenderLayer.SPLASH_LAYER, newViewRenderer()).also { viewOverlays += it }

    fun newOverlayView() =
        ViewOverlay(RenderLayer.OVERLAY_LAYER, newViewRenderer()).also { viewOverlays += it }

    fun newScreenView(screen: Screen, adaptedScreen: Screen? = null, parentScreen: Screen? = null) =
        ScreenViewOverlay(newViewRenderer(), screen, adaptedScreen, parentScreen).also { viewOverlays += it }

    /**
     * Removes the view overlay from the list of overlays
     */
    fun removeView(viewOverlay: ViewOverlay) {
        if (viewOverlay.context.events._fireViewClose()) {
            viewOverlay.free()
            viewOverlays.remove(viewOverlay)
        } else {
            // Wait for the view to close
            viewOverlay.setOnStageChange {
                if (it == ViewOverlayState.END) {
                    viewOverlay.free()
                    viewOverlays.remove(viewOverlay)
                }
            }
        }
    }

    /**
     * Creates a new view renderer
     */
    private fun newViewRenderer() = CpuViewRenderer()

}

enum class RenderLayer {
    OVERLAY_LAYER, SCREEN_LAYER, SPLASH_LAYER
}
