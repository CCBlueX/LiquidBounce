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
 */
package net.ccbluex.liquidbounce.web.theme

import net.ccbluex.liquidbounce.config.ConfigSystem
import net.ccbluex.liquidbounce.utils.client.logger
import net.ccbluex.liquidbounce.utils.io.extractZip
import net.ccbluex.liquidbounce.utils.io.resource
import net.ccbluex.liquidbounce.web.integration.IntegrationHandler
import net.ccbluex.liquidbounce.web.socket.netty.NettyServer.Companion.NETTY_ROOT
import java.io.File

object ThemeManager {

    val themesFolder = File(ConfigSystem.rootFolder, "themes")
    val defaultTheme = Theme.defaults()

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

    /**
     * The integration URL represents the URL to the integration page of the active theme.
     *
     * todo: remove title parameter, this should default to the root of the theme
     */
    val integrationUrl: String
        get() = "http://localhost:5173"

    val overlayUrl: String
        get() = "$NETTY_ROOT/${activeTheme.name}/#/hud?static"

}

class Theme(val name: String) {

    internal val themeFolder = File(ThemeManager.themesFolder, name)

    val exists: Boolean
        get() = themeFolder.exists()

    companion object {

        fun defaults() = Theme("default").apply {
            runCatching {
                val stream = resource("/assets/liquidbounce/default_theme.zip")

                if (exists) {
                    themeFolder.deleteRecursively()
                }

                extractZip(stream, themeFolder)
                themeFolder.deleteOnExit()
            }.onFailure {
                logger.error("Unable to extract default theme", it)
            }.onSuccess {
                logger.info("Successfully extracted default theme")
            }

        }

    }

}
