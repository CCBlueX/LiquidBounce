/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2025 CCBlueX
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
import net.ccbluex.liquidbounce.api.core.HttpClient
import net.ccbluex.liquidbounce.config.ConfigSystem
import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.integration.browser.BrowserType
import net.ccbluex.liquidbounce.integration.browser.supports.tab.JcefTab
import net.ccbluex.liquidbounce.integration.browser.supports.tab.TabPosition
import net.ccbluex.liquidbounce.integration.task.MCEFProgressForwarder
import net.ccbluex.liquidbounce.integration.task.TaskManager
import net.ccbluex.liquidbounce.mcef.MCEF
import net.ccbluex.liquidbounce.utils.client.error.ErrorHandler
import net.ccbluex.liquidbounce.utils.client.error.QuickFix
import net.ccbluex.liquidbounce.utils.client.error.errors.JcefIsntCompatible
import net.ccbluex.liquidbounce.utils.client.formatAsCapacity
import net.ccbluex.liquidbounce.utils.client.logger
import net.ccbluex.liquidbounce.utils.kotlin.sortedInsert
import net.ccbluex.liquidbounce.utils.validation.HashValidator

/**
 * The time threshold for cleaning up old cache directories.
 */
private const val CACHE_CLEANUP_THRESHOLD = 1000 * 60 * 60 * 24 * 7 // 7 days

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
@Suppress("TooManyFunctions")
class JcefBrowser : IBrowser, EventListener {

    private val mcefFolder = ConfigSystem.rootFolder.resolve("mcef")
    private val librariesFolder = mcefFolder.resolve("libraries")
    private val cacheFolder = mcefFolder.resolve("cache")
    private val tabs = mutableListOf<JcefTab>()

    @Suppress("ThrowingExceptionsWithoutMessageOrCause")
    override fun makeDependenciesAvailable(taskManager: TaskManager, whenAvailable: () -> Unit) {
        // Clean up old cache directories
        cleanup()

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

            // Check if system is compatible with MCEF (JCEF)
            if (!resourceManager.isSystemCompatible) {
                throw JcefIsntCompatible
            }

            HashValidator.validateFolder(resourceManager.commitDirectory)

            if (resourceManager.requiresDownload()) {
                taskManager.launch("MCEF") { task ->
                    resourceManager.registerProgressListener(MCEFProgressForwarder(task))

                    runCatching {
                        resourceManager.downloadJcef()
                        RenderSystem.recordRenderCall(whenAvailable)
                    }.onFailure {
                        ErrorHandler.fatal(
                            error = it,
                            quickFix = QuickFix.DOWNLOAD_JCEF_FAILED,
                            additionalMessage = "Downloading jcef"
                        )
                    }
                }
            } else {
                whenAvailable()
            }
        }
    }

    /**
     * Cleans up old cache directories.
     *
     * TODO: Check if we have an active PID using the cache directory, if so, check if the LiquidBounce
     *   process attached to the JCEF PID is still running or not. If not, we could kill the JCEF process
     *   and clean up the cache directory.
     */
    fun cleanup() {
        if (cacheFolder.exists()) {
            runCatching {
                cacheFolder.listFiles()
                    ?.filter { file ->
                        file.isDirectory && System.currentTimeMillis() - file.lastModified() > CACHE_CLEANUP_THRESHOLD
                    }
                    ?.sumOf { file ->
                        try {
                            val fileSize = file.walkTopDown().sumOf { uFile -> uFile.length() }
                            file.deleteRecursively()
                            fileSize
                        } catch (e: Exception) {
                            logger.error("Failed to clean up old cache directory", e)
                            0
                        }
                    } ?: 0
            }.onFailure {
                // Not a big deal, not fatal.
                logger.error("Failed to clean up old JCEF cache directories", it)
            }.onSuccess { size ->
                if (size > 0) {
                    logger.info("Cleaned up ${size.formatAsCapacity()} JCEF cache directories")
                }
            }
        }
    }

    override fun startBrowser() {
        if (!MCEF.INSTANCE.isInitialized) {
            MCEF.INSTANCE.initialize()
        }
    }

    override fun stopBrowser() {
        MCEF.INSTANCE.shutdown()
        MCEF.INSTANCE.settings.cacheDirectory?.deleteRecursively()
    }

    override fun isInitialized() = MCEF.INSTANCE.isInitialized

    override fun createTab(url: String, position: TabPosition, frameRate: Int) =
        JcefTab(this, url, position, frameRate) { false }.apply(::addTab)

    override fun createInputAwareTab(url: String, position: TabPosition, frameRate: Int, takesInput: () -> Boolean) =
        JcefTab(this, url, position, frameRate, takesInput = takesInput).apply(::addTab)

    override fun getTabs(): List<JcefTab> = tabs

    private fun addTab(tab: JcefTab) {
        tabs.sortedInsert(tab, JcefTab::preferOnTop)
    }

    internal fun removeTab(tab: JcefTab) {
        tabs.remove(tab)
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
