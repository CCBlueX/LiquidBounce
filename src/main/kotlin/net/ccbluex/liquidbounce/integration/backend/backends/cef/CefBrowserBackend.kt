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
package net.ccbluex.liquidbounce.integration.backend.backends.cef

import com.mojang.blaze3d.systems.RenderSystem
import net.ccbluex.liquidbounce.api.core.HttpClient
import net.ccbluex.liquidbounce.config.ConfigSystem
import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.integration.backend.BrowserBackend
import net.ccbluex.liquidbounce.integration.backend.browser.BrowserSettings
import net.ccbluex.liquidbounce.integration.backend.browser.BrowserViewport
import net.ccbluex.liquidbounce.integration.backend.input.InputAcceptor
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
import net.minecraft.util.Util
import org.lwjgl.opengl.GL
import org.lwjgl.opengl.GL11

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
 * @author Izuna <izuna.seikatsu@ccbluex.net>
 */
@Suppress("TooManyFunctions")
class CefBrowserBackend : BrowserBackend, EventListener {

    private val mcefFolder = ConfigSystem.rootFolder.resolve("mcef")
    private val librariesFolder = mcefFolder.resolve("libraries")
    private val cacheFolder = mcefFolder.resolve("cache")

    override val isInitialized: Boolean
        get() = MCEF.INSTANCE.isInitialized
    override var browsers = mutableListOf<CefBrowser>()
    override var isAccelerationSupported: Boolean = false

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

    override fun start() {
        if (!MCEF.INSTANCE.isInitialized) {
            MCEF.INSTANCE.initialize()
        }

        // Check if acceleration is supported
        val system = Util.getOperatingSystem()
        isAccelerationSupported = when (system) {
            Util.OperatingSystem.WINDOWS -> {
                // Check if required OpenGL extensions for D3D11 shared texture interop are supported
                checkAccelerationSupport()
            }
            else -> false
        }
    }

    override fun stop() {
        MCEF.INSTANCE.shutdown()
        MCEF.INSTANCE.settings.cacheDirectory?.deleteRecursively()
    }

    override fun update() {
        if (MCEF.INSTANCE.isInitialized) {
            try {
                MCEF.INSTANCE.app.handle.N_DoMessageLoopWork()
            } catch (e: Exception) {
                logger.error("Failed to draw browser globally", e)
            }
        }
    }

    override fun createBrowser(
        url: String,
        position: BrowserViewport,
        settings: BrowserSettings,
        priority: Short,
        inputAcceptor: InputAcceptor?
    ) = CefBrowser(this, url, position, settings, priority, inputAcceptor)
        .apply(::addBrowser)

    private fun addBrowser(browser: CefBrowser) {
        browsers.sortedInsert(browser, CefBrowser::priority)
    }

    internal fun removeBrowser(browser: CefBrowser) {
        browsers.remove(browser)
    }

    /**
     * Checks if the GPU supports the required OpenGL extensions for accelerated CEF rendering.
     * Currently only NVIDIA GPUs are known to work reliably with D3D11 shared texture interoperability.
     *
     * @return true if all required extensions are supported, false otherwise
     */
    private fun checkAccelerationSupport(): Boolean {
        return try {
            RenderSystem.assertOnRenderThread()

            val capabilities = GL.getCapabilities()
            val vendor = GL11.glGetString(GL11.GL_VENDOR) ?: ""
            val renderer = GL11.glGetString(GL11.GL_RENDERER) ?: ""

            logger.info("GPU Vendor: $vendor")
            logger.info("GPU Renderer: $renderer")

            // Check if the GPU is NVIDIA or AMD as
            // we could not get this feature to work reliably on Intel GPUs.
            // On Intel GPU (Intel ARC), it does not work as well and is reported:
            // https://github.com/IGCIT/Intel-GPU-Community-Issue-Tracker-IGCIT/issues/1143

            val isSupportedGpu =
                vendor.contains("nvidia", true) ||
                    renderer.contains("geforce", true) ||
                    renderer.contains("quadro", true) ||
                    vendor.contains("amd", true) ||
                    renderer.contains("radeon", true)
            if (!isSupportedGpu) {
                logger.warn("GPU acceleration only supported on NVIDIA and AMD GPUs")
                logger.info("Falling back to software rendering for browser")
                return false
            }

            // Required OpenGL extensions for D3D11 shared texture interoperability
            // See https://registry.khronos.org/OpenGL/extensions/EXT/EXT_external_objects_win32.txt
            val extensions = arrayOf(
                capabilities.GL_EXT_memory_object,
                capabilities.GL_EXT_memory_object_win32
            )

            logger.info("Checking OpenGL extensions for GPU acceleration" +
                " support: ${extensions.joinToString(", ")}")
            for (extension in extensions) {
                if (!extension) {
                    logger.warn("Required OpenGL extension for GPU acceleration not supported")
                    logger.info("Falling back to software rendering for browser")
                    return false
                }
            }

            true
        } catch (e: Exception) {
            logger.warn("Failed to check GPU acceleration support: ${e.message}")
            logger.info("Falling back to software rendering for browser")
            false
        }
    }
}
