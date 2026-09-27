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
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.httpMethod
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import kotlinx.coroutines.CancellationException
import net.ccbluex.liquidbounce.api.core.formatAvatarUrl
import net.ccbluex.liquidbounce.api.core.httpException
import net.ccbluex.liquidbounce.api.models.liquidproxy.ProxyLocation
import net.ccbluex.liquidbounce.api.models.liquidproxy.ProxySession
import net.ccbluex.liquidbounce.api.models.liquidproxy.ProxySubscription
import net.ccbluex.liquidbounce.api.models.liquidproxy.ProxySubscriptionType
import net.ccbluex.liquidbounce.config.ConfigSystem
import net.ccbluex.liquidbounce.config.gson.interopGson
import net.ccbluex.liquidbounce.event.events.NotificationEvent
import net.ccbluex.liquidbounce.features.cosmetic.ClientAccountManager
import net.ccbluex.liquidbounce.features.misc.proxy.liquidproxy.LiquidProxy
import net.ccbluex.liquidbounce.features.misc.proxy.liquidproxy.LocationProbes
import net.ccbluex.liquidbounce.integration.interop.HttpStatusException
import net.ccbluex.liquidbounce.integration.interop.badRequest
import net.ccbluex.liquidbounce.integration.interop.protocol.rest.v1.game.ServerIcons
import net.ccbluex.liquidbounce.lang.translation
import net.ccbluex.liquidbounce.utils.client.logger
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.client.notification
import java.io.IOException
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/*
 * LiquidProxy endpoints. The theme shows no messages of its own: notices come with the state, and a
 * notification tells the player how a change went.
 */

/**
 * Runs [block] against the LiquidBounce API and answers a failure with a reason the theme can show. When the
 * request changes something, the player is told why it failed.
 */
private suspend inline fun <T> ApplicationCall.liquidProxy(block: () -> T): T = try {
    block()
} catch (e: Exception) {
    val failure = when (e) {
        is HttpStatusException -> e
        is CancellationException -> throw e
        is IOException -> {
            logger.error("LiquidProxy request failed", e)
            when (val code = e.httpException?.code) {
                null -> HttpStatusException(
                    HttpStatusCode.ServiceUnavailable,
                    mapOf("reason" to translation("liquidbounce.liquidproxy.error.offline").string)
                )
                HttpStatusCode.Unauthorized.value -> HttpStatusException(
                    HttpStatusCode.Unauthorized,
                    mapOf("reason" to translation("liquidbounce.liquidproxy.error.loginExpired").string)
                )
                else -> HttpStatusException(
                    HttpStatusCode.ServiceUnavailable,
                    mapOf("reason" to translation("liquidbounce.liquidproxy.error.status", code).string)
                )
            }
        }
        is IllegalStateException ->
            HttpStatusException(HttpStatusCode.BadRequest, mapOf("reason" to e.message.orEmpty()))
        else -> throw e
    }
    if (request.httpMethod != HttpMethod.Get) {
        val reason = failure.body["reason"].orEmpty()
        mc.execute { notification("LiquidProxy", reason, NotificationEvent.Severity.ERROR) }
    }
    throw failure
}

/**
 * What the main panel says instead of the sessions.
 */
private data class Notice(val title: String, val text: String)

private data class LiquidProxyState(
    val loggedIn: Boolean,
    /** Without an answer from LiquidProxy, the [notice] says why */
    val reachable: Boolean = true,
    val notice: Notice? = null,
    /** `active`, `expired` or `unavailable`, `null` without a subscription */
    val subscription: String? = null,
    val plans: List<ProxySubscriptionType> = emptyList(),
    val level: Int = LiquidProxy.SMART_LEVEL,
    val forwardAuthentication: Boolean = true,
    /** The location chosen last */
    val location: String? = null,
    /** The location the current proxy goes through */
    val connected: String? = null,
)

private val ProxySubscription.state
    get() = when {
        status != 0 -> "unavailable"
        isActive -> "active"
        else -> "expired"
    }

/**
 * The notice for an account whose [subscription] can't be used, `null` when it can.
 */
private fun subscriptionNotice(subscription: ProxySubscription?, email: String?) = when {
    subscription == null -> Notice(
        translation("liquidbounce.liquidproxy.subscription.none").string,
        if (email != null) {
            translation("liquidbounce.liquidproxy.subscription.none.text", email).string
        } else {
            translation("liquidbounce.liquidproxy.subscription.none.textNoEmail").string
        }
    )
    subscription.status != 0 -> Notice(
        translation("liquidbounce.liquidproxy.subscription.unavailable").string,
        translation("liquidbounce.liquidproxy.subscription.unavailable.text").string
    )
    !subscription.isActive -> Notice(
        translation("liquidbounce.liquidproxy.subscription.ended").string,
        translation(
            "liquidbounce.liquidproxy.subscription.ended.text",
            DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).withZone(ZoneId.systemDefault())
                .format(subscription.expiresAt)
        ).string
    )
    else -> null
}

/**
 * Responds with the state, which every change answers with. [refresh] fetches the account and subscription again.
 */
private suspend fun ApplicationCall.respondState(refresh: Boolean = false) {
    if (!LiquidProxy.isLoggedIn) {
        val notice = Notice(
            translation("liquidbounce.liquidproxy.loggedOut").string,
            translation("liquidbounce.liquidproxy.loggedOut.text").string
        )
        respond(interopGson.toJsonTree(LiquidProxyState(loggedIn = false, notice = notice)))
        return
    }

    val account = ClientAccountManager.clientAccount
    val subscription = try {
        liquidProxy {
            if (account.userInformation == null || refresh) {
                account.updateInfo()
            }
            LiquidProxy.locations()
            LiquidProxy.subscription(refresh).also {
                LiquidProxy.refreshConnection()
            }
        }
    } catch (e: HttpStatusException) {
        val notice = Notice(translation("liquidbounce.liquidproxy.unreachable").string, e.body["reason"].orEmpty())
        respond(interopGson.toJsonTree(LiquidProxyState(loggedIn = true, reachable = false, notice = notice)))
        return
    }

    respond(interopGson.toJsonTree(LiquidProxyState(
        loggedIn = true,
        notice = subscriptionNotice(subscription, account.userInformation?.email),
        subscription = subscription?.state,
        plans = LiquidProxy.plans,
        level = LiquidProxy.effectiveLevel,
        forwardAuthentication = LiquidProxy.forwardAuthentication,
        location = LiquidProxy.location.ifEmpty { null },
        connected = LiquidProxy.connectedLocation?.code,
    )))
}

// GET /api/v1/client/liquidproxy
private fun Route.getState() = get {
    call.respondState(refresh = call.request.queryParameters["refresh"] == "true")
}

/**
 * The country of this location, or its city where the country has more than one location.
 */
private fun ProxyLocation.label(locations: List<ProxyLocation>): String {
    val country = name.substringBefore(" - ")
    val city = name.substringAfter(" - ", "")
    val shared = locations.count { it.countryCode == countryCode } > 1
    return if (shared && city.isNotEmpty()) city else country
}

private data class LocationInfo(
    val code: String,
    val label: String,
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
                label = location.label(locations),
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
    /** Favicon of the server, as a URL */
    val icon: String?,
    val country: String,
    val type: String?,
    val startedAt: Long,
    val lastSeenAt: Long,
    val connected: Boolean,
    val error: String?,
)

private fun String.epochMillis() = LocalDateTime.parse(this).toInstant(ZoneOffset.UTC).toEpochMilli()

private fun ProxySession.info(icon: String?) = SessionInfo(
    id = connId,
    username = username,
    avatar = formatAvatarUrl(null, username),
    server = serverAddr,
    icon = icon,
    country = country,
    type = if (ipType == "Isp") "ISP" else ipType,
    startedAt = firstSeen.epochMillis(),
    lastSeenAt = lastSeen.epochMillis(),
    connected = connected,
    error = error,
)

// GET /api/v1/client/liquidproxy/sessions
private fun Route.getSessions() = get("/sessions") {
    val sessions = call.liquidProxy { LiquidProxy.sessions() }
    call.respond(JsonArray().apply {
        for (session in sessions) {
            add(interopGson.toJsonTree(session.info(ServerIcons.of(session.serverAddr))))
        }
    })
}

// POST /api/v1/client/liquidproxy/sessions/{id}/end
private fun Route.endSession() = post("/sessions/{id}/end") {
    val id = call.parameters["id"] ?: call.badRequest("Missing session")
    call.liquidProxy { LiquidProxy.endSession(id) }
    val message = translation("liquidbounce.liquidproxy.sessionEnded").string
    mc.execute { notification("LiquidProxy", message, NotificationEvent.Severity.SUCCESS) }
    call.respond(HttpStatusCode.NoContent)
}

// PUT /api/v1/client/liquidproxy/settings
private fun Route.putSettings() = put("/settings") {
    data class SettingsRequest(val level: Int?, val forwardAuthentication: Boolean?)

    val body = call.receive<SettingsRequest>()
    try {
        call.liquidProxy {
            LiquidProxy.subscription()

            body.level?.let { level ->
                check(LiquidProxy.plans.any { it.level == level }) {
                    translation("liquidbounce.liquidproxy.error.plan").string
                }
                LiquidProxy.level = level
            }
            body.forwardAuthentication?.let { LiquidProxy.forwardAuthentication = it }
            ConfigSystem.store(LiquidProxy)
        }
    } catch (ignored: HttpStatusException) {
        // The player was told why, and the state shows that nothing changed
    }
    call.respondState()
}

// POST /api/v1/client/liquidproxy/connect
private fun Route.postConnect() = post("/connect") {
    data class ConnectRequest(val location: String)

    val body = call.receive<ConnectRequest>()
    try {
        call.liquidProxy {
            LiquidProxy.connect(body.location)
            val locations = LiquidProxy.locations()
            val label = locations.first { it.code == body.location }.label(locations)
            val message = translation("liquidbounce.liquidproxy.connected", label).string
            mc.execute { notification("LiquidProxy", message, NotificationEvent.Severity.SUCCESS) }
        }
    } catch (ignored: HttpStatusException) {
        // The player was told why, and the state shows that nothing changed
    }
    call.respondState()
}

// POST /api/v1/client/liquidproxy/disconnect
private fun Route.postDisconnect() = post("/disconnect") {
    if (LiquidProxy.connectedLocation != null) {
        LiquidProxy.disconnect()
        val message = translation("liquidbounce.liquidproxy.disconnected").string
        mc.execute { notification("LiquidProxy", message, NotificationEvent.Severity.SUCCESS) }
    }
    call.respondState()
}

// POST /api/v1/client/liquidproxy/new-ip
private fun Route.postNewIp() = post("/new-ip") {
    val username = mc.user.name
    val requested = call.liquidProxy { LiquidProxy.requestNewIp(username) }
    val message = if (requested) {
        translation("liquidbounce.liquidproxy.newIp", username).string
    } else {
        translation("liquidbounce.liquidproxy.newIp.already", username).string
    }
    mc.execute { notification("LiquidProxy", message, NotificationEvent.Severity.SUCCESS) }
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
}
