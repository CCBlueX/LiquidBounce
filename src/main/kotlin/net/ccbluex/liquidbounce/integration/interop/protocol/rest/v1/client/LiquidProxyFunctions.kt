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

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import kotlinx.coroutines.CancellationException
import net.ccbluex.liquidbounce.api.core.httpException
import net.ccbluex.liquidbounce.api.models.liquidproxy.ProxyLocation
import net.ccbluex.liquidbounce.api.models.liquidproxy.ProxyNetwork
import net.ccbluex.liquidbounce.api.models.liquidproxy.ProxySession
import net.ccbluex.liquidbounce.api.services.auth.isInvalidGrant
import net.ccbluex.liquidbounce.event.events.NotificationEvent.Severity
import net.ccbluex.liquidbounce.features.misc.proxy.liquidproxy.LiquidProxy
import net.ccbluex.liquidbounce.features.misc.proxy.liquidproxy.LiquidProxyException
import net.ccbluex.liquidbounce.features.misc.proxy.liquidproxy.LocationPings
import net.ccbluex.liquidbounce.features.misc.proxy.liquidproxy.ProxySessions
import net.ccbluex.liquidbounce.integration.interop.badRequest
import net.ccbluex.liquidbounce.integration.interop.protocol.rest.v1.game.ServerIcons
import net.ccbluex.liquidbounce.integration.interop.serviceUnavailable
import net.ccbluex.liquidbounce.lang.translation
import net.ccbluex.liquidbounce.utils.client.logger
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.client.notification
import java.io.IOException
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/*
 * LiquidProxy endpoints. The theme words nothing itself: the state brings a notice when LiquidProxy can't be
 * used, a change answers with what it changed, and a notification says how it went.
 */

private fun text(key: String, vararg args: Any?) = translation(key, *args).string

/**
 * Like [runCatching], except that a cancelled request stays cancelled.
 */
private inline fun <T> attempt(block: () -> T): Result<T> = runCatching(block).onFailure { e ->
    if (e is CancellationException) {
        throw e
    }
    logger.warn("LiquidProxy: $e")
}

/**
 * Why a request to LiquidProxy failed, in words for the player.
 */
private val Throwable.reason: String
    get() {
        val http = httpException
        return when {
            this is LiquidProxyException -> message.orEmpty()
            http?.code == HttpStatusCode.Unauthorized.value || http?.isInvalidGrant == true ->
                text("liquidbounce.liquidproxy.error.loginExpired")
            http != null -> text("liquidbounce.liquidproxy.error.status", http.code)
            this is IOException -> text("liquidbounce.liquidproxy.error.offline")
            else -> toString()
        }
    }

/**
 * Tells the player how [change] went: with the message it returns, if any, or with why it failed.
 */
private suspend fun announce(change: suspend () -> String?) {
    val (message, severity) = attempt { change() }
        .fold({ it to Severity.SUCCESS }, { it.reason to Severity.ERROR })

    if (message != null) {
        mc.execute { notification("LiquidProxy", message, severity) }
    }
}

/**
 * Responds with what [read] returns, or with why it could not be read.
 */
private suspend inline fun <reified T : Any> ApplicationCall.respondOrUnavailable(read: () -> T) =
    respond(attempt(read).getOrElse { serviceUnavailable(it.reason) })

private enum class Status { LOGGED_OUT, UNREACHABLE, NO_SUBSCRIPTION, EXPIRED, UNAVAILABLE, ACTIVE }

/**
 * What the main panel says in place of the sessions.
 */
private class Notice(val title: String, val text: String)

/**
 * The translation of [key] as the title, over that of `key.text`.
 */
private fun notice(key: String, vararg args: Any?) = Notice(text(key), text("$key.text", *args))

private class State(
    val status: Status,
    val notice: Notice?,
    val networks: List<ProxyNetwork> = emptyList(),
    val network: String? = null,
    /** The location the current proxy goes through */
    val connected: String? = null,
    val forwardAuthentication: Boolean = LiquidProxy.forwardAuthentication,
    /** The location chosen last */
    val location: String? = LiquidProxy.location.ifEmpty { null },
)

private val LONG_DATE = DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).withZone(ZoneId.systemDefault())

private suspend fun ApplicationCall.respondState(refresh: Boolean = false) {
    val connected = attempt { LiquidProxy.connectedLocation() }.getOrNull()?.code
    val subscription = attempt { LiquidProxy.subscription(refresh) }.getOrElse {
        val notice = Notice(text("liquidbounce.liquidproxy.unreachable"), it.reason)
        return respond(State(Status.UNREACHABLE, notice, connected = connected))
    }

    val entitlement = subscription?.entitlement
    val (status, notice) = when {
        LiquidProxy.account == null -> Status.LOGGED_OUT to notice("liquidbounce.liquidproxy.loggedOut")
        entitlement == null -> Status.NO_SUBSCRIPTION to notice("liquidbounce.liquidproxy.subscription.none")
        entitlement.isActive -> Status.ACTIVE to null
        entitlement.isExpired -> Status.EXPIRED to
            notice("liquidbounce.liquidproxy.subscription.ended", LONG_DATE.format(entitlement.accessUntil))
        else -> Status.UNAVAILABLE to notice("liquidbounce.liquidproxy.subscription.unavailable")
    }

    respond(State(
        status,
        notice,
        networks = subscription?.networks.orEmpty(),
        network = subscription?.network?.code,
        connected = connected,
    ))
}

// GET /api/v1/client/liquidproxy
private fun Route.getState() = get {
    call.respondState(refresh = call.request.queryParameters["refresh"] == "true")
}

// PUT /api/v1/client/liquidproxy/settings
private fun Route.putSettings() = put("/settings") {
    data class Settings(val network: String?, val forwardAuthentication: Boolean?)

    val settings = call.receive<Settings>()
    announce {
        LiquidProxy.configure(settings.network, settings.forwardAuthentication)
        null
    }
    call.respondState()
}

// POST /api/v1/client/liquidproxy/connect
private fun Route.postConnect() = post("/connect") {
    data class ConnectRequest(val location: String)

    val body = call.receive<ConnectRequest>()
    announce {
        val location = LiquidProxy.connect(body.location)
        text("liquidbounce.liquidproxy.connected", location.name)
    }
    call.respondState()
}

// POST /api/v1/client/liquidproxy/disconnect
private fun Route.postDisconnect() = post("/disconnect") {
    announce {
        if (LiquidProxy.disconnect()) text("liquidbounce.liquidproxy.disconnected") else null
    }
    call.respondState()
}

private enum class LocationStatus { ONLINE, MAINTENANCE, UNREACHABLE }

private class LocationInfo(location: ProxyLocation, val latency: Int?) {
    val code = location.code
    val name = location.name
    val latitude = location.latitude
    val longitude = location.longitude
    val status = when {
        location.maintenance -> LocationStatus.MAINTENANCE
        latency == null -> LocationStatus.UNREACHABLE
        else -> LocationStatus.ONLINE
    }
}

// GET /api/v1/client/liquidproxy/locations
private fun Route.getLocations() = get("/locations") {
    call.respondOrUnavailable {
        val locations = LiquidProxy.locations(refresh = true)
        val pings = LocationPings.measure(locations)
        locations.map { location -> LocationInfo(location, pings[location.code]) }
    }
}

private class SessionInfo(session: ProxySession, val icon: String?, val canChangeIp: Boolean) {
    val id = session.id
    val player = session.player
    val server = session.rootHost
    val country = session.exitCountry
    val ipType = session.ipType
    val connected = session.connected
    val startedAt = session.startedAt.toEpochMilli()
    val endedAt = (session.endedAt ?: session.lastSeenAt).takeUnless { session.connected }?.toEpochMilli()
}

private suspend fun ApplicationCall.respondSessions() = respondOrUnavailable {
    // A saved IP can be changed once, on the newest session that got it
    val savedIps = ProxySessions.savedIps().toMutableSet()

    ProxySessions.latest().map { session ->
        SessionInfo(session, ServerIcons.of(session.host), canChangeIp = savedIps.remove(session.savedIp))
    }
}

// GET /api/v1/client/liquidproxy/sessions
private fun Route.getSessions() = get("/sessions") {
    call.respondSessions()
}

// POST /api/v1/client/liquidproxy/sessions/{id}/end
private fun Route.postEndSession() = post("/sessions/{id}/end") {
    val id = call.parameters["id"] ?: call.badRequest("Missing session")
    announce {
        ProxySessions.end(id)
        text("liquidbounce.liquidproxy.sessionEnded")
    }
    call.respondSessions()
}

// POST /api/v1/client/liquidproxy/sessions/{id}/new-ip
private fun Route.postNewIp() = post("/sessions/{id}/new-ip") {
    val id = call.parameters["id"] ?: call.badRequest("Missing session")
    announce {
        val session = ProxySessions.changeIp(id)
        text("liquidbounce.liquidproxy.newIp", session.player, session.rootHost)
    }
    call.respondSessions()
}

internal fun Route.liquidProxyRoutes() = route("/liquidproxy") {
    getState()
    putSettings()
    postConnect()
    postDisconnect()
    getLocations()
    getSessions()
    postEndSession()
    postNewIp()
}
