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
package net.ccbluex.liquidbounce.web.browser.supports

import com.mojang.blaze3d.systems.RenderSystem
import net.ccbluex.liquidbounce.config.ConfigSystem
import net.ccbluex.liquidbounce.event.Listenable
import net.ccbluex.liquidbounce.mcef.MCEF
import net.ccbluex.liquidbounce.mcef.MCEFDownloader
import net.ccbluex.liquidbounce.utils.client.ErrorHandler
import net.ccbluex.liquidbounce.utils.client.logger
import net.ccbluex.liquidbounce.utils.io.HttpClient
import net.ccbluex.liquidbounce.validation.ClientDataValidator
import net.ccbluex.liquidbounce.web.browser.BrowserType
import net.ccbluex.liquidbounce.web.browser.supports.tab.JcefTab
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
    private val tabs = mutableListOf<JcefTab>()

    override fun makeDependenciesAvailable(whenAvailable: () -> Unit) {
        if (!MCEF.isInitialized()) {
            ClientDataValidator.expectHashOrDelete(librariesFolder)

            MCEF.getSettings().apply {
                downloadMirror = "https://dl.liquidbounce.net/resources"
                // Uses a natural user agent to prevent websites from blocking the browser
                userAgent = HttpClient.DEFAULT_AGENT
            }

            val downloader = MCEFDownloader.newDownloader()

            if (downloader.requiresDownload(librariesFolder)) {
                thread(name = "mcef-downloader") {
                    runCatching {
                        downloader.downloadJcef(librariesFolder)
                        RenderSystem.recordRenderCall(whenAvailable)
                    }.onFailure(ErrorHandler::fatal)
                }
            } else {
                whenAvailable()
            }
        }
    }

    override fun initBrowserBackend() {
        if (!MCEF.isInitialized()) {
            MCEF.initialize()
        }
    }

    override fun shutdownBrowserBackend() {
        MCEF.shutdown()
    }

    override fun isInitialized() = MCEF.isInitialized()

    override fun createTab(url: String) = JcefTab(this, url) { false }.apply {
        synchronized(tabs) {
            tabs.add(this)
        }
    }

    override fun createInputAwareTab(url: String, takesInput: () -> Boolean) = JcefTab(this, url, takesInput).apply {
        synchronized(tabs) {
            tabs.add(this)
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
        if (MCEF.isInitialized()) {
            try {
                MCEF.getApp().handle.N_DoMessageLoopWork()
            } catch (e: Exception) {
                logger.error("Failed to draw browser globally", e)
            }
        }
    }

}
