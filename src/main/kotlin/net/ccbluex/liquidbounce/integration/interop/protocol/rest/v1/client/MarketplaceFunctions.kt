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

import com.google.gson.JsonObject
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import net.ccbluex.liquidbounce.api.models.auth.ClientAccount.Companion.EMPTY_ACCOUNT
import net.ccbluex.liquidbounce.api.models.marketplace.MarketplaceItemStatus
import net.ccbluex.liquidbounce.api.models.marketplace.MarketplaceItemType
import net.ccbluex.liquidbounce.api.services.marketplace.MarketplaceApi
import net.ccbluex.liquidbounce.config.gson.interopGson
import net.ccbluex.liquidbounce.features.cosmetic.ClientAccountManager
import net.ccbluex.liquidbounce.features.marketplace.MarketplaceManager
import net.ccbluex.liquidbounce.integration.interop.forbidden
import net.ccbluex.liquidbounce.utils.client.logger


/**
 * Extract a required integer path parameter or respond with 403 Forbidden
 */
private suspend fun ApplicationCall.requireId(parameter: String = "id"): Int {
    return parameters[parameter]?.toIntOrNull() ?: this.forbidden("Invalid $parameter: ${parameters[parameter]}")
}

/**
 * GET /api/v1/marketplace
 *
 * Lists marketplace items with optional filtering
 */
private fun Route.getMarketplaceItems() = get {
    val page = call.queryParameters["page"]?.toIntOrNull() ?: 1
    val limit = call.queryParameters["limit"]?.toIntOrNull() ?: 12
    val query = call.queryParameters["query"]
    val typeStr = call.queryParameters["type"]
    val type = typeStr?.let { MarketplaceItemType.valueOf(it.uppercase()) }
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
 * GET /api/v1/marketplace/{id}
 */
private fun Route.getMarketplaceItem() = get {
    val id = call.requireId()

    val item = MarketplaceApi.getMarketplaceItem(id)
    call.respond(JsonObject().apply {
        add("item", interopGson.toJsonTree(item))
        addProperty("isSubscribed", MarketplaceManager.isSubscribed(id))
        addProperty("hasUpdate", false) // TODO: Implement version check
    })
}

/**
 * GET /api/v1/marketplace/{id}/revisions
 */
private fun Route.getMarketplaceItemRevisions() = get {
    val id = call.requireId()
    val page = call.queryParameters["page"]?.toIntOrNull() ?: 1
    val limit = call.queryParameters["limit"]?.toIntOrNull() ?: 10

    val response = MarketplaceApi.getMarketplaceItemRevisions(id, page, limit)
    call.respond(interopGson.toJsonTree(response))
}

/**
 * GET /api/v1/marketplace/{id}/revisions/{revisionId}
 */
private fun Route.getMarketplaceItemRevision() = get("/{revisionId}") {
    val id = call.requireId()
    val revisionId = call.requireId("revisionId")

    val response = MarketplaceApi.getMarketplaceItemRevision(id, revisionId)
    call.respond(interopGson.toJsonTree(response))
}

/**
 * POST /api/v1/marketplace/{id}/subscribe
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
 * POST /api/v1/marketplace/{id}/unsubscribe
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
 * GET /api/v1/marketplace/{id}/reviews
 */
private fun Route.getMarketplaceItemReviews() = get {
    val id = call.requireId()
    val page = call.queryParameters["page"]?.toIntOrNull() ?: 1
    val limit = call.queryParameters["limit"]?.toIntOrNull() ?: 10

    val response = MarketplaceApi.getReviews(id, page, limit)
    call.respond(interopGson.toJsonTree(response))
}

/**
 * POST /api/v1/marketplace/{id}/reviews
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
    call.respond(interopGson.toJsonTree(response))
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
