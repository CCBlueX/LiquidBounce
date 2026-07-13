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
import net.ccbluex.netty.http.routing.Routing
import net.ccbluex.netty.http.application.ApplicationCall


/**
 * Extract a required integer path parameter or respond with 403 Forbidden
 */
private suspend fun ApplicationCall.requireId(parameter: String = "id"): Int {
    return parameters[parameter]?.toIntOrNull() ?: forbidden("Invalid $parameter: ${parameters[parameter]}")
}

/**
 * GET /api/v1/marketplace
 *
 * Lists marketplace items with optional filtering
 */
private fun Routing.getMarketplaceItems() = get {
    val page = call.queryParameters.getOrDefault("page", "1").toInt()
    val limit = call.queryParameters.getOrDefault("limit", "12").toInt()
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
 * GET /api/v1/marketplace/:id
 */
private fun Routing.getMarketplaceItem() = get {
    val id = call.requireId()

    val item = MarketplaceApi.getMarketplaceItem(id)
    call.respond(JsonObject().apply {
        add("item", interopGson.toJsonTree(item))
        addProperty("isSubscribed", MarketplaceManager.isSubscribed(id))
        addProperty("hasUpdate", false) // TODO: Implement version check
    })
}

/**
 * GET /api/v1/marketplace/:id/revisions
 */
private fun Routing.getMarketplaceItemRevisions() = get {
    val id = call.requireId()
    val page = call.queryParameters.getOrDefault("page", "1").toInt()
    val limit = call.queryParameters.getOrDefault("limit", "10").toInt()

    val response = MarketplaceApi.getMarketplaceItemRevisions(id, page, limit)
    call.respond(response, interopGson)
}

/**
 * GET /api/v1/marketplace/:id/revisions/:revisionId
 */
private fun Routing.getMarketplaceItemRevision() = get("/:revisionId") {
    val id = call.requireId()
    val revisionId = call.requireId("revisionId")

    val response = MarketplaceApi.getMarketplaceItemRevision(id, revisionId)
    call.respond(response, interopGson)
}

/**
 * POST /api/v1/marketplace/:id/subscribe
 */
private fun Routing.subscribeMarketplaceItem() = post("/subscribe") {
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
        call.respondNoContent()
    } catch (e: Exception) {
        logger.error("Failed to subscribe to marketplace item", e)
        call.forbidden("Failed to subscribe: ${e.message}")
    }
}

/**
 * POST /api/v1/marketplace/:id/unsubscribe
 */
private fun Routing.unsubscribeMarketplaceItem() = post("/unsubscribe") {
    val id = call.requireId()

    if (!MarketplaceManager.isSubscribed(id)) {
        call.forbidden("Not subscribed")
    }

    try {
        MarketplaceManager.unsubscribe(id)
        call.respondNoContent()
    } catch (e: Exception) {
        logger.error("Failed to unsubscribe from marketplace item", e)
        call.forbidden("Failed to unsubscribe: ${e.message}")
    }
}

/**
 * GET /api/v1/marketplace/:id/reviews
 */
private fun Routing.getMarketplaceItemReviews() = get {
    val id = call.requireId()
    val page = call.queryParameters.getOrDefault("page", "1").toInt()
    val limit = call.queryParameters.getOrDefault("limit", "10").toInt()

    val response = MarketplaceApi.getReviews(id, page, limit)
    call.respond(response, interopGson)
}

/**
 * POST /api/v1/marketplace/:id/reviews
 */
private fun Routing.postMarketplaceItemReview() = post {
    data class MarketplaceReview(
        val rating: Int,
        val comment: String
    )

    val id = call.requireId()
    val review = call.body.let { interopGson.fromJson(it, MarketplaceReview::class.java) }
        ?: call.forbidden("Invalid review data")

    val clientAccount = ClientAccountManager.clientAccount
    if (clientAccount == EMPTY_ACCOUNT) {
        call.forbidden("Not logged in")
    }

    val response = MarketplaceApi.createReview(clientAccount.takeSession(), id, review.rating, review.comment)
    call.respond(response, interopGson)
}

internal fun Routing.marketplaceRoutes() = route("/marketplace") {
    getMarketplaceItems()
    route("/:id") {
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
