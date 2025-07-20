package net.ccbluex.liquidbounce.integration.interop.protocol.rest.v1.client

import com.google.gson.JsonElement
import io.netty.handler.codec.http.FullHttpResponse
import net.ccbluex.liquidbounce.api.thirdparty.IpInfoApi
import net.ccbluex.liquidbounce.config.gson.interopGson
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.netty.http.model.RequestObject
import net.ccbluex.netty.http.util.httpForbidden
import net.ccbluex.netty.http.util.httpOk

// GET /api/v1/client/session
@Suppress("UNUSED_PARAMETER")
fun getSessionInfo(requestObject: RequestObject): FullHttpResponse {
    val sessionInfo: JsonElement = interopGson.toJsonTree(mc.session)
    return httpOk(sessionInfo)
}

// GET /api/v1/client/location
@Suppress("UNUSED_PARAMETER")
fun getLocationInfo(requestObject: RequestObject): FullHttpResponse {
    val locationInfo = IpInfoApi.current ?: return httpForbidden("Location is not known")
    return httpOk(interopGson.toJsonTree(locationInfo))
}
