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

import io.ktor.server.request.receive
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.api.core.orNotFound
import net.ccbluex.liquidbounce.api.models.auth.OAuthSession
import net.ccbluex.liquidbounce.api.models.marketplace.MarketplaceItem
import net.ccbluex.liquidbounce.api.models.marketplace.MarketplaceItemType
import net.ccbluex.liquidbounce.api.models.pagination.Pagination
import net.ccbluex.liquidbounce.api.services.marketplace.MarketplaceApi
import net.ccbluex.liquidbounce.features.marketplace.autoconfig.ConfigTracker
import net.ccbluex.liquidbounce.features.marketplace.autoconfig.MarketplaceConfigs
import net.ccbluex.liquidbounce.features.marketplace.dependenciesOf
import net.ccbluex.liquidbounce.features.module.ModuleManager
import net.ccbluex.liquidbounce.integration.interop.badRequest
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.text.dropPort

private const val PAGE_SIZE = 20
private const val HISTORY_SIZE = 10

private suspend fun tagIds(names: Collection<String>): List<Int> {
    if (names.isEmpty()) {
        return emptyList()
    }

    val tags = MarketplaceConfigs.tags.ifEmpty { MarketplaceApi.getTags() }
    return names.mapNotNull { name -> tags.find { it.name.equals(name, true) }?.id }
}

/**
 * The config [id] and its newest revision, which loads and reports go to.
 */
private suspend fun liveConfig(id: Int, session: OAuthSession?): Pair<MarketplaceItem, Int> {
    val item = MarketplaceApi.getMarketplaceItem(id, session)
    return item to (item.liveRevisionId ?: error("${item.name} has nothing published"))
}

private fun Route.getTags() = get("/tags") {
    call.respondMarketplace { MarketplaceApi.getTags().map { it.name } }
}

/**
 * A query with a share code finds that config, unlisted ones included. When the featured filter leaves
 * nothing, the page counts the configs it hides.
 */
private fun Route.getConfigs() = get {
    val list = call.listQuery
    val query = list.query
    val parameters = call.queryParameters
    val tags = parameters["tags"]?.split(',')?.map(String::trim)?.filter(String::isNotEmpty).orEmpty()
    val server = if (parameters["server"].toBoolean()) currentServer() else null
    val featured = parameters["featured"].toBoolean()

    call.respondMarketplace {
        if (query != null && query.startsWith(MarketplaceConfigs.SHARE_CODE_PREFIX, ignoreCase = true)) {
            val item = orNotFound { MarketplaceApi.getMarketplaceItemByCode(query) }
            val items = listOfNotNull(item?.takeIf { it.type == MarketplaceItemType.CONFIG })
            return@respondMarketplace PageView(configViews(items), Pagination(1, 1, items.size), true, 0)
        }

        val session = optionalSession()
        val tagIds = tagIds(tags)
        suspend fun page(featured: Boolean?, limit: Int) = MarketplaceConfigs.list(
            page = list.page,
            limit = limit,
            query = query,
            tags = tagIds,
            targetServer = server,
            featured = featured,
            sort = list.sort,
            session = session
        )

        val response = page(featured.takeIf { it }, PAGE_SIZE)
        val unfeatured = if (featured && response.items.isEmpty()) page(null, 1).pagination.items else 0
        PageView(configViews(response.items), response.pagination, false, unfeatured)
    }
}

private fun Route.getConfig() = get {
    val id = call.requireId()
    call.respondMarketplace { configDetail(id) }
}

private suspend fun configDetail(id: Int): ConfigDetailView = coroutineScope {
    val session = optionalSession()
    val item = MarketplaceApi.getMarketplaceItem(id, session)
    val liveRevisionId = item.liveRevisionId
    val config = async { configView(item) }
    val dependencies = async { dependenciesOf(id) }
    // A linked item leaves out the servers the icon comes from
    val configs = async {
        dependencies.await().configs.map { async { linkedConfig(MarketplaceApi.getMarketplaceItem(it.item.id)) } }
    }
    val installs = async { dependencies.await().installables.map { async { itemView(it.item) } } }
    val revisions = async { revisions(item) }
    val forkOf = async {
        item.forkedFromItemId?.let { runCatching { linkedConfig(MarketplaceApi.getMarketplaceItem(it)) }.getOrNull() }
    }
    val report = async {
        if (session != null && liveRevisionId != null) {
            orNotFound { MarketplaceApi.getOwnConfigReport(session, id, liveRevisionId) }?.works
        } else {
            null
        }
    }

    ConfigDetailView(
        config = config.await(),
        description = item.description,
        createdAt = epochMillis(item.createdAt),
        forkOf = forkOf.await(),
        configs = configs.await().awaitAll(),
        installs = installs.await().awaitAll(),
        revisions = revisions.await(),
        changes = liveRevisionId?.takeIf { config.await().overlayOn != null }?.let { ConfigTracker.modules(id, it) },
        report = report.await(),
    )
}

private suspend fun revisions(item: MarketplaceItem): List<ConfigRevisionView> = coroutineScope {
    val reports = async { MarketplaceApi.getConfigReports(item.id).associateBy { it.revisionId } }
    val revisions = MarketplaceApi.getMarketplaceItemRevisions(item.id, 1, HISTORY_SIZE)
    revisions.items.mapIndexed { index, revision ->
        val summary = reports.await()[revision.id]
        ConfigRevisionView(
            id = revision.id,
            createdAt = epochMillis(revision.createdAt),
            changelog = revision.changelog?.takeIf(String::isNotBlank),
            works = summary?.works ?: 0,
            fails = summary?.fails ?: 0,
            latest = revision.id == item.liveRevisionId,
            first = index == revisions.items.lastIndex && revisions.pagination.pages <= 1,
        )
    }
}

/**
 * The modules a load can be restricted to.
 */
private fun Route.getModules() = get("/modules") {
    val id = call.requireId()
    call.respondMarketplace {
        val (item, revisionId) = liveConfig(id, optionalSession())
        ConfigTracker.modulesToLoad(item, revisionId)
    }
}

/**
 * Without modules the whole config loads and is tracked, with them only those modules change.
 */
private fun Route.postLoad() = post("/load") {
    data class LoadRequest(val modules: List<String>?)

    val id = call.requireId()
    val names = call.receive<LoadRequest>().modules
    val modules = names.orEmpty().mapNotNull { ModuleManager[it] }
    if (names != null && modules.isEmpty()) {
        call.badRequest("None of these modules exist")
    }

    call.respondMarketplace {
        val (item, revisionId) = liveConfig(id, optionalSession())
        InstallResult(ConfigTracker.load(item, revisionId, modules).installed.map { it.name })
    }
}

/**
 * Reports the newest revision as working or broken, or withdraws the report without [works], and answers the
 * config with the new counts.
 */
private fun Route.putReport() = put("/report") {
    data class ReportRequest(val works: Boolean?)

    val id = call.requireId()
    val works = call.receive<ReportRequest>().works
    val session = call.requireSession()
    call.respondMarketplace {
        val (_, revisionId) = liveConfig(id, session)
        if (works == null) {
            MarketplaceApi.deleteConfigReport(session, id, revisionId)
        } else {
            val server = mc.currentServer?.ip?.dropPort()
            MarketplaceApi.putConfigReport(session, id, revisionId, works, LiquidBounce.clientVersion, server)
        }
        configView(MarketplaceApi.getMarketplaceItem(id, session))
    }
}

internal fun Route.marketplaceConfigRoutes() = route("/marketplace") {
    getTags()
    route("/configs") {
        getConfigs()
        route("/{id}") {
            getConfig()
            getModules()
            postLoad()
            putReport()
        }
    }
}
