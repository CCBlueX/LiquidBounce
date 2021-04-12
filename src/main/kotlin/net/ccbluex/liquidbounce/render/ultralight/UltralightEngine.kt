/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2016 - 2021 CCBlueX
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
package net.ccbluex.liquidbounce.render.ultralight

import com.labymedia.ultralight.UltralightJava
import com.labymedia.ultralight.UltralightPlatform
import com.labymedia.ultralight.UltralightRenderer
import com.labymedia.ultralight.config.FontHinting
import com.labymedia.ultralight.config.UltralightConfig
import com.labymedia.ultralight.plugin.logging.UltralightLogLevel
import net.ccbluex.liquidbounce.render.ultralight.glfw.GlfwClipboardAdapter
import net.ccbluex.liquidbounce.render.ultralight.glfw.GlfwCursorAdapter
import net.ccbluex.liquidbounce.render.ultralight.glfw.GlfwInputAdapter
import net.ccbluex.liquidbounce.render.ultralight.hooks.UltralightIntegrationHook
import net.ccbluex.liquidbounce.render.ultralight.hooks.UltralightScreenHook
import net.ccbluex.liquidbounce.render.ultralight.renderer.CpuViewRenderer
import net.ccbluex.liquidbounce.render.ultralight.theme.ThemeManager
import net.ccbluex.liquidbounce.utils.SingleThreadTaskScheduler
import net.ccbluex.liquidbounce.utils.logger
import net.ccbluex.liquidbounce.utils.mc
import net.minecraft.client.gui.screen.Screen

object UltralightEngine {

    val window = mc.window.handle

    /**
     * This is the thread that has also write access to the Ultralight instance
     */
    val contextThread = SingleThreadTaskScheduler()

    /**
     * Ultralight resources
     */
    private val resources = UltralightResources()

    /**
     * Ultralight platform and renderer
     */
    lateinit var platform: UltralightPlatform
    lateinit var renderer: UltralightRenderer

    /**
     * Glfw
     */
    lateinit var clipboardAdapter: GlfwClipboardAdapter
    lateinit var cursorAdapter: GlfwCursorAdapter
    lateinit var inputAdapter: GlfwInputAdapter

    /**
     * Views
     */
    val activeView: View?
        get() = screenViews.find { mc.currentScreen == it.screen }

    private val screenViews = mutableListOf<ScreenView>()
    private val overlayViews = mutableListOf<View>()

    /**
     * Initializes the platform
     */
    fun init() {
        val refreshRate = mc.window.refreshRate

        contextThread.scheduleBlocking {
            logger.info("Loading ultralight...")

            // Check resources
            logger.info("Checking resources...")
            resources.downloadResources()

            // Load natives from native directory inside root folder
            logger.debug("Loading ultralight natives")
            UltralightJava.load(resources.resourcesRoot.toPath())

            // Setup platform
            logger.debug("Setting up ultralight platform")
            platform = UltralightPlatform.instance()
            platform.setConfig(
                UltralightConfig()
                    .animationTimerDelay(1.0 / refreshRate)
                    .scrollTimerDelay(1.0 / refreshRate)
                    .resourcePath(resources.resourcesRoot.absolutePath)
                    .cachePath(resources.cacheRoot.absolutePath)
                    .fontHinting(FontHinting.SMOOTH)
            )
            platform.usePlatformFontLoader()
            platform.usePlatformFileSystem(ThemeManager.themesFolder.absolutePath)
            platform.setClipboard(GlfwClipboardAdapter())
            platform.setLogger { level, message ->
                @Suppress("WHEN_ENUM_CAN_BE_NULL_IN_JAVA")
                when (level) {
                    UltralightLogLevel.ERROR -> logger.debug("[Ultralight/ERR] $message")
                    UltralightLogLevel.WARNING -> logger.debug("[Ultralight/WARN] $message")
                    UltralightLogLevel.INFO -> logger.debug("[Ultralight/INFO] $message")
                }
            }

            // Setup renderer
            logger.debug("Setting up ultralight renderer")
            renderer = UltralightRenderer.create()

            // Setup hooks
            UltralightIntegrationHook
            UltralightScreenHook

            logger.info("Successfully loaded ultralight!")
        }

        // Setup GLFW adapters
        clipboardAdapter = GlfwClipboardAdapter()
        cursorAdapter = GlfwCursorAdapter()
        inputAdapter = GlfwInputAdapter()
    }

    fun shutdown() {
        cursorAdapter.cleanup()
    }

    fun update() {
        contextThread.scheduleBlocking {
            screenViews.removeIf { !it.active }

            overlayViews.forEach(View::update)
            screenViews.forEach(View::update)
            renderer.update()
        }
    }

    fun render(layer: RenderLayer) {
        // todo: probably not working for gpu driver, think about something better.
        contextThread.scheduleBlocking {
            renderer.render()
        }

        when(layer) {
            RenderLayer.SCREEN_LAYER -> screenViews.forEach(View::render)
            RenderLayer.OVERLAY_LAYER -> overlayViews.forEach(View::render)
        }
    }

    fun resize(width: Long, height: Long) {
        screenViews.forEach { it.resize(width, height) }
        overlayViews.forEach { it.resize(width, height) }
    }

    fun newOverlayView()
        = View(renderer, newViewRenderer()).also { overlayViews += it }

    fun newScreenView(screen: Screen)
        = ScreenView(renderer, newViewRenderer(), screen).also { screenViews += it }

    private fun newViewRenderer() = CpuViewRenderer()

    /*private fun newViewRenderer() = when (RenderEngine.openglLevel) {
        OpenGLLevel.OpenGL1_2 -> CpuViewRenderer()
        OpenGLLevel.OpenGL3_3, OpenGLLevel.OpenGL4_3 -> GpuViewRenderer()
    }*/

}

enum class RenderLayer {
    OVERLAY_LAYER, SCREEN_LAYER
}
