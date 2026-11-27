/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2026 CCBlueX
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
package net.ccbluex.liquidbounce.api.services.liquidproxy

import net.ccbluex.liquidbounce.api.core.apiRetrofit
import net.ccbluex.liquidbounce.api.core.orNotFound
import net.ccbluex.liquidbounce.api.models.auth.OAuthSession
import net.ccbluex.liquidbounce.api.models.auth.authorization
import net.ccbluex.liquidbounce.api.models.liquidproxy.ProxyCredentials
import net.ccbluex.liquidbounce.api.models.liquidproxy.ProxyEntitlement
import net.ccbluex.liquidbounce.api.models.liquidproxy.ProxyLocation
import net.ccbluex.liquidbounce.api.models.liquidproxy.ProxyNetwork
import net.ccbluex.liquidbounce.api.models.liquidproxy.ProxySavedIp
import net.ccbluex.liquidbounce.api.models.liquidproxy.ProxySession
import retrofit2.create
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * The LiquidProxy API. Locations and networks are public, everything else belongs to the LiquidBounce account
 * of the session.
 */
object LiquidProxyApi {

    private val service = apiRetrofit(BASE_URL).create<Service>()

    suspend fun getLocations() = service.locations()

    suspend fun getNetworks() = service.networks()

    /**
     * @return `null` for an account that never had LiquidProxy
     */
    suspend fun getEntitlement(session: OAuthSession) = orNotFound { service.entitlement(session.authorization) }

    suspend fun getCredentials(session: OAuthSession) = service.credentials(session.authorization)

    /**
     * The sessions on Minecraft servers, running ones first, each newest first.
     */
    suspend fun getSessions(session: OAuthSession, limit: Int) =
        service.sessions(session.authorization, MINECRAFT, limit).items

    suspend fun getSession(session: OAuthSession, id: String) = service.session(session.authorization, id)

    /**
     * Asks for a running session to be closed, which happens a moment later.
     */
    suspend fun closeSession(session: OAuthSession, id: String) {
        // One that is closed already is not found
        orNotFound { service.closeConnection(session.authorization, id) }
    }

    suspend fun getSavedIps(session: OAuthSession) = service.savedIps(session.authorization).items

    /**
     * Makes the next join of the player to the server get a new IP.
     */
    suspend fun releaseSavedIp(session: OAuthSession, ip: ProxySavedIp) {
        // Without a saved IP, that join gets a new one anyway
        orNotFound {
            service.releaseSavedIp(session.authorization, ip.service, ip.locationCode, ip.key, ip.hostKey)
        }
    }

    internal class Page<T>(val items: List<T>)

    internal interface Service {
        @GET("locations")
        suspend fun locations(): List<ProxyLocation>

        @GET("networks")
        suspend fun networks(): List<ProxyNetwork>

        @GET("me/entitlement")
        suspend fun entitlement(@Header("Authorization") authorization: String): ProxyEntitlement

        @GET("me/credentials")
        suspend fun credentials(@Header("Authorization") authorization: String): ProxyCredentials

        @GET("me/sessions")
        suspend fun sessions(
            @Header("Authorization") authorization: String,
            @Query("service") service: String,
            @Query("limit") limit: Int,
        ): Page<ProxySession>

        @GET("me/sessions/{id}")
        suspend fun session(@Header("Authorization") authorization: String, @Path("id") id: String): ProxySession

        @POST("me/connections/{id}/close")
        suspend fun closeConnection(@Header("Authorization") authorization: String, @Path("id") id: String)

        @GET("me/sticky-exits")
        suspend fun savedIps(@Header("Authorization") authorization: String): Page<ProxySavedIp>

        @DELETE("me/sticky-exits/{service}/{location}/{key}")
        suspend fun releaseSavedIp(
            @Header("Authorization") authorization: String,
            @Path("service") service: String,
            @Path("location") location: String,
            @Path("key") key: String,
            @Query("host") host: String?,
        )
    }

}

private const val BASE_URL = "https://api.liquidproxy.net/v1"
private const val MINECRAFT = "minecraft_java"
