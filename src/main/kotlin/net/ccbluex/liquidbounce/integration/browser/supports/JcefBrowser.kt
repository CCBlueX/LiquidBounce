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
package net.ccbluex.liquidbounce.integration.browser.supports

import com.mojang.blaze3d.systems.RenderSystem
import net.ccbluex.liquidbounce.config.ConfigSystem
import net.ccbluex.liquidbounce.event.Listenable
import net.ccbluex.liquidbounce.mcef.MCEF
import net.ccbluex.liquidbounce.utils.client.ErrorHandler
import net.ccbluex.liquidbounce.utils.client.logger
import net.ccbluex.liquidbounce.utils.io.HttpClient
import net.ccbluex.liquidbounce.utils.validation.HashValidator
import net.ccbluex.liquidbounce.integration.browser.BrowserType
import net.ccbluex.liquidbounce.integration.browser.supports.tab.JcefTab
import net.ccbluex.liquidbounce.integration.browser.supports.tab.TabPosition
import kotlin.concurrent.thread

/**
 * Uses a modified fork of the JCEF library browser backend made for Minecraft.
 * This browser backend is based on Chromium and is the most advanced browser backend.
 * JCEF is available through the MCEF library, which provides a Minecraft compatible version of JCEF.
 *
 * @see <a href="https://github.com/CCBlueX/java-cef/">JCEF</a>
 * @see <a href="https://github.com/CCBlueX/mcef/">MCEF</a>
 *
 * @author 1zuna <marco@ccbluex.net>
 */
class JcefBrowser : IBrowser, Listenable {

    private val mcefFolder = ConfigSystem.rootFolder.resolve("mcef")
    private val librariesFolder = mcefFolder.resolve("libraries")
    private val cacheFolder = mcefFolder.resolve("cache")
    private val tabs = mutableListOf<JcefTab>()

    override fun makeDependenciesAvailable(whenAvailable: () -> Unit) {
        if (!MCEF.INSTANCE.isInitialized) {
            MCEF.INSTANCE.settings.apply {
                // Uses a natural user agent to prevent websites from blocking the browser
                userAgent = HttpClient.DEFAULT_AGENT
                cacheDirectory = cacheFolder.resolve(System.currentTimeMillis().toString(16)).apply {
                    deleteOnExit()
                }
                librariesDirectory = librariesFolder
            }

            val resourceManager = MCEF.INSTANCE.newResourceManager()
            HashValidator.validateFolder(resourceManager.commitDirectory)

            if (resourceManager.requiresDownload()) {
                thread(name = "mcef-downloader") {
                    runCatching {
                        resourceManager.downloadJcef()
                        RenderSystem.recordRenderCall(whenAvailable)
                    }.onFailure(ErrorHandler::fatal)
                }
            } else {
                whenAvailable()
            }
        }
    }

    override fun initBrowserBackend() {
        if (!MCEF.INSTANCE.isInitialized) {
            MCEF.INSTANCE.initialize()
        }
    }

    override fun shutdownBrowserBackend() {
        MCEF.INSTANCE.shutdown()
        MCEF.INSTANCE.settings.cacheDirectory?.deleteRecursively()
    }

    override fun isInitialized() = MCEF.INSTANCE.isInitialized

    override fun createTab(url: String, position: TabPosition, frameRate: Int, takesInput: () -> Boolean) =
        JcefTab(this, url, position, frameRate, takesInput = takesInput).apply {
            synchronized(tabs) {
                tabs += this

                // Sort tabs by preferOnTop
                tabs.sortBy { it.preferOnTop }
            }
        }

    override fun getTabs() = tabs

    internal fun removeTab(tab: JcefTab) {
        synchronized(tabs) {
            tabs.remove(tab)
        }
    }

    override fun getBrowserType() = BrowserType.JCEF
    override fun drawGlobally() {
        if (MCEF.INSTANCE.isInitialized) {
            try {
                MCEF.INSTANCE.app.handle.N_DoMessageLoopWork()
            } catch (e: Exception) {
                logger.error("Failed to draw browser globally", e)
            }
        }
    }

}
