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
import io.ktor.server.request.receive
import io.ktor.server.response.respond
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
import net.ccbluex.liquidbounce.api.models.marketplace.MarketplaceItem
import net.ccbluex.liquidbounce.api.models.marketplace.MarketplaceItemType
import net.ccbluex.liquidbounce.api.models.pagination.Pagination
import net.ccbluex.liquidbounce.api.services.marketplace.MarketplaceApi
import net.ccbluex.liquidbounce.features.command.brigadier.CmdI18n
import net.ccbluex.liquidbounce.features.command.commands.client.config.reportInstalled
import net.ccbluex.liquidbounce.features.marketplace.autoconfig.ConfigTracker
import net.ccbluex.liquidbounce.features.marketplace.autoconfig.MarketplaceConfigs
import net.ccbluex.liquidbounce.features.marketplace.getDependencies
import net.ccbluex.liquidbounce.features.module.ModuleManager
import net.ccbluex.liquidbounce.integration.interop.badRequest
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.client.variable
import net.ccbluex.liquidbounce.utils.text.dropPort

private const val PAGE_SIZE = 20
private const val HISTORY_SIZE = 10

private val configI18n = object : CmdI18n {
    override val path = "config"
}

private suspend fun findTagIds(names: Collection<String>): List<Int> {
    if (names.isEmpty()) {
        return emptyList()
    }

    val tags = MarketplaceConfigs.tags.ifEmpty { MarketplaceApi.getTags() }
    return names.mapNotNull { name -> tags.find { it.name.equals(name, true) }?.id }
}

/**
 * The config and its newest revision, the one loads and reports go to.
 */
private suspend fun getLiveConfig(id: Int): Pair<MarketplaceItem, Int> {
    val item = MarketplaceApi.getMarketplaceItem(id)
    return item to (item.liveRevision?.id ?: error("${item.name} has nothing published"))
}

private fun Route.getTags() = get("/tags") {
    call.respondMarketplace { MarketplaceApi.getTags().map { it.name } }
}

private fun Route.getConfigs() = get {
    val list = call.listRequest
    val parameters = call.queryParameters
    val tags = parameters["tags"]?.split(',')?.map(String::trim)?.filter(String::isNotEmpty).orEmpty()
    val server = if (parameters["server"].toBoolean()) getCurrentServerDomain() else null
    val featuredOnly = parameters["featured"].toBoolean()

    call.respondMarketplace {
        val code = list.query?.takeIf { it.startsWith(MarketplaceConfigs.SHARE_CODE_PREFIX, ignoreCase = true) }
        if (code != null) {
            return@respondMarketplace findSharedConfig(code)
        }

        val tagIds = findTagIds(tags)
        suspend fun page(featured: Boolean?, limit: Int = PAGE_SIZE) = MarketplaceConfigs.list(
            page = list.page,
            limit = limit,
            query = list.query,
            tags = tagIds,
            targetServer = server,
            featured = featured,
            sort = list.sort
        )

        val response = page(featured = if (featuredOnly) true else null)
        val unfeatured = if (featuredOnly && response.items.isEmpty()) {
            page(featured = null, limit = 1).pagination.items
        } else {
            0
        }
        ConfigPageData(response.items.toConfigData(), response.pagination, code = false, unfeatured)
    }
}

/**
 * A share code also finds unlisted configs.
 */
private suspend fun findSharedConfig(code: String): ConfigPageData {
    val item = orNotFound { MarketplaceApi.getMarketplaceItemByCode(code) }
    val items = listOfNotNull(item?.takeIf { it.type == MarketplaceItemType.CONFIG })
    return ConfigPageData(items.toConfigData(), Pagination(1, 1, items.size), code = true, unfeatured = 0)
}

private fun Route.getConfig() = get {
    val id = call.requireId()
    call.respondMarketplace { getConfigDetail(id) }
}

private suspend fun getConfigDetail(id: Int): ConfigDetailData = coroutineScope {
    val item = MarketplaceApi.getMarketplaceItem(id)
    val liveRevisionId = item.liveRevision?.id
    val config = async { item.toConfigData() }
    val dependencies = getDependencies(item)
    val configs = async { dependencies.configs.map { async { it.item.toLinkedConfigData() } }.awaitAll() }
    val installs = async { dependencies.installables.map { async { it.item.toItemData() } }.awaitAll() }
    val revisions = async { getRevisionHistory(item) }
    val forkOf = async {
        // Fetched for the servers its icon comes from.
        item.forkedFrom?.let { forkedFrom ->
            runCatching { MarketplaceApi.getMarketplaceItem(forkedFrom.id).toLinkedConfigData() }.getOrNull()
        }
    }
    val report = async {
        val session = getSessionOrNull()
        if (session == null || liveRevisionId == null) {
            return@async null
        }
        orNotFound { MarketplaceApi.getOwnConfigReport(session, liveRevisionId) }?.works
    }
    val isOverlay = config.await().overlayOn != null

    ConfigDetailData(
        config = config.await(),
        description = item.description,
        createdAt = parseEpochMillis(item.createdAt),
        forkOf = forkOf.await(),
        configs = configs.await(),
        installs = installs.await(),
        revisions = revisions.await(),
        changes = liveRevisionId?.takeIf { isOverlay }?.let { ConfigTracker.getModuleNames(id, it) },
        report = report.await(),
    )
}

private suspend fun getRevisionHistory(item: MarketplaceItem): List<ConfigRevisionData> {
    val revisions = MarketplaceApi.getMarketplaceItemRevisions(item.id, 1, HISTORY_SIZE)
    return revisions.items.mapIndexed { index, revision ->
        ConfigRevisionData(
            id = revision.id,
            createdAt = parseEpochMillis(revision.createdAt),
            changelog = revision.changelog?.takeIf(String::isNotBlank),
            works = revision.reports?.works ?: 0,
            fails = revision.reports?.fails ?: 0,
            latest = revision.id == item.liveRevision?.id,
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
        val (item, revisionId) = getLiveConfig(id)
        ConfigTracker.getLoadableModuleNames(item, revisionId)
    }
}

/**
 * Without modules the whole config loads and is tracked. With them only those modules change.
 */
private fun Route.postLoad() = post("/load") {
    data class LoadRequest(val modules: List<String>?)

    call.handleMarketplaceErrors {
        val id = call.requireId()
        val names = call.receive<LoadRequest>().modules
        val modules = names.orEmpty().mapNotNull { ModuleManager[it] }
        if (names != null && modules.isEmpty()) {
            call.badRequest("None of these modules exist")
        }

        val (item, revisionId) = getLiveConfig(id)
        val result = ConfigTracker.load(item, revisionId, modules)
        sendFeedback(configI18n.t("load.loaded", variable(item.name)))
        mc.execute { configI18n.reportInstalled(result) }
    }
    call.respond(HttpStatusCode.NoContent)
}

/**
 * Reports the newest revision as working or broken. Without `works` the report is withdrawn.
 */
private fun Route.putReport() = put("/report") {
    data class ReportRequest(val works: Boolean?)

    call.respondMarketplace {
        val id = call.requireId()
        val works = call.receive<ReportRequest>().works
        val session = call.requireSession()
        val (item, revisionId) = getLiveConfig(id)
        if (works == null) {
            MarketplaceApi.deleteConfigReport(session, revisionId)
        } else {
            val server = mc.currentServer?.ip?.dropPort()
            MarketplaceApi.putConfigReport(session, revisionId, works, LiquidBounce.clientVersion, server)
            sendFeedback(configI18n.t("report.reported", variable(item.name)))
        }
        MarketplaceApi.getMarketplaceItem(id).toConfigData()
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
