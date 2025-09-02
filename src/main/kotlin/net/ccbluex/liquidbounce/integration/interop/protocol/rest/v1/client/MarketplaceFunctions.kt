/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2025 CCBlueX
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
import kotlinx.coroutines.runBlocking
import net.ccbluex.liquidbounce.api.models.auth.ClientAccount.Companion.EMPTY_ACCOUNT
import net.ccbluex.liquidbounce.api.models.marketplace.MarketplaceItemStatus
import net.ccbluex.liquidbounce.api.models.marketplace.MarketplaceItemType
import net.ccbluex.liquidbounce.api.services.marketplace.MarketplaceApi
import net.ccbluex.liquidbounce.config.gson.interopGson
import net.ccbluex.liquidbounce.features.cosmetic.ClientAccountManager
import net.ccbluex.liquidbounce.features.marketplace.MarketplaceManager
import net.ccbluex.liquidbounce.utils.client.logger
import net.ccbluex.netty.http.model.RequestObject
import net.ccbluex.netty.http.util.httpForbidden
import net.ccbluex.netty.http.util.httpOk

/**
 * GET /api/v1/marketplace
 *
 * Lists marketplace items with optional filtering
 */
fun getMarketplaceItems(requestObject: RequestObject) = runBlocking {
    val page = requestObject.queryParams.getOrDefault("page", "1").toInt()
    val limit = requestObject.queryParams.getOrDefault("limit", "12").toInt()
    val query = requestObject.queryParams["query"]
    val typeStr = requestObject.queryParams["type"]
    val type = typeStr?.let { MarketplaceItemType.valueOf(it.uppercase()) }
    val featured = requestObject.queryParams["featured"]?.toBoolean() ?: true

    val response = MarketplaceApi.getMarketplaceItems(page, limit, query, type, featured)

    val items = response.items.map { item ->
        JsonObject().apply {
            add("item", interopGson.toJsonTree(item))
            addProperty("isSubscribed", MarketplaceManager.isSubscribed(item.id))
        }
    }

    JsonObject().apply {
        add("items", interopGson.toJsonTree(items))
        add("pagination", interopGson.toJsonTree(response.pagination))
    }.let { httpOk(it) }
}

/**
 * GET /api/v1/marketplace/:id
 */
fun getMarketplaceItem(requestObject: RequestObject) = runBlocking {
    val id = requestObject.params["id"]?.toIntOrNull() ?: return@runBlocking httpForbidden("Invalid ID")

    val item = MarketplaceApi.getMarketplaceItem(id)
    JsonObject().apply {
        add("item", interopGson.toJsonTree(item))
        addProperty("isSubscribed", MarketplaceManager.isSubscribed(id))
        addProperty("hasUpdate", false) // TODO: Implement version check
    }.let { httpOk(it) }
}

/**
 * GET /api/v1/marketplace/:id/revisions
 */
fun getMarketplaceItemRevisions(requestObject: RequestObject) = runBlocking {
    val id = requestObject.params["id"]?.toIntOrNull() ?: return@runBlocking httpForbidden("Invalid ID")
    val page = requestObject.queryParams.getOrDefault("page", "1").toInt()
    val limit = requestObject.queryParams.getOrDefault("limit", "10").toInt()

    val response = MarketplaceApi.getMarketplaceItemRevisions(id, page, limit)
    httpOk(interopGson.toJsonTree(response))
}

/**
 * GET /api/v1/marketplace/:id/revisions/:revisionId
 */
fun getMarketplaceItemRevision(requestObject: RequestObject) = runBlocking {
    val id = requestObject.params["id"]?.toIntOrNull() ?: return@runBlocking httpForbidden("Invalid ID")
    val revisionId = requestObject.params["revisionId"]?.toIntOrNull()
        ?: return@runBlocking httpForbidden("Invalid revision ID")

    val response = MarketplaceApi.getMarketplaceItemRevision(id, revisionId)
    httpOk(interopGson.toJsonTree(response))
}

/**
 * POST /api/v1/marketplace/:id/subscribe
 */
fun subscribeMarketplaceItem(requestObject: RequestObject) = runBlocking {
    val id = requestObject.params["id"]?.toIntOrNull() ?: return@runBlocking httpForbidden("Invalid ID")

    try {
        if (MarketplaceManager.isSubscribed(id)) {
            return@runBlocking httpForbidden("Already subscribed")
        }

        // Verify item exists and is active
        val item = MarketplaceApi.getMarketplaceItem(id)
        if (item.status != MarketplaceItemStatus.ACTIVE) {
            return@runBlocking httpForbidden("Item is not active")
        }

        MarketplaceManager.subscribe(item)
        httpOk(interopGson.toJsonTree(item))
    } catch (e: Exception) {
        logger.error("Failed to subscribe to marketplace item", e)
        httpForbidden("Failed to subscribe: ${e.message}")
    }
}

/**
 * POST /api/v1/marketplace/:id/unsubscribe
 */
fun unsubscribeMarketplaceItem(requestObject: RequestObject) = runBlocking {
    val id = requestObject.params["id"]?.toIntOrNull() ?: return@runBlocking httpForbidden("Invalid ID")

    try {
        if (!MarketplaceManager.isSubscribed(id)) {
            return@runBlocking httpForbidden("Not subscribed")
        }

        MarketplaceManager.unsubscribe(id)
        httpOk(interopGson.toJsonTree(requestObject))
    } catch (e: Exception) {
        logger.error("Failed to unsubscribe from marketplace item", e)
        httpForbidden("Failed to unsubscribe: ${e.message}")
    }
}

/**
 * GET /api/v1/marketplace/:id/reviews
 */
fun getMarketplaceItemReviews(requestObject: RequestObject) = runBlocking {
    val id = requestObject.params["id"]?.toIntOrNull() ?: return@runBlocking httpForbidden("Invalid ID")
    val page = requestObject.queryParams.getOrDefault("page", "1").toInt()
    val limit = requestObject.queryParams.getOrDefault("limit", "10").toInt()

    val response = MarketplaceApi.getReviews(id, page, limit)
    httpOk(interopGson.toJsonTree(response))
}

/**
 * POST /api/v1/marketplace/:id/reviews
 */
fun postMarketplaceItemReview(requestObject: RequestObject) = runBlocking {
    data class MarketplaceReview(
        val rating: Int,
        val comment: String
    )

    val id = requestObject.params["id"]?.toIntOrNull() ?: return@runBlocking httpForbidden("Invalid ID")
    val review = requestObject.body.let { interopGson.fromJson(it, MarketplaceReview::class.java) }
        ?: return@runBlocking httpForbidden("Invalid review data")

    val clientAccount = ClientAccountManager.clientAccount
    if (clientAccount == EMPTY_ACCOUNT) {
        return@runBlocking httpForbidden("Not logged in")
    }

    val response = MarketplaceApi.createReview(clientAccount.takeSession(), id, review.rating, review.comment)
    httpOk(interopGson.toJsonTree(response))
}
