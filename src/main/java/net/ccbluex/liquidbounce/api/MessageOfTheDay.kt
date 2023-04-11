package net.ccbluex.liquidbounce.api

import net.ccbluex.liquidbounce.LiquidBounce.CLIENT_API
import net.ccbluex.liquidbounce.file.FileManager.PRETTY_GSON
import net.ccbluex.liquidbounce.utils.ClientUtils.LOGGER
import net.ccbluex.liquidbounce.utils.misc.HttpUtils.get

val messageOfTheDay by lazy {
    try {
        PRETTY_GSON.fromJson(get("$CLIENT_API/client/motd/legacy"), MessageOfTheDay::class.java)
    } catch (e: Exception) {
        LOGGER.error("Unable to receive message of the day", e)
        return@lazy null
    }
}

data class MessageOfTheDay(val message: String)