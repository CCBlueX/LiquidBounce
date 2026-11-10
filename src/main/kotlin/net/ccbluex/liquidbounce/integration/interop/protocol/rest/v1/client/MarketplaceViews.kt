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
import net.ccbluex.liquidbounce.features.marketplace.autoconfig.MarketplaceConfigs.address
import net.ccbluex.liquidbounce.features.marketplace.itemStatus
import net.ccbluex.liquidbounce.integration.interop.protocol.rest.v1.game.ServerIcons
import net.ccbluex.liquidbounce.integration.theme.ThemeManager
import net.ccbluex.liquidbounce.utils.client.logger
import net.ccbluex.liquidbounce.utils.client.protocolVersion as clientProtocol

private const val REVIEW_PAGE_SIZE = 100

internal class PageView<T>(val items: List<T>, val pagination: Pagination)

/**
 * @param code the query was a share code
 * @param unfeatured how many configs the featured filter hides when it leaves nothing
 */
internal class ConfigPageView(
    val items: List<ConfigView>,
    val pagination: Pagination,
    val code: Boolean,
    val unfeatured: Int,
)

/**
 * @param overlayOn the config it loads on top of
 */
internal class ConfigView(
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

internal class LinkedConfig(val id: Int, val address: String, val image: String?, val featured: Boolean)

/**
 * @param configs loaded before it, in this order
 * @param installs add-ons and scripts loading it installs
 * @param changes the modules an overlay sets
 * @param report the player's own report on the newest revision
 */
internal class ConfigDetailView(
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

internal class ConfigRevisionView(
    val id: Int,
    val createdAt: Long?,
    val changelog: String?,
    val works: Int,
    val fails: Int,
    val latest: Boolean,
    val first: Boolean,
)

/**
 * @param liquidbounce the LiquidBounce versions an add-on revision works with
 */
internal class RevisionView(
    val id: Int,
    val version: String,
    val liquidbounce: String?,
    val createdAt: Long?,
    val changelog: String?,
)

/**
 * @param summary the first line of the description, as markdown
 * @param notFor the running LiquidBounce, when no revision is for it
 */
internal class ItemView(
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

internal class VersionView(val revision: RevisionView, val installed: Boolean, val fits: Boolean)

/**
 * @param liquidbounce the running LiquidBounce
 */
internal class ItemDetailView(
    val item: ItemView,
    val description: String,
    val liquidbounce: String,
    val versions: List<VersionView>,
)

internal class InstalledItem(val id: Int, val type: MarketplaceItemType, val name: String)

internal val runningLiquidBounce get() = "v${AddonInstaller.liquidbounce}"

private val MarketplaceLinkedItem.address
    get() = (author ?: item.author)?.let { "$it/${item.name}" } ?: item.name

private suspend fun MarketplaceItem.serverIcon() = targetServers.orEmpty().firstNotNullOfOrNull { ServerIcons.of(it) }

internal suspend fun configView(item: MarketplaceItem): ConfigView {
    val base = MarketplaceApi.getItemDependencies(item.id).firstOrNull { it.item.type == MarketplaceItemType.CONFIG }
    return ConfigView(
        id = item.id,
        name = item.name,
        address = item.address,
        image = item.serverIcon(),
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
        overlayOn = base?.address,
        visibility = item.visibility,
    )
}

internal suspend fun configViews(items: List<MarketplaceItem>) = coroutineScope {
    items.map { async { configView(it) } }.awaitAll()
}

internal suspend fun linkedConfig(item: MarketplaceItem) =
    LinkedConfig(item.id, item.address, item.serverIcon(), item.featured)

internal fun MarketplaceItemRevision.view() = RevisionView(
    id = id,
    version = version,
    liquidbounce = liquidbounce?.toString(),
    createdAt = epochMillis(createdAt),
    changelog = changelog?.takeIf(String::isNotBlank),
)

internal suspend fun itemView(item: MarketplaceItem): ItemView = coroutineScope {
    val rating = async { rating(item.id) }
    val status = item.itemStatus()

    ItemView(
        id = item.id,
        type = item.type,
        name = item.name,
        author = item.author,
        image = item.thumbnailPid?.let(MarketplaceApi::fileUrl),
        summary = item.description.lineSequence().map(String::trim).firstOrNull(String::isNotEmpty).orEmpty(),
        featured = item.featured,
        downloads = item.downloads,
        rating = rating.await().average,
        reviews = rating.await().count,
        subscribed = status.subscribed,
        installable = status.installable,
        notFor = runningLiquidBounce.takeIf { status.incompatible },
        installed = status.installed?.view(),
        update = status.hasUpdate,
        restartRequired = status.restartRequired,
        inUse = ThemeManager.marketplaceThemes[item.id]?.let { it === ThemeManager.theme } == true,
    )
}

private class Rating(val average: Double?, val count: Int)

/**
 * Reviews failing to load leave the item without a rating rather than failing the item.
 */
private suspend fun rating(id: Int): Rating {
    val ratings = try {
        allPages { page -> MarketplaceApi.getReviews(id, page, REVIEW_PAGE_SIZE) }.map { it.rating }
    } catch (e: Exception) {
        if (e.httpException == null) throw e
        logger.debug("Failed to load the reviews of marketplace item $id", e)
        emptyList()
    }
    return Rating(ratings.average().takeIf { ratings.isNotEmpty() }, ratings.size)
}
