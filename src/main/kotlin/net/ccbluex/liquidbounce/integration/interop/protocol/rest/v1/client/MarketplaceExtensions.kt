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

import com.google.gson.JsonParser
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.httpMethod
import io.ktor.server.response.respond
import kotlinx.coroutines.CancellationException
import net.ccbluex.liquidbounce.api.core.HttpException
import net.ccbluex.liquidbounce.api.core.httpException
import net.ccbluex.liquidbounce.api.models.auth.ClientAccount.Companion.EMPTY_ACCOUNT
import net.ccbluex.liquidbounce.api.models.auth.OAuthSession
import net.ccbluex.liquidbounce.api.models.pagination.PaginatedResponse
import net.ccbluex.liquidbounce.api.services.marketplace.MarketplaceApi
import net.ccbluex.liquidbounce.event.events.NotificationEvent.Severity
import net.ccbluex.liquidbounce.features.cosmetic.ClientAccountManager
import net.ccbluex.liquidbounce.features.marketplace.NoCompatibleRevisionException
import net.ccbluex.liquidbounce.integration.interop.HttpStatusException
import net.ccbluex.liquidbounce.integration.interop.unauthorized
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.logger
import net.ccbluex.liquidbounce.utils.client.markAsError
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.client.notification
import net.ccbluex.liquidbounce.utils.client.regular
import net.ccbluex.liquidbounce.utils.text.asText
import net.ccbluex.liquidbounce.utils.text.dropPort
import net.ccbluex.liquidbounce.utils.text.rootDomain
import net.minecraft.network.chat.MutableComponent
import java.io.IOException
import java.time.LocalDateTime
import java.time.ZoneOffset

/**
 * Turns what goes wrong in [block] into an [HttpStatusException]. Themes show no errors for actions, so a
 * failed one is announced to the player.
 */
internal suspend inline fun <T> ApplicationCall.handleMarketplaceErrors(block: () -> T): T = try {
    block()
} catch (e: Exception) {
    val failure = when (e) {
        is HttpStatusException -> e
        is CancellationException -> throw e
        else -> e.toHttpStatusException()
    }
    if (request.httpMethod != HttpMethod.Get) {
        sendFeedback((failure.body["reason"] ?: failure.status.description).asText(), Severity.ERROR)
    }
    throw failure
}

internal suspend inline fun <reified T : Any> ApplicationCall.respondMarketplace(block: () -> T) =
    respond(handleMarketplaceErrors(block))

internal fun Exception.toHttpStatusException(): HttpStatusException {
    val http = httpException
    val reason = message ?: javaClass.simpleName
    return when {
        http != null -> {
            logger.warn("Marketplace request failed: ${http.message}")
            createStatusException(HttpStatusCode.fromValue(http.code), http.reason)
        }

        this is IOException -> createStatusException(HttpStatusCode.ServiceUnavailable, "Can't reach the marketplace")

        this is IllegalStateException || this is IllegalArgumentException || this is NoCompatibleRevisionException ->
            createStatusException(HttpStatusCode.BadRequest, reason)

        else -> {
            logger.error("Marketplace request failed", this)
            createStatusException(HttpStatusCode.InternalServerError, reason)
        }
    }
}

private fun createStatusException(status: HttpStatusCode, reason: String) =
    HttpStatusException(status, mapOf("reason" to reason))

/**
 * The marketplace answers errors as `{"error": "..."}`.
 */
private val HttpException.reason
    get() = runCatching { JsonParser.parseString(content).asJsonObject["error"].asString }.getOrDefault(content)

/**
 * Tells the player how an action went, in the chat and as a notification, like the commands do.
 */
internal fun sendFeedback(message: MutableComponent, severity: Severity = Severity.SUCCESS) = mc.execute {
    chat(if (severity == Severity.ERROR) markAsError(message) else regular(message))
    notification("Marketplace", message, severity)
}

internal suspend fun ApplicationCall.requireSession(): OAuthSession {
    val account = ClientAccountManager.clientAccount
    if (account == EMPTY_ACCOUNT) {
        unauthorized("Not logged in")
    }
    return handleMarketplaceErrors { account.takeSession() }
}

/**
 * `null` while logged out or when the session cannot be renewed, so browsing goes on without it.
 */
internal suspend fun getSessionOrNull(): OAuthSession? {
    val account = ClientAccountManager.clientAccount.takeIf { it != EMPTY_ACCOUNT } ?: return null
    return runCatching { account.takeSession() }
        .onFailure { logger.debug("Failed to renew the session", it) }
        .getOrNull()
}

/**
 * The marketplace user id of the account, `null` while logged out or unknown.
 */
internal suspend fun getOwnUserIdOrNull(): String? {
    val account = ClientAccountManager.clientAccount.takeIf { it != EMPTY_ACCOUNT } ?: return null
    if (account.userInformation == null) {
        runCatching { account.updateInfo() }.onFailure { logger.debug("Failed to load the account", it) }
    }
    return account.userInformation?.userId
}

internal fun getCurrentServerDomain() = mc.currentServer?.ip?.dropPort()?.rootDomain()

/**
 * The marketplace sends UTC without a zone.
 */
internal fun parseEpochMillis(dateTime: String?): Long? = dateTime?.let {
    runCatching { LocalDateTime.parse(it).toInstant(ZoneOffset.UTC).toEpochMilli() }.getOrNull()
}

private val LINK = Regex("""https?://[^\s<>"']+""", RegexOption.IGNORE_CASE)
private const val LINK_TRAILING = ".,;:!?)]*_~"

/**
 * Whether [text] links to [url]. Punctuation behind a link may belong to the sentence around it.
 */
internal fun hasLink(text: String, url: String) = LINK.findAll(text).any { match ->
    match.value.startsWith(url) && match.value.substring(url.length).all { it in LINK_TRAILING }
}

internal suspend inline fun <T> collectAllPages(fetch: (page: Int) -> PaginatedResponse<T>): List<T> {
    val items = mutableListOf<T>()
    var page = 1
    do {
        val response = fetch(page)
        items += response.items
    } while (page++ < response.pagination.pages)
    return items
}

internal class ListRequest(val page: Int, val query: String?, val sort: MarketplaceApi.Sort)

internal val ApplicationCall.listRequest
    get() = with(request.queryParameters) {
        ListRequest(
            page = get("page")?.toIntOrNull() ?: 1,
            query = get("query")?.trim()?.takeIf(String::isNotEmpty),
            sort = if (get("sort") == "new") MarketplaceApi.Sort.CREATED else MarketplaceApi.Sort.SCORE,
        )
    }
