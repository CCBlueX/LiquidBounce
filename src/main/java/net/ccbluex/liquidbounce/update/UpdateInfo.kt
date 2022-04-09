/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.update

import com.google.gson.Gson

import com.google.gson.annotations.SerializedName
import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.LiquidBounce.CLIENT_API
import net.ccbluex.liquidbounce.LiquidBounce.CLIENT_VERSION_INT
import net.ccbluex.liquidbounce.utils.ClientUtils

import net.ccbluex.liquidbounce.utils.misc.HttpUtils
import java.text.SimpleDateFormat

import java.util.*

object UpdateInfo {

    val gitInfo = Properties().also {
        val inputStream = LiquidBounce::class.java.classLoader.getResourceAsStream("git.properties")

        if(inputStream != null) {
            it.load(inputStream)
        } else {
            it["git.build.version"] = "unofficial"
        }
    }

    val newestVersion by lazy {
        // https://api.liquidbounce.net/api/v1/version/builds/legacy
        try {
            Gson().fromJson(HttpUtils.get("$CLIENT_API/version/newest/${gitInfo["git.branch"]}${if (LiquidBounce.IN_DEV) "" else "/release" }"), Build::class.java)
        } catch (e: Exception) {
            ClientUtils.getLogger().error("Unable to receive update information", e)
            return@lazy null
        }
    }

    fun hasUpdate(): Boolean {
        try {
            val newestVersion = newestVersion ?: return false
            val actualVersionNumber = newestVersion.lbVersion.substring(1).toIntOrNull() ?: 0 // version format: "b<VERSION>" on legacy

            return if (LiquidBounce.IN_DEV) { // check if new build is newer than current build
                val newestVersionDate = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").parse(newestVersion.date)
                val currentVersionDate = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ").parse(gitInfo["git.commit.time"].toString())

                newestVersionDate.after(currentVersionDate)
            } else {
                // check if version number is higher than current version number (on release builds only!)
                newestVersion.release && actualVersionNumber > CLIENT_VERSION_INT
            }
        } catch (e: Exception) {
            ClientUtils.getLogger().error("Unable to check for update", e)
            return false
        }
    }

}

data class Build(@SerializedName("build_id")
                 val buildId: Int,
                 @SerializedName("commit_id")
                 val commitId: String,
                 val branch: String,
                 @SerializedName("lb_version")
                 val lbVersion: String,
                 @SerializedName("mc_version")
                 val mcVersion: String,
                 val release: Boolean,
                 val date: String,
                 val message: String,
                 val url: String) {

}