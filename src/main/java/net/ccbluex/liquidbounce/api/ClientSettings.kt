/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.api

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import net.ccbluex.liquidbounce.utils.ClientUtils.LOGGER
import net.ccbluex.liquidbounce.utils.ClientUtils.displayChatMessage
import java.text.SimpleDateFormat

import kotlin.concurrent.thread

// Define a loadingLock object to synchronize access to the settings loading code
private val loadingLock = Object()

// Define a mutable list of AutoSetting objects to store the loaded settings
var autoSettingsList: Array<AutoSettings>? = null

// Define a function to load settings from a remote GitHub repository
suspend fun loadSettings(useCached: Boolean, join: Long? = null, callback: (Array<AutoSettings>) -> Unit) {
    val loadingComplete = CompletableDeferred<Unit>()

    // Fetch the settings list from the API outside the synchronized block
    val fetchedSettings = try {
        ClientApi.requestSettingsList().map { it ->
            runCatching {
                val date = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").parse(it.date)
                val statusDate = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").parse(it.statusDate)

                val humanReadableDateFormat = SimpleDateFormat()

                it.date = humanReadableDateFormat.format(date)
                it.statusDate = humanReadableDateFormat.format(statusDate)
            }.onFailure {
                LOGGER.error("Failed to parse date.", it)
            }

            it
        }.toTypedArray()
    } catch (e: Exception) {
        LOGGER.error("Failed to fetch auto settings list.", e)
        displayChatMessage("Failed to fetch auto settings list.")
        null
    }

    // Synchronize access to the loading code to prevent concurrent loading of settings
    synchronized(loadingLock) {
        // If cached settings are requested and have been loaded previously, return them immediately
        if (useCached && autoSettingsList != null) {
            callback(autoSettingsList!!)
            return
        }

        // If settings were fetched successfully, update the autoSettingsList and invoke the callback
        fetchedSettings?.let {
            autoSettingsList = it
            callback(it)
        }
    }

    // If a join time is provided, wait until the loading is complete or the timeout is reached
    if (join != null) {
        withTimeoutOrNull(join) {
            loadingComplete.await()
        }
    }
}
