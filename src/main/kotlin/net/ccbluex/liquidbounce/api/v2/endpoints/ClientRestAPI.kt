package net.ccbluex.liquidbounce.api.v2.endpoints

import net.ccbluex.liquidbounce.api.v2.ClientApiV2

/**
 * Represents the client rest api.
 *
 * GET /api/v2/client/services -> responses with [Services]
 * GET /api/v2/client/motd -> responses with the message of the day
 *
 * GET /api/v2/client/sync
 * POST /api/v2/client/sync
 */
class ClientRestAPI(private val clientApiV2: ClientApiV2) {

    fun getServices() = clientApiV2.get<Services>("client/services")

    fun getMessageOfTheDay() = clientApiV2.get<MessageOfTheDay>("client/motd")

}

data class MessageOfTheDay(val message: String)

/// This data class resprents the
// response of the endpoint `/api/v2/client/services`
data class Services(
    // Includes a list of enabled generators, in case we want to disable some
    val generators: List<String>,
    // This service is used to identify the location of the IP address
    val ipService: String,
    // This service is used for the head img in the menus
    val headService: String,
)
