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
@file:Suppress("TooManyFunctions")

package net.ccbluex.liquidbounce.integration.interop.protocol.rest.v1.client

import com.google.gson.JsonArray
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import net.ccbluex.liquidbounce.api.core.httpException
import net.ccbluex.liquidbounce.api.core.formatAvatarUrl
import net.ccbluex.liquidbounce.api.models.liquidproxy.ProxySession
import net.ccbluex.liquidbounce.api.models.liquidproxy.ProxySubscription
import net.ccbluex.liquidbounce.config.ConfigSystem
import net.ccbluex.liquidbounce.config.gson.interopGson
import net.ccbluex.liquidbounce.features.cosmetic.ClientAccountManager
import net.ccbluex.liquidbounce.features.misc.proxy.liquidproxy.LiquidProxy
import net.ccbluex.liquidbounce.features.misc.proxy.liquidproxy.LocationProbes
import net.ccbluex.liquidbounce.integration.interop.badRequest
import net.ccbluex.liquidbounce.integration.interop.serviceUnavailable
import net.ccbluex.liquidbounce.integration.interop.unauthorized
import net.ccbluex.liquidbounce.utils.client.logger
import net.ccbluex.liquidbounce.utils.client.mc
import java.io.IOException
import java.time.LocalDateTime
import java.time.ZoneOffset

/**
 * LiquidProxy endpoints
 */

/**
 * Answers a failed call to the LiquidBounce API with a reason the theme can show.
 */
private suspend inline fun <T> ApplicationCall.liquidProxy(block: () -> T): T = try {
    block()
} catch (e: IOException) {
    logger.error("LiquidProxy request failed", e)
    val code = e.httpException?.code ?: serviceUnavailable("LiquidProxy is not reachable")
    if (code == HttpStatusCode.Unauthorized.value) {
        unauthorized("Your LiquidBounce login has expired. Log in again.")
    }
    serviceUnavailable("LiquidProxy did not answer ($code)")
} catch (e: IllegalStateException) {
    badRequest(e.message ?: "LiquidProxy request failed")
}

private data class LiquidProxyState(
    val loggedIn: Boolean,
    val email: String? = null,
    val subscription: SubscriptionInfo? = null,
    val plans: List<PlanInfo> = emptyList(),
    val level: Int = LiquidProxy.SMART_LEVEL,
    val forwardAuthentication: Boolean = true,
    /** The location chosen last */
    val location: String? = null,
    /** The location the current proxy goes through */
    val connected: String? = null,
)

private data class PlanInfo(val level: Int, val name: String, val description: String?)

private data class SubscriptionInfo(
    val plan: String,
    /** `active`, `expired` or `unavailable` */
    val state: String,
    val expiresAt: Long,
    val autoRenew: Boolean,
)

private fun ProxySubscription.info() = SubscriptionInfo(
    plan = LiquidProxy.plans.lastOrNull { it.level <= level }?.name ?: "LiquidProxy",
    state = when {
        status != 0 -> "unavailable"
        isActive -> "active"
        else -> "expired"
    },
    expiresAt = expiresAt.toEpochMilli(),
    autoRenew = autoRenew,
)

// GET /api/v1/client/liquidproxy
private fun Route.getState() = get {
    if (!LiquidProxy.isLoggedIn) {
        call.respond(interopGson.toJsonTree(LiquidProxyState(loggedIn = false)))
        return@get
    }

    val refresh = call.request.queryParameters["refresh"] == "true"
    val account = ClientAccountManager.clientAccount
    val subscription = call.liquidProxy {
        if (account.userInformation == null || refresh) {
            account.updateInfo()
        }
        LiquidProxy.locations()
        LiquidProxy.subscription(refresh).also {
            LiquidProxy.refreshConnection()
        }
    }

    call.respond(interopGson.toJsonTree(LiquidProxyState(
        loggedIn = true,
        email = account.userInformation?.email,
        subscription = subscription?.info(),
        plans = LiquidProxy.plans.map { PlanInfo(it.level, it.name, it.description) },
        level = LiquidProxy.effectiveLevel,
        forwardAuthentication = LiquidProxy.forwardAuthentication,
        location = LiquidProxy.location.ifEmpty { null },
        connected = LiquidProxy.connectedLocation?.code,
    )))
}

private data class LocationInfo(
    val code: String,
    val name: String,
    val countryCode: String,
    val latitude: Double?,
    val longitude: Double?,
    val probed: Boolean,
    val latency: Int?,
    val maintenance: Boolean,
)

// GET /api/v1/client/liquidproxy/locations
private fun Route.getLocations() = get("/locations") {
    val locations = call.liquidProxy { LiquidProxy.locations() }
    LocationProbes.probe(locations)

    call.respond(JsonArray().apply {
        for (location in locations) {
            val probe = LocationProbes[location]
            add(interopGson.toJsonTree(LocationInfo(
                code = location.code,
                name = location.name,
                countryCode = location.countryCode,
                latitude = location.latitude,
                longitude = location.longitude,
                probed = probe != null,
                latency = probe?.latency,
                maintenance = probe?.maintenance ?: false,
            )))
        }
    })
}

private data class SessionInfo(
    val id: String,
    val username: String,
    val avatar: String,
    val server: String,
    val country: String,
    val type: String?,
    val location: String,
    val startedAt: Long,
    val lastSeenAt: Long,
    val connected: Boolean,
    val error: String?,
)

private fun String.epochMillis() = LocalDateTime.parse(this).toInstant(ZoneOffset.UTC).toEpochMilli()

private fun ProxySession.info() = SessionInfo(
    id = connId,
    username = username,
    avatar = formatAvatarUrl(null, username),
    server = serverAddr,
    country = country,
    type = if (ipType == "Isp") "ISP" else ipType,
    location = node.substringBefore('-'),
    startedAt = firstSeen.epochMillis(),
    lastSeenAt = lastSeen.epochMillis(),
    connected = connected,
    error = error,
)

// GET /api/v1/client/liquidproxy/sessions
private fun Route.getSessions() = get("/sessions") {
    val sessions = call.liquidProxy { LiquidProxy.sessions() }
    call.respond(JsonArray().apply {
        sessions.forEach { add(interopGson.toJsonTree(it.info())) }
    })
}

// POST /api/v1/client/liquidproxy/sessions/{id}/end
private fun Route.endSession() = post("/sessions/{id}/end") {
    val id = call.parameters["id"] ?: call.badRequest("Missing session")
    call.liquidProxy { LiquidProxy.endSession(id) }
    call.respond(HttpStatusCode.NoContent)
}

// PUT /api/v1/client/liquidproxy/settings
private fun Route.putSettings() = put("/settings") {
    data class SettingsRequest(val level: Int?, val forwardAuthentication: Boolean?)

    val body = call.receive<SettingsRequest>()
    call.liquidProxy {
        LiquidProxy.subscription()

        body.level?.let { level ->
            check(LiquidProxy.plans.any { it.level == level }) { "Your subscription does not include this plan" }
            LiquidProxy.level = level
        }
        body.forwardAuthentication?.let { LiquidProxy.forwardAuthentication = it }
        ConfigSystem.store(LiquidProxy)

        LiquidProxy.refreshConnection()
    }
    call.respond(HttpStatusCode.NoContent)
}

// POST /api/v1/client/liquidproxy/connect
private fun Route.postConnect() = post("/connect") {
    data class ConnectRequest(val location: String)

    val body = call.receive<ConnectRequest>()
    call.liquidProxy { LiquidProxy.connect(body.location) }
    call.respond(HttpStatusCode.NoContent)
}

// POST /api/v1/client/liquidproxy/disconnect
private fun Route.postDisconnect() = post("/disconnect") {
    LiquidProxy.disconnect()
    call.respond(HttpStatusCode.NoContent)
}

// POST /api/v1/client/liquidproxy/new-ip
private fun Route.postNewIp() = post("/new-ip") {
    data class NewIpResponse(val username: String, val alreadyRequested: Boolean)

    val username = mc.user.name
    val requested = call.liquidProxy { LiquidProxy.requestNewIp(username) }
    call.respond(interopGson.toJsonTree(NewIpResponse(username, alreadyRequested = !requested)))
}

/**
 * The proxy LiquidProxy would connect with, for use outside the client.
 */
private suspend fun ApplicationCall.credentials() = liquidProxy {
    val subscription = LiquidProxy.subscription()?.takeIf { it.isActive }
        ?: error("You have no active LiquidProxy subscription")
    val locations = LiquidProxy.locations()
    val location = LiquidProxy.connectedLocation
        ?: locations.find { it.code == LiquidProxy.location }
        ?: locations.first()
    LiquidProxy.proxy(location, subscription)
}

// GET /api/v1/client/liquidproxy/credentials
private fun Route.getCredentials() = get("/credentials") {
    data class CredentialsResponse(val host: String, val port: Int, val username: String, val password: String)

    val proxy = call.credentials()
    val credentials = proxy.credentials ?: call.badRequest("No credentials")
    call.respond(interopGson.toJsonTree(
        CredentialsResponse(proxy.host, proxy.port, credentials.username, credentials.password)
    ))
}

// POST /api/v1/client/liquidproxy/credentials/clipboard
private fun Route.postCredentialsClipboard() = post("/credentials/clipboard") {
    val proxy = call.credentials()
    val credentials = proxy.credentials ?: call.badRequest("No credentials")
    val text = "${proxy.host}:${proxy.port}:${credentials.username}:${credentials.password}"
    mc.execute {
        mc.keyboardHandler.clipboard = text
    }
    call.respond(HttpStatusCode.NoContent)
}

internal fun Route.liquidProxyRoutes() = route("/liquidproxy") {
    getState()
    getLocations()
    getSessions()
    endSession()
    putSettings()
    postConnect()
    postDisconnect()
    postNewIp()
    getCredentials()
    postCredentialsClipboard()
}
