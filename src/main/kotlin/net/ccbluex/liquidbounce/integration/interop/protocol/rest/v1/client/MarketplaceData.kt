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
import net.ccbluex.liquidbounce.features.marketplace.getInstallState
import net.ccbluex.liquidbounce.integration.interop.protocol.rest.v1.game.ServerIcons
import net.ccbluex.liquidbounce.integration.theme.ThemeManager
import net.ccbluex.liquidbounce.utils.client.logger
import net.ccbluex.liquidbounce.utils.client.protocolVersion as clientProtocol

private const val REVIEW_PAGE_SIZE = 100

internal class PageData<T>(val items: List<T>, val pagination: Pagination)

/**
 * @param code the query was a share code
 * @param unfeatured how many configs the featured filter hides when it leaves nothing
 */
internal class ConfigPageData(
    val items: List<ConfigData>,
    val pagination: Pagination,
    val code: Boolean,
    val unfeatured: Int,
)

/**
 * @param overlayOn the config it loads on top of
 */
internal class ConfigData(
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

internal class LinkedConfigData(val id: Int, val address: String, val image: String?, val featured: Boolean)

/**
 * @param configs loaded before it, in this order
 * @param installs add-ons and scripts loading it installs
 * @param changes the modules an overlay sets
 * @param report the player's own report on the newest revision
 */
internal class ConfigDetailData(
    val config: ConfigData,
    val description: String,
    val createdAt: Long?,
    val forkOf: LinkedConfigData?,
    val configs: List<LinkedConfigData>,
    val installs: List<ItemData>,
    val revisions: List<ConfigRevisionData>,
    val changes: List<String>?,
    val report: Boolean?,
)

internal class ConfigRevisionData(
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
internal class RevisionData(
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
internal class ItemData(
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
    val installed: RevisionData?,
    val update: Boolean,
    val restartRequired: Boolean,
    val inUse: Boolean,
)

internal class VersionData(val revision: RevisionData, val installed: Boolean, val fits: Boolean)

/**
 * @param liquidbounce the running LiquidBounce
 */
internal class ItemDetailData(
    val item: ItemData,
    val description: String,
    val liquidbounce: String,
    val versions: List<VersionData>,
)

internal class InstalledItemData(val id: Int, val type: MarketplaceItemType, val name: String)

internal val clientVersionTag get() = "v${AddonInstaller.liquidbounce}"

private val MarketplaceLinkedItem.address
    get() = (author ?: item.author)?.let { "$it/${item.name}" } ?: item.name

private suspend fun MarketplaceItem.findServerIcon() =
    targetServers.orEmpty().firstNotNullOfOrNull { ServerIcons.of(it) }

internal suspend fun MarketplaceItem.toConfigData(): ConfigData {
    val base = MarketplaceApi.getItemDependencies(id).firstOrNull { it.item.type == MarketplaceItemType.CONFIG }
    return ConfigData(
        id = id,
        name = name,
        address = address,
        image = findServerIcon(),
        featured = featured,
        binds = includesBinds == true,
        tags = tags.orEmpty().map { it.name },
        servers = targetServers.orEmpty(),
        protocol = protocolName?.takeIf(String::isNotEmpty),
        protocolMatches = protocolVersion == null || protocolVersion == clientProtocol.version,
        works = recentWorks,
        fails = recentFails,
        downloads = downloads,
        updatedAt = parseEpochMillis(updatedAt ?: createdAt),
        overlayOn = base?.address,
        visibility = visibility,
    )
}

internal suspend fun List<MarketplaceItem>.toConfigData() = coroutineScope {
    map { async { it.toConfigData() } }.awaitAll()
}

internal suspend fun MarketplaceItem.toLinkedConfigData() = LinkedConfigData(id, address, findServerIcon(), featured)

internal fun MarketplaceItemRevision.toRevisionData() = RevisionData(
    id = id,
    version = version,
    liquidbounce = liquidbounce?.toString(),
    createdAt = parseEpochMillis(createdAt),
    changelog = changelog?.takeIf(String::isNotBlank),
)

internal suspend fun MarketplaceItem.toItemData(): ItemData = coroutineScope {
    val rating = async { getRating(id) }
    val state = getInstallState()

    ItemData(
        id = id,
        type = type,
        name = name,
        author = author,
        image = thumbnailPid?.let(MarketplaceApi::fileUrl),
        summary = description.lineSequence().map(String::trim).firstOrNull(String::isNotEmpty).orEmpty(),
        featured = featured,
        downloads = downloads,
        rating = rating.await().average,
        reviews = rating.await().count,
        subscribed = state.subscribed,
        installable = state.installable,
        notFor = clientVersionTag.takeIf { state.incompatible },
        installed = state.installedRevision?.toRevisionData(),
        update = state.hasUpdate,
        restartRequired = state.restartRequired,
        inUse = ThemeManager.marketplaceThemes[id]?.let { it === ThemeManager.theme } == true,
    )
}

private class Rating(val average: Double?, val count: Int)

/**
 * Reviews failing to load leave the item without a rating rather than failing the item.
 */
private suspend fun getRating(id: Int): Rating {
    val ratings = try {
        collectAllPages { page -> MarketplaceApi.getReviews(id, page, REVIEW_PAGE_SIZE) }.map { it.rating }
    } catch (e: Exception) {
        if (e.httpException == null) throw e
        logger.debug("Failed to load the reviews of marketplace item $id", e)
        emptyList()
    }
    return Rating(ratings.average().takeIf { ratings.isNotEmpty() }, ratings.size)
}
