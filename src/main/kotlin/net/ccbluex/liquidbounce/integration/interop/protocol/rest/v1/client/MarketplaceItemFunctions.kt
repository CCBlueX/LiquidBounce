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
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import net.ccbluex.liquidbounce.api.core.httpException
import net.ccbluex.liquidbounce.api.models.marketplace.MarketplaceItem
import net.ccbluex.liquidbounce.api.models.marketplace.MarketplaceItemRevision
import net.ccbluex.liquidbounce.api.models.marketplace.MarketplaceItemType
import net.ccbluex.liquidbounce.api.models.pagination.Pagination
import net.ccbluex.liquidbounce.api.services.marketplace.MarketplaceApi
import net.ccbluex.liquidbounce.config.ConfigSystem
import net.ccbluex.liquidbounce.features.addon.AddonInstaller
import net.ccbluex.liquidbounce.features.marketplace.MarketplaceManager
import net.ccbluex.liquidbounce.features.marketplace.UpdateResult
import net.ccbluex.liquidbounce.features.marketplace.checkStatus
import net.ccbluex.liquidbounce.features.marketplace.installWithDependencies
import net.ccbluex.liquidbounce.integration.interop.badRequest
import net.ccbluex.liquidbounce.integration.interop.notFound
import net.ccbluex.liquidbounce.integration.theme.ThemeManager
import net.ccbluex.liquidbounce.utils.client.logger
import net.ccbluex.liquidbounce.utils.kotlin.MinecraftDispatcher

private const val PAGE_SIZE = 20
private const val REVIEW_PAGE_SIZE = 100
private const val REVISION_PAGE_SIZE = 50
private const val SUMMARY_LENGTH = 200

/**
 * [liquidbounce] are the LiquidBounce versions an add-on revision works with.
 */
internal data class RevisionView(
    val id: Int,
    val version: String,
    val liquidbounce: String?,
    val createdAt: Long?,
    val changelog: String?,
)

/**
 * An add-on, script or theme as the marketplace tab lists it. [notFor] is the running LiquidBounce
 * when nothing fits it.
 */
internal data class ItemRow(
    val id: Int,
    val type: MarketplaceItemType,
    val name: String,
    val author: String?,
    val image: String?,
    val summary: String,
    val featured: Boolean,
    val downloads: Int,
    val rating: Double?,
    val reviews: Int,
    val subscribed: Boolean,
    val installable: Boolean,
    val notFor: String?,
    val installed: RevisionView?,
    val update: Boolean,
    val restartRequired: Boolean,
    val inUse: Boolean,
)

private data class ItemPage(val items: List<ItemRow>, val pagination: Pagination)

private data class VersionRow(val revision: RevisionView, val installed: Boolean, val fits: Boolean)

/**
 * [liquidbounce] is the version of the running LiquidBounce, which the versions that do not fit are not for.
 */
private data class ItemDetail(
    val item: ItemRow,
    val description: String,
    val liquidbounce: String,
    val versions: List<VersionRow>,
)

private data class InstallResult(val installed: List<String>)

private data class InstalledItem(val id: Int, val type: MarketplaceItemType, val name: String)

internal fun MarketplaceItemRevision.view() = RevisionView(
    id = id,
    version = version,
    liquidbounce = liquidbounce?.toString(),
    createdAt = epochMillis(createdAt),
    changelog = changelog?.takeIf(String::isNotBlank),
)

internal fun MarketplaceItem.summary() = description.lineSequence()
    .map(String::trim)
    .firstOrNull { it.isNotEmpty() && !it.startsWith("![") }
    ?.removePrefix("#")?.trim()
    ?.take(SUMMARY_LENGTH)
    .orEmpty()

internal suspend fun itemRow(item: MarketplaceItem): ItemRow = coroutineScope {
    val rating = async { rating(item.id) }
    val status = item.checkStatus()

    ItemRow(
        id = item.id,
        type = item.type,
        name = item.name,
        author = item.author,
        image = item.thumbnailPid?.let(MarketplaceApi::fileUrl),
        summary = item.summary(),
        featured = item.featured,
        downloads = item.downloads,
        rating = rating.await().first,
        reviews = rating.await().second,
        subscribed = status.subscribed,
        installable = status.installable,
        notFor = if (status.notFor) "v${AddonInstaller.liquidbounce}" else null,
        installed = status.installed?.view(),
        update = status.hasUpdate,
        restartRequired = status.restartRequired,
        inUse = ThemeManager.marketplaceThemes[item.id]?.let { it === ThemeManager.theme } == true,
    )
}

/**
 * The average of every review and how many there are. Reviews failing to load leave the row without
 * a rating rather than without the row.
 */
private suspend fun rating(id: Int): Pair<Double?, Int> {
    val ratings = mutableListOf<Int>()
    var page = 1
    try {
        do {
            val reviews = MarketplaceApi.getReviews(id, page, REVIEW_PAGE_SIZE)
            reviews.items.mapTo(ratings) { it.rating }
        } while (page++ < reviews.pagination.pages)
    } catch (e: Exception) {
        if (e.httpException == null) throw e
        logger.debug("Failed to load the reviews of marketplace item $id", e)
        return null to 0
    }
    return ratings.average().takeIf { ratings.isNotEmpty() } to ratings.size
}

private fun Route.getItems() = get {
    val parameters = call.queryParameters
    val type = MarketplaceItemType.entries.find { it.tag.equals(parameters["type"], true) && it.isSubscribable }
        ?: call.badRequest("Unknown type ${parameters["type"]}")
    val page = parameters["page"]?.toIntOrNull() ?: 1
    val query = parameters["query"]?.trim()?.takeIf(String::isNotEmpty)
    val sort = if (parameters["sort"] == "new") MarketplaceApi.Sort.CREATED else MarketplaceApi.Sort.SCORE

    val response = call.marketplace {
        val response = MarketplaceApi.getMarketplaceItems(
            page = page,
            limit = PAGE_SIZE,
            query = query,
            type = type,
            filter = MarketplaceApi.Filter(sort = sort)
        )
        val items = coroutineScope { response.items.map { async { itemRow(it) } }.awaitAll() }
        ItemPage(items, response.pagination)
    }
    call.respond(response)
}

private fun Route.getItem() = get {
    val id = call.requireId()
    val detail = call.marketplace {
        val item = MarketplaceApi.getMarketplaceItem(id)
        ItemDetail(itemRow(item), item.description, "v${AddonInstaller.liquidbounce}", versions(item))
    }
    call.respond(detail)
}

/**
 * Every revision, newest first. For an add-on, only those the marketplace offers for this game fit.
 */
private suspend fun versions(item: MarketplaceItem): List<VersionRow> {
    val revisions = revisions(item.id)
    val fitting = if (item.type == MarketplaceItemType.ADDON) {
        revisions(item.id, AddonInstaller.minecraft, AddonInstaller.liquidbounce).mapTo(HashSet()) { it.id }
    } else {
        null
    }
    val installed = MarketplaceManager.getItem(item.id)?.installedRevisionId

    return revisions.map { revision ->
        VersionRow(revision.view(), revision.id == installed, fitting == null || revision.id in fitting)
    }
}

private suspend fun revisions(
    id: Int,
    minecraft: String? = null,
    liquidbounce: String? = null
): List<MarketplaceItemRevision> {
    val revisions = mutableListOf<MarketplaceItemRevision>()
    var page = 1
    do {
        val response = MarketplaceApi.getMarketplaceItemRevisions(id, page, REVISION_PAGE_SIZE, minecraft, liquidbounce)
        revisions += response.items
    } while (page++ < response.pagination.pages)
    return revisions
}

private fun Route.postInstall() = post("/install") {
    val id = call.requireId()
    val result = call.marketplace {
        installWithDependencies(MarketplaceApi.getMarketplaceItem(id))
    }
    call.respond(InstallResult(result.installed.map { it.name }))
}

private fun Route.postUpdate() = post("/update") {
    val id = call.requireId()
    val subscribed = MarketplaceManager.getItem(id) ?: call.notFound(id.toString(), "Not installed")
    when (val result = call.marketplace { MarketplaceManager.update(subscribed) }) {
        is UpdateResult.Updated, is UpdateResult.NoUpdate ->
            call.respond(call.marketplace { itemRow(MarketplaceApi.getMarketplaceItem(id)) })
        is UpdateResult.Incompatible -> call.badRequest(result.unavailable.text().string)
        is UpdateResult.Failed -> throw marketplaceFailure(result.error as? Exception ?: Exception(result.error))
    }
}

private fun Route.postRemove() = post("/remove") {
    val id = call.requireId()
    if (!MarketplaceManager.isSubscribed(id)) {
        call.notFound(id.toString(), "Not installed")
    }

    call.marketplace { MarketplaceManager.unsubscribe(id) }
    call.respond(HttpStatusCode.NoContent)
}

private fun Route.postApply() = post("/apply") {
    val id = call.requireId()
    val theme = ThemeManager.marketplaceThemes[id] ?: call.notFound(id.toString(), "Theme not loaded")

    withContext(MinecraftDispatcher) {
        ThemeManager.theme = theme
        ConfigSystem.store(ThemeManager)
    }
    call.respond(HttpStatusCode.NoContent)
}

/**
 * What is installed, without asking the marketplace.
 */
private fun Route.getInstalled() = get("/installed") {
    call.respond(MarketplaceManager.subscribedItems.map { item -> InstalledItem(item.id, item.type, item.name) })
}

internal fun Route.marketplaceItemRoutes() = route("/marketplace/items") {
    getItems()
    getInstalled()
    route("/{id}") {
        getItem()
        postInstall()
        postUpdate()
        postRemove()
        postApply()
    }
}
