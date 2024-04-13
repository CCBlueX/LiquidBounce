/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.api

import kotlinx.coroutines.runBlocking
import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.LiquidBounce.clientVersionNumber
import net.ccbluex.liquidbounce.LiquidBounce.IN_DEV
import net.ccbluex.liquidbounce.utils.ClientUtils.LOGGER
import java.text.SimpleDateFormat
import java.util.*
import net.ccbluex.liquidbounce.api.ClientApi.requestNewestBuildEndpoint

object ClientUpdate {

    val gitInfo = Properties().also {
        val inputStream = LiquidBounce::class.java.classLoader.getResourceAsStream("git.properties")

        if(inputStream != null) {
            it.load(inputStream)
        } else {
            it["git.build.version"] = "unofficial"
        }
    }

    val newestVersion by lazy {
        runBlocking {
            try {
                requestNewestBuildEndpoint(branch = LiquidBounce.clientBranch, release = !IN_DEV)
            } catch (e: Exception) {
                LOGGER.error("Unable to receive update information", e)
                null
            }
        }
    }

    fun hasUpdate(): Boolean {
        return runBlocking {
            try {
                val newestVersion = newestVersion ?: return@runBlocking false
                val actualVersionNumber = newestVersion.lbVersion.substring(1).toIntOrNull() ?: 0 // version format: "b<VERSION>" on legacy

                if (IN_DEV) { // check if new build is newer than current build
                    val newestVersionDate = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").parse(newestVersion.date)
                    val currentVersionDate = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ").parse(gitInfo["git.commit.time"].toString())

                    return@runBlocking newestVersionDate.after(currentVersionDate)
                } else {
                    // check if version number is higher than current version number (on release builds only!)
                    return@runBlocking newestVersion.release && actualVersionNumber > clientVersionNumber
                }
            } catch (e: Exception) {
                LOGGER.error("Unable to check for update", e)
                return@runBlocking false
            }
        }
    }

}

