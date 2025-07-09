package net.ccbluex.liquidbounce.integration.backend

import net.ccbluex.liquidbounce.integration.IntegrationListener
import net.ccbluex.liquidbounce.integration.backend.browser.BrowserSettings
import net.ccbluex.liquidbounce.integration.backend.browser.Browser
import net.ccbluex.liquidbounce.integration.backend.browser.BrowserViewport
import net.ccbluex.liquidbounce.integration.backend.input.InputAcceptor
import net.ccbluex.liquidbounce.integration.task.TaskManager

/**
 * The browser interface which is used to create tabs and manage the browser backend.
 * Due to different possible browser backends, this interface is used to abstract the browser backend.
 */
interface BrowserBackend {

    val isInitialized: Boolean
    var isAccelerationSupported: Boolean
    val browsers: List<Browser>

    fun makeDependenciesAvailable(taskManager: TaskManager, whenAvailable: () -> Unit)

    /**
     * Starts the browser backend and initializes it.
     */
    fun start()

    /**
     * Stops the browser backend and cleans up resources.
     */
    fun stop()

    /**
     * Usually does a global render update of the browser.
     */
    fun update()

    fun createBrowser(
        url: String,
        position: BrowserViewport = BrowserViewport.Companion.FULLSCREEN,
        settings: BrowserSettings = IntegrationListener.browserSettings,
        priority: Short = 0,
        inputAcceptor: InputAcceptor? = null
    ): Browser

}
