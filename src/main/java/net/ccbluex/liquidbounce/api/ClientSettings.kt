package net.ccbluex.liquidbounce.api

import net.ccbluex.liquidbounce.utils.ClientUtils.LOGGER
import net.ccbluex.liquidbounce.utils.ClientUtils.displayChatMessage
import java.text.SimpleDateFormat

import kotlin.concurrent.thread

// Define a loadingLock object to synchronize access to the settings loading code
private val loadingLock = Object()

// Define a mutable list of AutoSetting objects to store the loaded settings
var autoSettingsList: Array<AutoSettings>? = null

// Define a function to load settings from a remote GitHub repository
fun loadSettings(useCached: Boolean, join: Long? = null, callback: (Array<AutoSettings>) -> Unit) {
    // Spawn a new thread to perform the loading operation
    val thread = thread {
        // Synchronize access to the loading code to prevent concurrent loading of settings
        synchronized(loadingLock) {
            // If cached settings are requested and have been loaded previously, return them immediately
            if (useCached && autoSettingsList != null) {
                callback(autoSettingsList!!)
                return@thread
            }

            try {
                // Fetch the settings list from the API
                val autoSettings = ClientApi.requestSettingsList().map {
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

                // Invoke the callback with the parsed AutoSetting objects and store them in the cache for future use
                callback(autoSettings)
                autoSettingsList = autoSettings
            } catch (e: Exception) {
                LOGGER.error("Failed to fetch auto settings list.", e)

                // If an error occurs, display an error message to the user
                displayChatMessage("Failed to fetch auto settings list.")
            }
        }
    }

    // If a join time is provided, block the current thread until the loading thread completes or the timeout is reached
    if (join != null) {
        thread.join(join)
    }
}
