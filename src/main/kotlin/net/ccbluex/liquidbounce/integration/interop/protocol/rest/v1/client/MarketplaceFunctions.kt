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
import net.ccbluex.liquidbounce.api.models.auth.ClientAccount.Companion.EMPTY_ACCOUNT
import net.ccbluex.liquidbounce.api.models.marketplace.MarketplaceItemStatus
import net.ccbluex.liquidbounce.api.models.marketplace.MarketplaceItemType
import net.ccbluex.liquidbounce.api.services.marketplace.MarketplaceApi
import net.ccbluex.liquidbounce.config.gson.interopGson
import net.ccbluex.liquidbounce.features.cosmetic.ClientAccountManager
import net.ccbluex.liquidbounce.features.marketplace.MarketplaceManager
import net.ccbluex.liquidbounce.utils.client.logger
import net.ccbluex.netty.http.routing.RoutingContext

/**
 * GET /api/v1/marketplace
 *
 * Lists marketplace items with optional filtering
 */
suspend fun RoutingContext.getMarketplaceItems() {
    val page = queryParameters.getOrDefault("page", "1").toInt()
    val limit = queryParameters.getOrDefault("limit", "12").toInt()
    val query = queryParameters["query"]
    val typeStr = queryParameters["type"]
    val type = typeStr?.let { MarketplaceItemType.valueOf(it.uppercase()) }
    val featured = queryParameters["featured"]?.toBoolean() ?: true

    val response = MarketplaceApi.getMarketplaceItems(page, limit, query, type, featured)

    val items = response.items.map { item ->
        JsonObject().apply {
            add("item", interopGson.toJsonTree(item))
            addProperty("isSubscribed", MarketplaceManager.isSubscribed(item.id))
        }
    }

    respond(JsonObject().apply {
        add("items", interopGson.toJsonTree(items))
        add("pagination", interopGson.toJsonTree(response.pagination))
    })
}

/**
 * GET /api/v1/marketplace/:id
 */
suspend fun RoutingContext.getMarketplaceItem() {
    val id = parameters["id"]?.toIntOrNull() ?: forbidden("Invalid ID")

    val item = MarketplaceApi.getMarketplaceItem(id)
    respond(JsonObject().apply {
        add("item", interopGson.toJsonTree(item))
        addProperty("isSubscribed", MarketplaceManager.isSubscribed(id))
        addProperty("hasUpdate", false) // TODO: Implement version check
    })
}

/**
 * GET /api/v1/marketplace/:id/revisions
 */
suspend fun RoutingContext.getMarketplaceItemRevisions() {
    val id = parameters["id"]?.toIntOrNull() ?: forbidden("Invalid ID")
    val page = queryParameters.getOrDefault("page", "1").toInt()
    val limit = queryParameters.getOrDefault("limit", "10").toInt()

    val response = MarketplaceApi.getMarketplaceItemRevisions(id, page, limit)
    respond(response, interopGson)
}

/**
 * GET /api/v1/marketplace/:id/revisions/:revisionId
 */
suspend fun RoutingContext.getMarketplaceItemRevision() {
    val id = parameters["id"]?.toIntOrNull() ?: forbidden("Invalid ID")
    val revisionId = parameters["revisionId"]?.toIntOrNull()
        ?: forbidden("Invalid revision ID")

    val response = MarketplaceApi.getMarketplaceItemRevision(id, revisionId)
    respond(response, interopGson)
}

/**
 * POST /api/v1/marketplace/:id/subscribe
 */
suspend fun RoutingContext.subscribeMarketplaceItem() {
    val id = parameters["id"]?.toIntOrNull() ?: forbidden("Invalid ID")

    if (MarketplaceManager.isSubscribed(id)) {
        forbidden("Already subscribed")
    }

    val item = try {
        MarketplaceApi.getMarketplaceItem(id)
    } catch (e: Exception) {
        logger.error("Failed to load marketplace item before subscribing", e)
        forbidden("Failed to subscribe: ${e.message}")
    }

    if (item.status != MarketplaceItemStatus.ACTIVE) {
        forbidden("Item is not active")
    }

    try {
        MarketplaceManager.subscribe(item)
        respondNoContent()
    } catch (e: Exception) {
        logger.error("Failed to subscribe to marketplace item", e)
        forbidden("Failed to subscribe: ${e.message}")
    }
}

/**
 * POST /api/v1/marketplace/:id/unsubscribe
 */
suspend fun RoutingContext.unsubscribeMarketplaceItem() {
    val id = parameters["id"]?.toIntOrNull() ?: forbidden("Invalid ID")

    if (!MarketplaceManager.isSubscribed(id)) {
        forbidden("Not subscribed")
    }

    try {
        MarketplaceManager.unsubscribe(id)
        respondNoContent()
    } catch (e: Exception) {
        logger.error("Failed to unsubscribe from marketplace item", e)
        forbidden("Failed to unsubscribe: ${e.message}")
    }
}

/**
 * GET /api/v1/marketplace/:id/reviews
 */
suspend fun RoutingContext.getMarketplaceItemReviews() {
    val id = parameters["id"]?.toIntOrNull() ?: forbidden("Invalid ID")
    val page = queryParameters.getOrDefault("page", "1").toInt()
    val limit = queryParameters.getOrDefault("limit", "10").toInt()

    val response = MarketplaceApi.getReviews(id, page, limit)
    respond(response, interopGson)
}

/**
 * POST /api/v1/marketplace/:id/reviews
 */
suspend fun RoutingContext.postMarketplaceItemReview() {
    data class MarketplaceReview(
        val rating: Int,
        val comment: String
    )

    val id = parameters["id"]?.toIntOrNull() ?: forbidden("Invalid ID")
    val review = body.let { interopGson.fromJson(it, MarketplaceReview::class.java) }
        ?: forbidden("Invalid review data")

    val clientAccount = ClientAccountManager.clientAccount
    if (clientAccount == EMPTY_ACCOUNT) {
        forbidden("Not logged in")
    }

    val response = MarketplaceApi.createReview(clientAccount.takeSession(), id, review.rating, review.comment)
    respond(response, interopGson)
}
