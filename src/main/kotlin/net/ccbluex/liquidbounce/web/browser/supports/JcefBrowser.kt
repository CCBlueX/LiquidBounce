package net.ccbluex.liquidbounce.web.browser.supports

import com.mojang.blaze3d.systems.RenderSystem
import net.ccbluex.liquidbounce.config.ConfigSystem
import net.ccbluex.liquidbounce.mcef.MCEF
import net.ccbluex.liquidbounce.utils.client.ErrorHandler
import net.ccbluex.liquidbounce.utils.client.logger
import net.ccbluex.liquidbounce.utils.io.HttpClient
import net.ccbluex.liquidbounce.utils.validation.HashValidator
import net.ccbluex.liquidbounce.web.browser.BrowserType
import net.ccbluex.liquidbounce.web.browser.supports.tab.JcefTab
import java.io.File
import kotlin.concurrent.thread

class JcefBrowser : IBrowser {

    private val mcefFolder: File = ConfigSystem.rootFolder.resolve("mcef")
    private val librariesFolder: File = mcefFolder.resolve("libraries")
    private val cacheFolder: File = mcefFolder.resolve("cache")
    private val tabs: MutableList<JcefTab> = mutableListOf()

    init {
        setupFolders()
    }

    private fun setupFolders() {
        try {
            mcefFolder.mkdirs()
            librariesFolder.mkdirs()
            cacheFolder.mkdirs()
        } catch (e: Exception) {
            logger.error("Failed to create MCEF folders", e)
            ErrorHandler.fatal("Failed to create MCEF folders")
        }
    }

    override fun initBrowserBackend() {
        if (!MCEF.INSTANCE.isInitialized) {
            initializeMCEF()
        }
    }

    private fun initializeMCEF() {
        try {
            MCEF.INSTANCE.settings.apply {
                userAgent = HttpClient.DEFAULT_AGENT
                cacheDirectory = cacheFolder.resolve(System.currentTimeMillis().toString(16)).apply {
                    deleteOnExit()
                }
                librariesDirectory = librariesFolder
            }

            val resourceManager = MCEF.INSTANCE.newResourceManager()
            HashValidator.validateFolder(resourceManager.commitDirectory)

            if (resourceManager.requiresDownload()) {
                downloadJcefResources(resourceManager)
            } else {
                MCEF.INSTANCE.initialize()
            }
        } catch (e: Exception) {
            logger.error("Failed to initialize MCEF", e)
            ErrorHandler.fatal("Failed to initialize MCEF")
        }
    }

    private fun downloadJcefResources(resourceManager: Any) {
        thread(name = "mcef-downloader") {
            try {
                resourceManager.downloadJcef()
                RenderSystem.recordRenderCall { MCEF.INSTANCE.initialize() }
            } catch (e: Exception) {
                logger.error("Failed to download JCEF resources", e)
                ErrorHandler.fatal("Failed to download JCEF resources")
            }
        }
    }

    override fun shutdownBrowserBackend() {
        try {
            MCEF.INSTANCE.shutdown()
            cacheFolder.deleteRecursively()
        } catch (e: Exception) {
            logger.error("Failed to shutdown MCEF", e)
        }
    }

    override fun createTab(url: String, frameRate: Int) = JcefTab(this, url, frameRate) { false }.apply {
        synchronized(tabs) {
            tabs += this
            tabs.sortBy { it.preferOnTop }
        }
    }

    override fun createInputAwareTab(url: String, frameRate: Int, takesInput: () -> Boolean) =
        JcefTab(this, url, frameRate, takesInput = takesInput).apply {
            synchronized(tabs) {
                tabs += this
                tabs.sortBy { it.preferOnTop }
            }
        }

    override fun getTabs() = tabs

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
