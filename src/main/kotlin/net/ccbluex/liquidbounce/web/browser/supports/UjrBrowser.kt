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
 *
 */

package net.ccbluex.liquidbounce.web.browser.supports

import net.ccbluex.liquidbounce.event.Listenable
import net.ccbluex.liquidbounce.utils.client.logger
import net.ccbluex.liquidbounce.web.browser.BrowserType
import net.ccbluex.liquidbounce.web.browser.supports.tab.UjrTab
import net.janrupf.ujr.api.UltralightConfigBuilder
import net.janrupf.ujr.api.UltralightPlatform
import net.janrupf.ujr.api.UltralightRenderer
import net.janrupf.ujr.core.UltralightJavaReborn
import net.janrupf.ujr.core.platform.PlatformEnvironment
import net.janrupf.ujr.example.glfw.bridge.FilesystemBridge
import net.janrupf.ujr.example.glfw.bridge.GlfwClipboardBridge
import net.janrupf.ujr.example.glfw.bridge.LoggerBridge
import net.janrupf.ujr.example.glfw.surface.GlfwSurfaceFactory
import java.io.File

/**
 * Uses Ultralight Java Reborn as browser backend.
 * This browser backend is based on WebKit.
 *
 * @see <a href="https://ultralig.ht/">Ultralight</a>
 * @see <a href="https://github.com/Janrupf/ultralight-java-reborn">Ultralight Java Reborn</a>
 *
 * @author 1zuna <marco@ccbluex.net>
 */
class UjrBrowser : IBrowser, Listenable {

    private lateinit var ujr: UltralightJavaReborn
    private val tabs = mutableListOf<UjrTab>()

    override fun makeDependenciesAvailable(whenAvailable: () -> Unit) {
        // not needed, the dependencies are already bundled in the jar
        whenAvailable()
    }

    override fun initBrowserBackend() {
        // Load the platform and create the Ultralight Java Reborn instance
        val environment = PlatformEnvironment.load()
        logger.info("Platform environment has been loaded!")
        ujr = UltralightJavaReborn(environment)
        ujr.activate()
        logger.info("Ultralight Java Reborn has been activated!")

        // Activate global bridge instances for Ultralight
        val platform = UltralightPlatform.instance()
        platform.usePlatformFontLoader()
        platform.filesystem = FilesystemBridge()
        platform.clipboard = GlfwClipboardBridge()
        platform.logger = LoggerBridge()

        // Note the usage of the GlfwSurfaceFactory here.
        // This is required to make Ultralight Java Reborn work custom surfaces.
        // Technically, we could also use the default surface factory, but that would
        // require us to deal with bitmaps and then upload them to OpenGLES.
        // Using a custom surface factory allows us to directly use pixel buffer objects.
        platform.setSurfaceFactory(GlfwSurfaceFactory())

        //TODO: change
        platform.setConfig(
            UltralightConfigBuilder().cachePath(System.getProperty("java.io.tmpdir")
                + File.separator + "ujr-example-glfw")
                .resourcePathPrefix(FilesystemBridge.RESOURCE_PREFIX)
                .build()
        )
    }

    override fun shutdownBrowserBackend() {
        ujr.cleanup()
    }

    override fun isInitialized() = UltralightJavaReborn.getActiveInstance().isOnCorrectThread

    override fun createTab(url: String, frameRate: Int) = UjrTab(this, url) { false }.apply {
        synchronized(tabs) {
            tabs.add(this)
        }
    }

    override fun createInputAwareTab(url: String, frameRate: Int, takesInput: () -> Boolean) =
        UjrTab(this, url, takesInput).apply {
            synchronized(tabs) {
                tabs.add(this)
            }
        }

    override fun getTabs() = tabs

    internal fun removeTab(tab: UjrTab) {
        synchronized(tabs) {
            tabs.remove(tab)
        }
    }

    override fun getBrowserType() = BrowserType.ULTRALIGHT
    override fun drawGlobally() {
        val renderer = UltralightRenderer.getOrCreate()
        renderer.update()
        renderer.render()
    }

}
