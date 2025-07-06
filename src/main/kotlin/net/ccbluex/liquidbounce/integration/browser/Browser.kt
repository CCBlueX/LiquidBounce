package net.ccbluex.liquidbounce.integration.browser

import net.ccbluex.liquidbounce.integration.browser.tab.Tab
import net.ccbluex.liquidbounce.integration.browser.tab.TabPosition
import net.ccbluex.liquidbounce.integration.task.TaskManager

/**
 * The browser interface which is used to create tabs and manage the browser backend.
 * Due to different possible browser backends, this interface is used to abstract the browser backend.
 */
interface Browser {

    val isInitialized: Boolean
    var isAccelerationSupported: Boolean
    val tabs: List<Tab>
    val type: BrowserType

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

    fun createTab(
        url: String,
        position: TabPosition = TabPosition.Companion.FULLSCREEN,
        settings: BrowserRendererSettings= BrowserManager.settings
    ): Tab

    fun createInputAwareTab(
        url: String,
        position: TabPosition = TabPosition.Companion.FULLSCREEN,
        settings: BrowserRendererSettings = BrowserManager.settings,
        takesInput: () -> Boolean
    ): Tab

}
