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

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.utils.*
import java.io.File

object Natives {

    val nativesRoot = File(LiquidBounce.configSystem.rootFolder, "natives")

    /**
     * Download required natives
     */
    fun downloadNatives() {
        if (nativesRoot.exists())
            return

        runCatching {
            val os = when {
                IS_WINDOWS -> "win"
                IS_MAC -> "mac"
                IS_UNIX -> "linux"
                else -> error("unsupported operating system")
            }

            logger.info("Download natives... (os: $os)")
            val nativeUrl = "https://cloud.liquidbounce.net/LiquidBounce/natives/$os-x64.zip"

            nativesRoot.mkdir()
            val pkgNatives = File(nativesRoot, "natives.zip").apply {
                createNewFile()
                HttpUtils.download(nativeUrl, this)
            }

            logger.info("Extracting natives...")
            extractZip(pkgNatives, nativesRoot)

            logger.debug("Deleting natives bundle...")
            pkgNatives.delete()
        }.onFailure {
            logger.error("Unable to download natives", it)
            nativesRoot.delete()
        }
    }

}
