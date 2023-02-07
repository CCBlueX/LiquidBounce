/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2016 - 2023 CCBlueX
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
import com.labymedia.ultralight.gpu.UltralightOpenGLGPUDriverNative
import com.labymedia.ultralight.os.OperatingSystem
import com.labymedia.ultralight.plugin.logging.UltralightLogLevel
import net.ccbluex.liquidbounce.base.ultralight.filesystem.BrowserFileSystem
import net.ccbluex.liquidbounce.base.ultralight.glfw.GlfwClipboardAdapter
import net.ccbluex.liquidbounce.base.ultralight.glfw.GlfwCursorAdapter
import net.ccbluex.liquidbounce.base.ultralight.glfw.GlfwInputAdapter
import net.ccbluex.liquidbounce.base.ultralight.hooks.UltralightIntegrationHook
import net.ccbluex.liquidbounce.base.ultralight.hooks.UltralightScreenHook
import net.ccbluex.liquidbounce.base.ultralight.js.bindings.UltralightStorage
import net.ccbluex.liquidbounce.base.ultralight.renderer.CpuViewRenderer
import net.ccbluex.liquidbounce.utils.client.ThreadLock
import net.ccbluex.liquidbounce.utils.client.logger
import net.ccbluex.liquidbounce.utils.client.mc
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.util.math.MatrixStack

object UltralightEngine {

    /**
     * Ultralight window
     *
     * Might be useful in the future for external UI.
     */
    val window = mc.window.handle

    /**
     * Ultralight resources
     */
    val resources = UltralightResources()

    /**
     * Ultralight platform and renderer
     */
    var platform = ThreadLock<UltralightPlatform>()
    var renderer = ThreadLock<UltralightRenderer>()

    /**
     * Glfw
     */
    lateinit var gpuDriver: UltralightOpenGLGPUDriverNative

    lateinit var clipboardAdapter: GlfwClipboardAdapter
    lateinit var cursorAdapter: GlfwCursorAdapter
    lateinit var inputAdapter: GlfwInputAdapter

    /**
     * Views
     */
    val activeView: View?
        get() = views.find { it is ScreenView && mc.currentScreen == it.screen }

    private val views = mutableListOf<View>()

    /**
     * Initializes the platform
     */
    fun init() {
        val refreshRate = mc.window.refreshRate

        logger.info("Loading ultralight...")

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

        // Setup platform
        logger.debug("Setting up ultralight platform")
        platform.lock(UltralightPlatform.instance())
        platform.get().setConfig(
            UltralightConfig()
                .animationTimerDelay(1.0 / refreshRate)
                .scrollTimerDelay(1.0 / refreshRate)
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
                UltralightLogLevel.ERROR -> logger.debug("[Ultralight/ERR] $message")
                UltralightLogLevel.WARNING -> logger.debug("[Ultralight/WARN] $message")
                UltralightLogLevel.INFO -> logger.debug("[Ultralight/INFO] $message")
            }
        }

        // Setup renderer
        logger.info("Setting up ultralight renderer")
        val ulRenderer = UltralightRenderer.create()
        ulRenderer.logMemoryUsage();
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

    fun shutdown() {
        cursorAdapter.cleanup()
    }

    fun update() {
        views.forEach(View::update)
        renderer.get().update()
    }

    fun render(layer: RenderLayer, matrices: MatrixStack) {
        frameLimitedRender()

        views.filter { it.layer == layer }
            .forEach {
                it.render(matrices)
            }
    }

    // Maximum frame rate for the renderer
    private const val MAX_FRAME_RATE = 60
    private var lastRenderTime = 0.0
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
        views.forEach { it.resize(width, height) }
    }

    fun newSplashView() =
        View(RenderLayer.SPLASH_LAYER, newViewRenderer()).also { views += it }

    fun newOverlayView() =
        View(RenderLayer.OVERLAY_LAYER, newViewRenderer()).also { views += it }

    fun newScreenView(screen: Screen, adaptedScreen: Screen? = null, parentScreen: Screen? = null) =
        ScreenView(newViewRenderer(), screen, adaptedScreen, parentScreen).also { views += it }

    fun removeView(view: View) {
        view.free()
        views.remove(view)
    }

    private fun newViewRenderer() = CpuViewRenderer()

    /*private fun newViewRenderer() = when (RenderEngine.openglLevel) {
        OpenGLLevel.OpenGL1_2 -> CpuViewRenderer()
        OpenGLLevel.OpenGL3_3, OpenGLLevel.OpenGL4_3 ->
    }*/

}

enum class RenderLayer {
    OVERLAY_LAYER, SCREEN_LAYER, SPLASH_LAYER
}
