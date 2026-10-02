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
package net.ccbluex.liquidbounce.integration.interop.protocol.rest.v1.client

import com.google.gson.JsonParser
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.httpMethod
import io.ktor.server.response.respond
import kotlinx.coroutines.CancellationException
import net.ccbluex.liquidbounce.api.core.httpException
import net.ccbluex.liquidbounce.api.models.auth.ClientAccount.Companion.EMPTY_ACCOUNT
import net.ccbluex.liquidbounce.api.models.auth.OAuthSession
import net.ccbluex.liquidbounce.api.services.marketplace.MarketplaceApi
import net.ccbluex.liquidbounce.event.events.NotificationEvent
import net.ccbluex.liquidbounce.features.cosmetic.ClientAccountManager
import net.ccbluex.liquidbounce.features.marketplace.NoCompatibleRevisionException
import net.ccbluex.liquidbounce.integration.interop.HttpStatusException
import net.ccbluex.liquidbounce.integration.interop.unauthorized
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.logger
import net.ccbluex.liquidbounce.utils.client.markAsError
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.client.notification
import net.ccbluex.liquidbounce.utils.text.dropPort
import net.ccbluex.liquidbounce.utils.text.rootDomain
import net.minecraft.network.chat.Component
import java.io.IOException
import java.time.LocalDateTime
import java.time.ZoneOffset

// What the marketplace routes of the ClickGUI share: reaching the marketplace, the account and list queries.

/**
 * Runs [block] against the marketplace: an unreachable marketplace answers 503, a refused request
 * the marketplace's own status and reason. The tab shows no errors of its actions, so the player is told those.
 */
internal suspend inline fun <T> ApplicationCall.marketplace(block: () -> T): T = try {
    block()
} catch (e: Exception) {
    val failure = when (e) {
        is HttpStatusException -> e
        is CancellationException -> throw e
        else -> marketplaceFailure(e)
    }
    if (request.httpMethod != HttpMethod.Get) {
        val reason = Component.literal(failure.body["reason"] ?: failure.status.description)
        mc.execute {
            chat(markAsError(reason))
            notification("Marketplace", reason, NotificationEvent.Severity.ERROR)
        }
    }
    throw failure
}

/**
 * Responds with what [block] gets from the marketplace, as [marketplace] does.
 */
internal suspend inline fun <reified T : Any> ApplicationCall.respondMarketplace(block: () -> T) =
    respond(marketplace(block))

internal fun marketplaceFailure(e: Exception): HttpStatusException {
    val http = e.httpException
    return when {
        http != null -> {
            logger.warn("Marketplace request failed: ${http.message}")
            HttpStatusException(
                HttpStatusCode.fromValue(http.code),
                mapOf("reason" to runCatching { JsonParser.parseString(http.content).asJsonObject["error"].asString }
                    .getOrDefault(http.content))
            )
        }

        e is IOException -> HttpStatusException(
            HttpStatusCode.ServiceUnavailable,
            mapOf("reason" to "Can't reach the marketplace")
        )

        e is IllegalStateException || e is IllegalArgumentException || e is NoCompatibleRevisionException ->
            HttpStatusException(HttpStatusCode.BadRequest, mapOf("reason" to (e.message ?: e.javaClass.simpleName)))

        else -> {
            logger.error("Marketplace request failed", e)
            HttpStatusException(
                HttpStatusCode.InternalServerError,
                mapOf("reason" to (e.message ?: e.javaClass.simpleName))
            )
        }
    }
}

internal suspend fun ApplicationCall.requireSession(): OAuthSession {
    val account = ClientAccountManager.clientAccount
    if (account == EMPTY_ACCOUNT) {
        unauthorized("Not logged in")
    }
    return marketplace { account.takeSession() }
}

/**
 * The session of the account, `null` while logged out or when it cannot be renewed, so browsing goes on
 * without it.
 */
internal suspend fun optionalSession(): OAuthSession? {
    val account = ClientAccountManager.clientAccount.takeIf { it != EMPTY_ACCOUNT } ?: return null
    return runCatching { account.takeSession() }
        .onFailure { logger.debug("Failed to renew the session", it) }
        .getOrNull()
}

internal fun currentServer() = mc.currentServer?.ip?.dropPort()?.rootDomain()

/**
 * The API sends UTC without a zone.
 */
internal fun epochMillis(dateTime: String?): Long? = dateTime?.let {
    runCatching { LocalDateTime.parse(it).toInstant(ZoneOffset.UTC).toEpochMilli() }.getOrNull()
}

/**
 * The page, search and order of a list request.
 */
internal class ListQuery(val page: Int, val query: String?, val sort: MarketplaceApi.Sort)

internal val ApplicationCall.listQuery
    get() = with(request.queryParameters) {
        ListQuery(
            page = get("page")?.toIntOrNull() ?: 1,
            query = get("query")?.trim()?.takeIf(String::isNotEmpty),
            sort = if (get("sort") == "new") MarketplaceApi.Sort.CREATED else MarketplaceApi.Sort.SCORE,
        )
    }
