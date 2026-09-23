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

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import kotlinx.coroutines.CancellationException
import net.ccbluex.liquidbounce.api.core.httpException
import net.ccbluex.liquidbounce.api.models.auth.ClientAccount.Companion.EMPTY_ACCOUNT
import net.ccbluex.liquidbounce.api.models.auth.OAuthSession
import net.ccbluex.liquidbounce.api.models.marketplace.MarketplaceItemStatus
import net.ccbluex.liquidbounce.api.models.marketplace.MarketplaceItemType
import net.ccbluex.liquidbounce.api.models.user.UserInformation
import net.ccbluex.liquidbounce.api.services.marketplace.MarketplaceApi
import net.ccbluex.liquidbounce.config.gson.interopGson
import net.ccbluex.liquidbounce.features.cosmetic.ClientAccountManager
import net.ccbluex.liquidbounce.features.marketplace.MarketplaceManager
import net.ccbluex.liquidbounce.features.marketplace.NoCompatibleRevisionException
import net.ccbluex.liquidbounce.integration.interop.HttpStatusException
import net.ccbluex.liquidbounce.integration.interop.forbidden
import net.ccbluex.liquidbounce.integration.interop.unauthorized
import net.ccbluex.liquidbounce.utils.client.logger
import java.io.IOException
import java.time.LocalDateTime
import java.time.ZoneOffset

/**
 * Extract a required integer path parameter or respond with 403 Forbidden
 */
internal suspend fun ApplicationCall.requireId(parameter: String = "id"): Int {
    return parameters[parameter]?.toIntOrNull() ?: this.forbidden("Invalid $parameter: ${parameters[parameter]}")
}

/**
 * Runs [block] against the marketplace: an unreachable marketplace answers 503, a refused request
 * the marketplace's own status and reason.
 */
internal suspend inline fun <T> ApplicationCall.marketplace(block: () -> T): T = try {
    block()
} catch (e: CancellationException) {
    throw e
} catch (e: Exception) {
    throw marketplaceFailure(e)
}

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

/**
 * The marketplace user of the account, `null` while logged out or unknown.
 */
internal suspend fun ownUser(): UserInformation? {
    val account = ClientAccountManager.clientAccount.takeIf { it != EMPTY_ACCOUNT } ?: return null
    if (account.userInformation == null) {
        runCatching { account.updateInfo() }.onFailure { logger.debug("Failed to load the account", it) }
    }
    return account.userInformation
}

internal suspend fun ownUserId() = ownUser()?.userId

/**
 * The API sends UTC without a zone.
 */
internal fun epochMillis(dateTime: String?): Long? = dateTime?.let {
    runCatching { LocalDateTime.parse(it).toInstant(ZoneOffset.UTC).toEpochMilli() }.getOrNull()
}

/**
 * GET /api/v1/client/marketplace
 *
 * Lists marketplace items with optional filtering
 */
private fun Route.getMarketplaceItems() = get {
    val page = call.queryParameters["page"]?.toIntOrNull() ?: 1
    val limit = call.queryParameters["limit"]?.toIntOrNull() ?: 12
    val query = call.queryParameters["query"]
    val typeStr = call.queryParameters["type"]
    val type = typeStr?.let { name ->
        MarketplaceItemType.entries.find { it.tag.equals(name, ignoreCase = true) }
            ?: return@get call.respondText(
                "Unknown marketplace item type '$name'",
                status = HttpStatusCode.BadRequest,
            )
    }
    val featured = call.queryParameters["featured"]?.toBoolean() ?: true

    val response = MarketplaceApi.getMarketplaceItems(page, limit, query, type, featured)

    val items = response.items.map { item ->
        JsonObject().apply {
            add("item", interopGson.toJsonTree(item))
            addProperty("isSubscribed", MarketplaceManager.isSubscribed(item.id))
        }
    }

    call.respond(JsonObject().apply {
        add("items", interopGson.toJsonTree(items))
        add("pagination", interopGson.toJsonTree(response.pagination))
    })
}

/**
 * GET /api/v1/client/marketplace/{id}
 */
private fun Route.getMarketplaceItem() = get {
    val id = call.requireId()

    val item = MarketplaceApi.getMarketplaceItem(id)
    val subscribed = MarketplaceManager.getItem(id)
    val hasUpdate = subscribed != null && runCatching { subscribed.hasUpdate() }
        .onFailure { logger.warn("Failed to check marketplace item $id for updates", it) }
        .getOrDefault(false)

    call.respond(JsonObject().apply {
        add("item", interopGson.toJsonTree(item))
        addProperty("isSubscribed", subscribed != null)
        addProperty("hasUpdate", hasUpdate)
    })
}

/**
 * GET /api/v1/client/marketplace/{id}/revisions
 */
private fun Route.getMarketplaceItemRevisions() = get {
    val id = call.requireId()
    val page = call.queryParameters["page"]?.toIntOrNull() ?: 1
    val limit = call.queryParameters["limit"]?.toIntOrNull() ?: 10

    val response = MarketplaceApi.getMarketplaceItemRevisions(id, page, limit)
    call.respond(response)
}

/**
 * GET /api/v1/client/marketplace/{id}/revisions/{revisionId}
 */
private fun Route.getMarketplaceItemRevision() = get("/{revisionId}") {
    val id = call.requireId()
    val revisionId = call.requireId("revisionId")

    val response = MarketplaceApi.getMarketplaceItemRevision(id, revisionId)
    call.respond(response)
}

/**
 * POST /api/v1/client/marketplace/{id}/subscribe
 */
private fun Route.subscribeMarketplaceItem() = post("/subscribe") {
    val id = call.requireId()

    if (MarketplaceManager.isSubscribed(id)) {
        call.forbidden("Already subscribed")
    }

    val item = try {
        MarketplaceApi.getMarketplaceItem(id)
    } catch (e: Exception) {
        logger.error("Failed to load marketplace item before subscribing", e)
        call.forbidden("Failed to subscribe: ${e.message}")
    }

    if (item.status != MarketplaceItemStatus.ACTIVE) {
        call.forbidden("Item is not active")
    }

    try {
        MarketplaceManager.subscribe(item)
        call.respond(io.ktor.http.HttpStatusCode.NoContent)
    } catch (e: Exception) {
        logger.error("Failed to subscribe to marketplace item", e)
        call.forbidden("Failed to subscribe: ${e.message}")
    }
}

/**
 * POST /api/v1/client/marketplace/{id}/unsubscribe
 */
private fun Route.unsubscribeMarketplaceItem() = post("/unsubscribe") {
    val id = call.requireId()

    if (!MarketplaceManager.isSubscribed(id)) {
        call.forbidden("Not subscribed")
    }

    try {
        MarketplaceManager.unsubscribe(id)
        call.respond(io.ktor.http.HttpStatusCode.NoContent)
    } catch (e: Exception) {
        logger.error("Failed to unsubscribe from marketplace item", e)
        call.forbidden("Failed to unsubscribe: ${e.message}")
    }
}

/**
 * GET /api/v1/client/marketplace/{id}/reviews
 */
private fun Route.getMarketplaceItemReviews() = get {
    val id = call.requireId()
    val page = call.queryParameters["page"]?.toIntOrNull() ?: 1
    val limit = call.queryParameters["limit"]?.toIntOrNull() ?: 10

    val response = MarketplaceApi.getReviews(id, page, limit)
    call.respond(response)
}

/**
 * POST /api/v1/client/marketplace/{id}/reviews
 */
private fun Route.postMarketplaceItemReview() = post {
    data class MarketplaceReview(
        val rating: Int,
        val comment: String
    )

    val id = call.requireId()
    val review = call.receive<MarketplaceReview>()

    val clientAccount = ClientAccountManager.clientAccount
    if (clientAccount == EMPTY_ACCOUNT) {
        call.forbidden("Not logged in")
    }

    val response = MarketplaceApi.createReview(clientAccount.takeSession(), id, review.rating, review.comment)
    call.respond(response)
}

internal fun Route.marketplaceRoutes() = route("/marketplace") {
    getMarketplaceItems()
    route("/{id}") {
        getMarketplaceItem()
        route("/revisions") {
            getMarketplaceItemRevisions()
            getMarketplaceItemRevision()
        }
        subscribeMarketplaceItem()
        unsubscribeMarketplaceItem()
        route("/reviews") {
            getMarketplaceItemReviews()
            postMarketplaceItemReview()
        }
    }
}
