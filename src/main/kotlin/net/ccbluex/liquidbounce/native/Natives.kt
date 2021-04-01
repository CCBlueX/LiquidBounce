/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2016 - 2021 CCBlueX
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
package net.ccbluex.liquidbounce.native

import net.ccbluex.liquidbounce.config.ConfigSystem
import net.ccbluex.liquidbounce.utils.*
import java.io.File
import kotlin.system.exitProcess

object Natives {

    val nativesRoot = File(ConfigSystem.rootFolder, "natives")
    private const val LIBRARY_VERSION = 0.41

    /**
     * Download required natives
     */
    fun downloadNatives() {
        runCatching {
            val versionsFile = File(nativesRoot, "VERSION")

            if (versionsFile.exists() && versionsFile.readText().toDoubleOrNull() == LIBRARY_VERSION) {
                return
            }

            if (nativesRoot.exists()) {
                nativesRoot.deleteRecursively()
            }

            val os = when {
                IS_WINDOWS -> "win"
                IS_MAC -> "mac"
                IS_UNIX -> "linux"
                else -> error("unsupported operating system")
            }

            logger.info("Downloading v$LIBRARY_VERSION natives... (os: $os)")
            val nativeUrl = "https://cloud.liquidbounce.net/LiquidBounce/natives/$LIBRARY_VERSION/$os-x64.zip"

            nativesRoot.mkdir()
            val pkgNatives = File(nativesRoot, "natives.zip").apply {
                createNewFile()
                HttpUtils.download(nativeUrl, this)
            }

            logger.info("Extracting natives...")
            extractZip(pkgNatives, nativesRoot)
            versionsFile.createNewFile()
            versionsFile.writeText(LIBRARY_VERSION.toString())

            logger.debug("Deleting natives bundle...")
            pkgNatives.delete()

            logger.info("Successfully loaded natives.")
        }.onFailure {
            logger.error("Unable to download natives", it)
            nativesRoot.delete()

            exitProcess(-1)
        }
    }

}
