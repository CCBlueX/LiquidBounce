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

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import net.ccbluex.liquidbounce.api.core.httpException
import net.ccbluex.liquidbounce.api.models.marketplace.MarketplaceItem
import net.ccbluex.liquidbounce.api.models.marketplace.MarketplaceItemRevision
import net.ccbluex.liquidbounce.api.models.marketplace.MarketplaceItemType
import net.ccbluex.liquidbounce.api.models.marketplace.MarketplaceItemVisibility
import net.ccbluex.liquidbounce.api.models.marketplace.MarketplaceLinkedItem
import net.ccbluex.liquidbounce.api.models.pagination.Pagination
import net.ccbluex.liquidbounce.api.services.marketplace.MarketplaceApi
import net.ccbluex.liquidbounce.features.addon.AddonInstaller
import net.ccbluex.liquidbounce.features.marketplace.itemStatus
import net.ccbluex.liquidbounce.integration.interop.protocol.rest.v1.game.ServerIcons
import net.ccbluex.liquidbounce.integration.theme.ThemeManager
import net.ccbluex.liquidbounce.utils.client.logger
import net.ccbluex.liquidbounce.utils.client.protocolVersion as clientProtocol

// What the marketplace tab of the ClickGUI shows, and how it comes from the marketplace's models.

private const val SUMMARY_LENGTH = 200
private const val REVIEW_PAGE_SIZE = 100

/**
 * Only a page of configs has [code] and [unfeatured], which tells it apart for the theme.
 */
internal data class PageView<T>(
    val items: List<T>,
    val pagination: Pagination,
    val code: Boolean? = null,
    val unfeatured: Int? = null,
)

/**
 * A config as the tab lists it. [overlayOn] is the config it loads on top of.
 */
internal data class ConfigView(
    val id: Int,
    val name: String,
    val address: String,
    val image: String?,
    val featured: Boolean,
    val binds: Boolean,
    val tags: List<String>,
    val servers: List<String>,
    val protocol: String?,
    val protocolMatches: Boolean,
    val works: Int,
    val fails: Int,
    val downloads: Int,
    val updatedAt: Long?,
    val overlayOn: String?,
    val visibility: MarketplaceItemVisibility?,
)

internal data class LinkedConfig(val id: Int, val address: String, val image: String?, val featured: Boolean)

/**
 * [configs] load before it in this order and [installs] get installed. [changes] are the modules an overlay
 * sets, [report] the own report on the newest revision.
 */
internal data class ConfigDetailView(
    val config: ConfigView,
    val description: String,
    val createdAt: Long?,
    val forkOf: LinkedConfig?,
    val configs: List<LinkedConfig>,
    val installs: List<ItemView>,
    val revisions: List<ConfigRevisionView>,
    val changes: List<String>?,
    val report: Boolean?,
)

internal data class ConfigRevisionView(
    val id: Int,
    val createdAt: Long?,
    val changelog: String?,
    val works: Int,
    val fails: Int,
    val latest: Boolean,
    val first: Boolean,
)

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
 * An add-on, script or theme as the tab lists it. [notFor] is the running LiquidBounce when nothing fits it.
 */
internal data class ItemView(
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

internal data class VersionView(val revision: RevisionView, val installed: Boolean, val fits: Boolean)

/**
 * [liquidbounce] is the running LiquidBounce, which the versions that do not fit are not for.
 */
internal data class ItemDetailView(
    val item: ItemView,
    val description: String,
    val liquidbounce: String,
    val versions: List<VersionView>,
)

internal data class InstalledItem(val id: Int, val type: MarketplaceItemType, val name: String)

internal val MarketplaceItem.displayAddress get() = author?.let { "$it/$name" } ?: name

private val MarketplaceLinkedItem.displayAddress
    get() = (author ?: item.author)?.let { "$it/${item.name}" } ?: item.name

private val MarketplaceItem.thumbnail get() = thumbnailPid?.let(MarketplaceApi::fileUrl)

private suspend fun serverIcon(servers: List<String>?) = servers.orEmpty().firstNotNullOfOrNull { ServerIcons.of(it) }

internal suspend fun configView(item: MarketplaceItem): ConfigView {
    val base = MarketplaceApi.getItemDependencies(item.id).firstOrNull { it.item.type == MarketplaceItemType.CONFIG }
    return ConfigView(
        id = item.id,
        name = item.name,
        address = item.displayAddress,
        image = serverIcon(item.targetServers),
        featured = item.featured,
        binds = item.includesBinds == true,
        tags = item.tags.orEmpty().map { it.name },
        servers = item.targetServers.orEmpty(),
        protocol = item.protocolName?.takeIf(String::isNotEmpty),
        protocolMatches = item.protocolVersion == null || item.protocolVersion == clientProtocol.version,
        works = item.recentWorks,
        fails = item.recentFails,
        downloads = item.downloads,
        updatedAt = epochMillis(item.updatedAt ?: item.createdAt),
        overlayOn = base?.displayAddress,
        visibility = item.visibility,
    )
}

internal suspend fun configViews(items: List<MarketplaceItem>) = coroutineScope {
    items.map { async { configView(it) } }.awaitAll()
}

internal suspend fun linkedConfig(item: MarketplaceItem) =
    LinkedConfig(item.id, item.displayAddress, serverIcon(item.targetServers), item.featured)

internal fun MarketplaceItemRevision.view() = RevisionView(
    id = id,
    version = version,
    liquidbounce = liquidbounce?.toString(),
    createdAt = epochMillis(createdAt),
    changelog = changelog?.takeIf(String::isNotBlank),
)

private val MARKDOWN_IMAGE = Regex("""!\[[^\]]*]\([^)]*\)""")
private val MARKDOWN_LINK = Regex("""\[([^\]]*)]\([^)]*\)""")
// Heading, quote and list marks, emphasis, code and HTML tags
private val MARKDOWN_MARKS =
    Regex("""^(#+|>|[-*+]|\d+\.)\s+|\*\*|__|~~|`|<[^>]+>|(?<!\w)[*_](?=\S)|(?<=\S)[*_](?!\w)""")

/**
 * The first line of the description with text, without its markdown.
 */
private fun MarketplaceItem.summary() = description.lineSequence()
    .map { it.replace(MARKDOWN_IMAGE, "").replace(MARKDOWN_LINK, "$1").replace(MARKDOWN_MARKS, "").trim() }
    .firstOrNull(String::isNotEmpty)
    ?.take(SUMMARY_LENGTH)
    .orEmpty()

internal suspend fun itemView(item: MarketplaceItem): ItemView = coroutineScope {
    val rating = async { rating(item.id) }
    val status = item.itemStatus()

    ItemView(
        id = item.id,
        type = item.type,
        name = item.name,
        author = item.author,
        image = item.thumbnail,
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
 * The average of every review and how many there are. Reviews failing to load leave the item without a
 * rating rather than without the item.
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
