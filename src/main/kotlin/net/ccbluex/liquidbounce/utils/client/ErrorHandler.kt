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
package net.ccbluex.liquidbounce.utils.client

import net.ccbluex.liquidbounce.LiquidBounce
import org.lwjgl.util.tinyfd.TinyFileDialogs
import kotlin.system.exitProcess

/**
 * The ErrorHandler class is responsible for handling and reporting errors encountered by the application.
 */
object ErrorHandler {

    /**
     * Logs the error message, uploads the error information to Paste,
     * displays an error message to the user, copies the error information to the clipboard,
     * opens the GitHub issues page, and exits the application.
     *
     * TODO: Upload error to our API if allowed by the user
     *
     * @param error the throwable error that occurred
     */
    fun fatal(error: Throwable, additionalMessage: String? = null) {
        logger.error("Fatal error", error)

        val logPath = mc.runDirectory.resolve("logs").resolve("latest.log").absolutePath
        val message = """LiquidBounce Nextgen has encountered an error!
                    |Try restarting the client.
                    |Please report this issue to the developers on GitHub if the error keeps occuring.
                    |
                    |Include the following information:
                    |OS: ${System.getProperty("os.name")} (${System.getProperty("os.arch")})
                    |Java: ${System.getProperty("java.version")}
                    |Client Version: ${LiquidBounce.clientVersion} (${LiquidBounce.clientCommit})
                    |${additionalMessage ?: ""}
                    |Error: ${error.message}
                    |Error Type: ${error.javaClass.name}
                    |
                    |Include your game log, which can be found at:
                    |$logPath""".trimMargin()

        TinyFileDialogs.tinyfd_messageBox(
            "LiquidBounce Nextgen",
            message.replace("\"", "").replace("'", ""),
            "ok",
            "error",
            true
        )

        // Open GitHub issue
        browseUrl("https://github.com/CCBlueX/LiquidBounce/issues")

        exitProcess(1)
    }

}
