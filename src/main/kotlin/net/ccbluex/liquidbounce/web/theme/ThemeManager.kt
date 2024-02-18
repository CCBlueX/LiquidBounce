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
 *
 */
package net.ccbluex.liquidbounce.web.theme

import net.ccbluex.liquidbounce.config.ConfigSystem
import net.ccbluex.liquidbounce.config.util.decode
import net.ccbluex.liquidbounce.utils.client.logger
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.io.extractZip
import net.ccbluex.liquidbounce.utils.io.resource
import net.ccbluex.liquidbounce.web.browser.BrowserManager
import net.ccbluex.liquidbounce.web.browser.supports.tab.ITab
import net.ccbluex.liquidbounce.web.integration.IntegrationHandler
import net.ccbluex.liquidbounce.web.integration.VirtualScreenType
import net.ccbluex.liquidbounce.web.socket.netty.NettyServer.Companion.NETTY_ROOT
import net.minecraft.client.gui.screen.ChatScreen
import java.io.File

object ThemeManager {

    internal val themesFolder = File(ConfigSystem.rootFolder, "themes")
    private val defaultTheme = Theme.defaults()

    var activeTheme = defaultTheme
        set(value) {
            if (!value.exists) {
                logger.warn("Unable to set theme to ${value.name}, theme does not exist")
                return
            }

            field = value

            // Update integration browser
            IntegrationHandler.updateIntegrationBrowser()
        }

    private val takesInputHandler: () -> Boolean
        get() = { mc.currentScreen != null && mc.currentScreen !is ChatScreen }

    fun openImmediate(virtualScreenType: VirtualScreenType? = null, markAsStatic: Boolean = false): ITab =
        BrowserManager.browser?.createTab(getUrl(virtualScreenType?.routeName, markAsStatic))
            ?: error("Browser is not initialized")

    fun openInputAwareImmediate(virtualScreenType: VirtualScreenType? = null, markAsStatic: Boolean = false): ITab =
        BrowserManager.browser?.createInputAwareTab(getUrl(virtualScreenType?.routeName, markAsStatic), takesInputHandler)
            ?: error("Browser is not initialized")

    fun updateImmediate(tab: ITab?, virtualScreenType: VirtualScreenType? = null) =
        tab?.loadUrl(getUrl(virtualScreenType?.routeName))

    fun doesOverlay(name: String) = activeTheme.doesOverlay(name) || defaultTheme.doesOverlay(name)

    fun doesSupport(name: String) = activeTheme.doesSupport(name) || defaultTheme.doesSupport(name)

    fun getUrl(name: String? = null, markAsStatic: Boolean = false) =
        (activeTheme.getUrl(name) ?: defaultTheme.getUrl(name))?.let { if (markAsStatic) "$it?static" else it }
            ?: error("No theme supports $name")

}

class Theme(val name: String) {

    val folder = File(ThemeManager.themesFolder, name)
    val metadata: ThemeMetadata = run {
        val metadataFile = File(folder, "metadata.json")
        if (!metadataFile.exists()) {
            error("Theme $name does not contain a metadata file")
        }

        decode<ThemeMetadata>(metadataFile.readText())
    }

    val exists: Boolean
        get() = folder.exists()

    private val url: String
        get() = "$NETTY_ROOT/${ThemeManager.activeTheme.name}/#/"

    /**
     * Get the URL to the given page name in the theme.
     */
    fun getUrl(name: String?) = if (name == null) {
        url
    } else if (doesSupport(name) || doesOverlay(name)) {
        "$url$name"
    } else {
        null
    }

    fun doesSupport(name: String) = metadata.supports.contains(name)

    fun doesOverlay(name: String) = metadata.overlays.contains(name)

    companion object {

        fun defaults() = runCatching {
            val folder = ThemeManager.themesFolder.resolve("default")
            val stream = resource("/assets/liquidbounce/default_theme.zip")

            if (folder.exists()) {
                folder.deleteRecursively()
            }

            extractZip(stream, folder)
            folder.deleteOnExit()

            Theme("default")
        }.onFailure {
            logger.error("Unable to extract default theme", it)
        }.onSuccess {
            logger.info("Successfully extracted default theme")
        }.getOrThrow()

    }

}

data class ThemeMetadata(
    val name: String,
    val author: String,
    val version: String,
    val baseUrl: String,
    val supports: List<String>,
    val overlays: List<String>
)
