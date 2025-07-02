package net.ccbluex.liquidbounce.integration.browser

import net.ccbluex.liquidbounce.integration.browser.tab.Tab
import net.ccbluex.liquidbounce.integration.browser.tab.TabPosition
import net.ccbluex.liquidbounce.integration.task.TaskManager

/**
 * The browser interface which is used to create tabs and manage the browser backend.
 * Due to different possible browser backends, this interface is used to abstract the browser backend.
 */
interface Browser {

    fun makeDependenciesAvailable(taskManager: TaskManager, whenAvailable: () -> Unit)

    fun startBrowser()

    fun stopBrowser()

    fun isInitialized(): Boolean

    fun createTab(url: String, position: TabPosition = TabPosition.Companion.FULLSCREEN, frameRate: Int): Tab

    fun createInputAwareTab(url: String, position: TabPosition = TabPosition.Companion.FULLSCREEN, frameRate: Int,
                            takesInput: () -> Boolean): Tab

    fun getTabs(): List<Tab>

    fun getBrowserType(): BrowserType

    fun drawGlobally()

}
