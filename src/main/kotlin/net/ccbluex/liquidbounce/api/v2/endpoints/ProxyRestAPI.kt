package net.ccbluex.liquidbounce.api.v2.endpoints

import net.ccbluex.liquidbounce.api.v2.ClientApiV2

/**
 * Represents the proxy rest api.
 *
 * GET /api/v2/proxy -> responses with the list of available liquidproxy servers (REQUIRES AUTH)
 * POST /api/v2/proxy/<username>/disconnect -> POST disconnect reason to liquidproxy (REQUIRES AUTH)
 * DELETE /api/v2/proxy/<username> -> Delete bound proxy for the specified username (REQUIRES AUTH)
 */
class ProxyRestAPI(private val api: ClientApiV2) {

    fun getProxies() = api.get<List<LiquidProxy>>("proxy")

    fun disconnectProxy(username: String, reason: String) =
        api.post<Unit, String>("proxy/$username/disconnect", reason)

    fun deleteProxy(username: String) =
        api.delete<Unit>("proxy/$username")

}

data class LiquidProxy(
    val name: String,
    val address: String,
)
