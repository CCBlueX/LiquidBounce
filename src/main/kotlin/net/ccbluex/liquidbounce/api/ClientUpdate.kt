/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2016 - 2024 CCBlueX
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
package net.ccbluex.liquidbounce.api

import com.vdurmont.semver4j.Semver
import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.LiquidBounce.IN_DEVELOPMENT
import net.ccbluex.liquidbounce.LiquidBounce.clientVersion
import net.ccbluex.liquidbounce.api.v1.ClientApiV1.requestNewestBuildEndpoint
import net.ccbluex.liquidbounce.utils.client.logger
import java.text.SimpleDateFormat
import java.util.*

object ClientUpdate {

    val gitInfo = Properties().also {
        val inputStream = LiquidBounce::class.java.classLoader.getResourceAsStream("git.properties")

        if (inputStream != null) {
            it.load(inputStream)
        } else {
            it["git.build.version"] = "unofficial"
        }
    }

    val newestVersion by lazy {
        // https://api.liquidbounce.net/api/v1/version/builds/nextgen
        try {
            requestNewestBuildEndpoint(branch = LiquidBounce.clientBranch, release = !IN_DEVELOPMENT)
        } catch (e: Exception) {
            logger.error("Unable to receive update information", e)
            return@lazy null
        }
    }

    fun hasUpdate(): Boolean {
        try {
            val newestVersion = newestVersion ?: return false
            val newestSemVersion = Semver(newestVersion.lbVersion, Semver.SemverType.LOOSE)

            return if (IN_DEVELOPMENT) { // check if new build is newer than current build
                val newestVersionDate = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").parse(newestVersion.date)
                val currentVersionDate =
                    SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ").parse(gitInfo["git.commit.time"].toString())

                newestVersionDate.after(currentVersionDate)
            } else {
                // check if version number is higher than current version number (on release builds only!)
                val clientSemVersion = Semver(clientVersion, Semver.SemverType.LOOSE)

                newestVersion.release && newestSemVersion.isGreaterThan(clientSemVersion)
            }
        } catch (e: Exception) {
            logger.error("Unable to check for update", e)
            return false
        }
    }

}
