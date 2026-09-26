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

import com.google.gson.JsonObject
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.patch
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.api.core.orNotFound
import net.ccbluex.liquidbounce.api.models.auth.OAuthSession
import net.ccbluex.liquidbounce.api.models.marketplace.MarketplaceItem
import net.ccbluex.liquidbounce.api.models.marketplace.MarketplaceItemType
import net.ccbluex.liquidbounce.api.models.marketplace.MarketplaceItemVisibility
import net.ccbluex.liquidbounce.api.models.marketplace.MarketplaceLinkedItem
import net.ccbluex.liquidbounce.api.models.pagination.Pagination
import net.ccbluex.liquidbounce.api.services.marketplace.MarketplaceApi
import net.ccbluex.liquidbounce.features.marketplace.autoconfig.ConfigTracker
import net.ccbluex.liquidbounce.features.marketplace.autoconfig.MarketplaceConfigs
import net.ccbluex.liquidbounce.features.marketplace.installNeedsRestart
import net.ccbluex.liquidbounce.features.marketplace.resolveDependencies
import net.ccbluex.liquidbounce.features.module.ModuleManager
import net.ccbluex.liquidbounce.features.module.modules.misc.ModuleAutoConfig
import net.ccbluex.liquidbounce.integration.interop.badRequest
import net.ccbluex.liquidbounce.integration.interop.forbidden
import net.ccbluex.liquidbounce.integration.interop.protocol.rest.v1.game.ServerIcons
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.client.protocolVersion as clientProtocol
import net.ccbluex.liquidbounce.utils.kotlin.MinecraftDispatcher
import net.ccbluex.liquidbounce.utils.text.dropPort
import net.ccbluex.liquidbounce.utils.text.rootDomain

private const val PAGE_SIZE = 20
private const val HISTORY_SIZE = 10

private data class Context(
    val server: String?,
    val autoConfig: Boolean,
    val onlyFeatured: Boolean,
    val user: String?,
)

private data class LinkedConfig(val id: Int, val address: String)

/**
 * A config as the marketplace tab lists it. [tracking] is the tracker's state when it runs this
 * config, [overlayOn] the config it loads on top of.
 */
private data class ConfigRow(
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
    val overlayOn: LinkedConfig?,
    val tracking: ConfigTracker.State,
    val own: Boolean,
    val visibility: MarketplaceItemVisibility?,
)

/**
 * [unfeatured] counts the configs the featured filter hides when it leaves the page empty.
 */
private data class ConfigPage(
    val items: List<ConfigRow>,
    val pagination: Pagination,
    val code: Boolean,
    val unfeatured: Int,
)

private data class ConfigFilter(
    val page: Int,
    val query: String?,
    val tags: List<String>,
    val server: String?,
    val featured: Boolean,
    val sort: MarketplaceApi.Sort,
)

/**
 * A dependency of a config: another config, [order] in the load order, or an add-on or script with
 * its [status].
 */
private data class DependencyRow(
    val id: Int,
    val type: MarketplaceItemType,
    val address: String,
    val image: String?,
    val order: Int?,
    val featured: Boolean,
    val protocol: String?,
    val protocolMatches: Boolean,
    val status: ItemRow?,
)

private data class RevisionRow(
    val id: Int,
    val createdAt: Long?,
    val changelog: String?,
    val works: Int,
    val fails: Int,
    val latest: Boolean,
    val loaded: Boolean,
    val first: Boolean,
)

/**
 * [changes] are the modules an overlay sets, [report] the own report on the newest revision.
 */
private data class ConfigDetail(
    val config: ConfigRow,
    val summary: String,
    val description: String,
    val createdAt: Long?,
    val shareCode: String?,
    val forkOf: LinkedConfig?,
    val dependencies: List<DependencyRow>,
    val revisions: List<RevisionRow>,
    val changes: List<String>?,
    val report: Boolean?,
)

private data class PlanInstall(
    val id: Int,
    val type: MarketplaceItemType,
    val name: String,
    val author: String?,
    val image: String?,
    val revision: RevisionView,
    val restart: Boolean,
)

private data class PlanLeftOut(
    val id: Int,
    val type: MarketplaceItemType,
    val name: String,
    val image: String?,
)

private data class LoadPlanView(
    val installs: List<PlanInstall>,
    val leftOut: List<PlanLeftOut>,
    val modules: List<String>,
)

private data class LoadResultView(val installed: List<String>)

private data class ReportView(val works: Int, val fails: Int, val report: Boolean?)

/**
 * [edited] are the modules changed since the config was applied.
 */
private data class TrackerView(
    val state: ConfigTracker.State,
    val id: Int,
    val address: String,
    val image: String?,
    val own: Boolean,
    val backup: Boolean,
    val edited: List<String>,
)

private data class Published(
    val id: Int,
    val address: String,
    val visibility: MarketplaceItemVisibility?,
    val shareCode: String?,
)

private val MarketplaceItem.displayAddress get() = author?.let { "$it/$name" } ?: name

private val MarketplaceLinkedItem.displayAddress
    get() = (author ?: item.author)?.let { "$it/${item.name}" } ?: item.name

private val MarketplaceItem.protocolMatches
    get() = protocolVersion == null || protocolVersion == clientProtocol.version

private fun currentServer() = mc.currentServer?.ip?.dropPort()?.rootDomain()

private suspend fun serverIcon(servers: List<String>?) = servers.orEmpty().firstNotNullOfOrNull { ServerIcons.of(it) }

private suspend fun tagIds(names: Collection<String>): List<Int> {
    if (names.isEmpty()) {
        return emptyList()
    }

    val tags = MarketplaceConfigs.tags.ifEmpty { MarketplaceApi.getTags() }
    return names.mapNotNull { name -> tags.find { it.name.equals(name, true) }?.id }
}

private fun visibility(name: String) = MarketplaceItemVisibility.entries.find { it.name.equals(name, true) }

private fun tracking(id: Int) =
    if (ConfigTracker.state != ConfigTracker.State.NONE && ConfigTracker.itemId == id) {
        ConfigTracker.state
    } else {
        ConfigTracker.State.NONE
    }

private suspend fun configRow(item: MarketplaceItem, userId: String?): ConfigRow {
    val base = MarketplaceApi.getItemDependencies(item.id).firstOrNull { it.item.type == MarketplaceItemType.CONFIG }
    return ConfigRow(
        id = item.id,
        name = item.name,
        address = item.displayAddress,
        image = serverIcon(item.targetServers),
        featured = item.featured,
        binds = item.includesBinds == true,
        tags = item.tags.orEmpty().map { it.name },
        servers = item.targetServers.orEmpty(),
        protocol = item.protocolName?.takeIf(String::isNotEmpty),
        protocolMatches = item.protocolMatches,
        works = item.recentWorks,
        fails = item.recentFails,
        downloads = item.downloads,
        updatedAt = epochMillis(item.updatedAt ?: item.createdAt),
        overlayOn = base?.let { LinkedConfig(it.item.id, it.displayAddress) },
        tracking = tracking(item.id),
        own = userId != null && item.uid == userId,
        visibility = item.visibility,
    )
}

private suspend fun configRows(items: List<MarketplaceItem>, userId: String?) = coroutineScope {
    items.map { async { configRow(it, userId) } }.awaitAll()
}

private fun Route.getContext() = get("/context") {
    val user = ownUser()
    call.respond(Context(
        server = currentServer(),
        autoConfig = ModuleAutoConfig.enabled,
        onlyFeatured = ModuleAutoConfig.onlyFeatured,
        user = user?.nickname ?: user?.name,
    ))
}

private fun Route.getTags() = get("/tags") {
    call.respond(call.marketplace { MarketplaceApi.getTags().map { it.name } })
}

/**
 * A query with a share code resolves to that config, unlisted ones included.
 */
private fun Route.getConfigs() = get {
    val parameters = call.queryParameters
    val query = parameters["query"]?.trim()?.takeIf(String::isNotEmpty)
    val filter = ConfigFilter(
        page = parameters["page"]?.toIntOrNull() ?: 1,
        query = query,
        tags = parameters["tags"]?.split(',')?.map(String::trim)?.filter(String::isNotEmpty).orEmpty(),
        server = if (parameters["server"].toBoolean()) currentServer() else null,
        featured = parameters["featured"].toBoolean(),
        sort = if (parameters["sort"] == "new") MarketplaceApi.Sort.CREATED else MarketplaceApi.Sort.SCORE,
    )

    val response = call.marketplace {
        if (query != null && query.startsWith(MarketplaceConfigs.SHARE_CODE_PREFIX, ignoreCase = true)) {
            codePage(query)
        } else {
            configPage(filter)
        }
    }
    call.respond(response)
}

private suspend fun codePage(code: String): ConfigPage {
    val item = orNotFound { MarketplaceApi.getMarketplaceItemByCode(code) }
    val items = listOfNotNull(item?.takeIf { it.type == MarketplaceItemType.CONFIG })
    return ConfigPage(configRows(items, ownUserId()), Pagination(1, 1, items.size), true, 0)
}

private suspend fun configPage(filter: ConfigFilter): ConfigPage {
    val session = optionalSession()
    val tagIds = tagIds(filter.tags)
    suspend fun list(featured: Boolean?, limit: Int) = MarketplaceConfigs.list(
        page = filter.page,
        limit = limit,
        query = filter.query,
        tags = tagIds,
        targetServer = filter.server,
        featured = featured,
        sort = filter.sort,
        session = session
    )

    val response = list(filter.featured.takeIf { it }, PAGE_SIZE)
    val unfeatured = if (filter.featured && response.items.isEmpty()) list(null, 1).pagination.items else 0
    return ConfigPage(configRows(response.items, ownUserId()), response.pagination, false, unfeatured)
}

private fun Route.getConfig() = get {
    val id = call.requireId()
    val detail = call.marketplace { configDetail(id) }
    call.respond(detail)
}

private suspend fun configDetail(id: Int): ConfigDetail = coroutineScope {
    val session = optionalSession()
    val item = MarketplaceApi.getMarketplaceItem(id, session)
    val liveRevisionId = item.liveRevisionId
    val row = async { configRow(item, ownUserId()) }
    val dependencies = async { dependencyRows(id) }
    val revisions = async { revisionRows(item) }
    val forkOf = async {
        item.forkedFromItemId?.let { source ->
            runCatching { MarketplaceApi.getMarketplaceItem(source) }.getOrNull()
                ?.let { LinkedConfig(it.id, it.displayAddress) }
        }
    }
    val report = async {
        if (session != null && liveRevisionId != null) ownReport(session, id, liveRevisionId) else null
    }
    val overlay = row.await().overlayOn != null

    ConfigDetail(
        config = row.await(),
        summary = item.summary(),
        description = item.description,
        createdAt = epochMillis(item.createdAt),
        shareCode = item.shareCode,
        forkOf = forkOf.await(),
        dependencies = dependencies.await(),
        revisions = revisions.await(),
        changes = liveRevisionId?.takeIf { overlay }?.let { ConfigTracker.modules(id, it) },
        report = report.await(),
    )
}

private suspend fun ownReport(session: OAuthSession, id: Int, revisionId: Int) =
    orNotFound { MarketplaceApi.getOwnConfigReport(session, id, revisionId) }?.works

/**
 * Configs in the order they load, then what gets installed.
 */
private suspend fun dependencyRows(id: Int): List<DependencyRow> = coroutineScope {
    val dependencies = resolveDependencies(id)
    val configs = dependencies.configs.mapIndexed { index, config ->
        async {
            // A linked item leaves out servers and protocol.
            val item = MarketplaceApi.getMarketplaceItem(config.item.id)
            DependencyRow(
                id = item.id,
                type = item.type,
                address = item.displayAddress,
                image = serverIcon(item.targetServers),
                order = index + 1,
                featured = item.featured,
                protocol = item.protocolName?.takeIf(String::isNotEmpty),
                protocolMatches = item.protocolMatches,
                status = null,
            )
        }
    }
    val installables = dependencies.installables.map { installable ->
        async {
            val item = installable.item
            DependencyRow(
                id = item.id,
                type = item.type,
                address = item.displayAddress,
                image = item.thumbnailPid?.let(MarketplaceApi::fileUrl),
                order = null,
                featured = item.featured,
                protocol = null,
                protocolMatches = true,
                status = itemRow(item),
            )
        }
    }
    (configs + installables).awaitAll()
}

private suspend fun revisionRows(item: MarketplaceItem): List<RevisionRow> = coroutineScope {
    val reports = async { MarketplaceApi.getConfigReports(item.id).associateBy { it.revisionId } }
    val revisions = MarketplaceApi.getMarketplaceItemRevisions(item.id, 1, HISTORY_SIZE)
    val loaded = if (tracking(item.id) != ConfigTracker.State.NONE) ConfigTracker.revisionId else null
    revisions.items.mapIndexed { index, revision ->
        val summary = reports.await()[revision.id]
        RevisionRow(
            id = revision.id,
            createdAt = epochMillis(revision.createdAt),
            changelog = revision.changelog?.takeIf(String::isNotBlank),
            works = summary?.works ?: 0,
            fails = summary?.fails ?: 0,
            latest = revision.id == item.liveRevisionId,
            loaded = revision.id == loaded,
            first = index == revisions.items.lastIndex && revisions.pagination.pages <= 1,
        )
    }
}

private fun Route.getPlan() = get("/plan") {
    val id = call.requireId()
    val plan = call.marketplace {
        val item = MarketplaceApi.getMarketplaceItem(id, optionalSession())
        val revisionId = item.liveRevisionId ?: error("${item.name} has nothing published")
        val plan = ConfigTracker.plan(item, revisionId)
        LoadPlanView(
            installs = plan.installs.installs.map { install ->
                val installable = install.item
                PlanInstall(
                    id = installable.id,
                    type = installable.type,
                    name = installable.name,
                    author = installable.author,
                    image = installable.thumbnailPid?.let(MarketplaceApi::fileUrl),
                    revision = install.resolution.revision.view(),
                    restart = installable.installNeedsRestart
                )
            },
            leftOut = plan.installs.leftOut.map { leftOut ->
                PlanLeftOut(
                    id = leftOut.item.id,
                    type = leftOut.item.type,
                    name = leftOut.item.name,
                    image = leftOut.item.thumbnailPid?.let(MarketplaceApi::fileUrl),
                )
            },
            modules = plan.modules,
        )
    }
    call.respond(plan)
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

    val result = call.marketplace {
        val item = MarketplaceApi.getMarketplaceItem(id, optionalSession())
        val revisionId = item.liveRevisionId ?: error("${item.name} has nothing published")
        ConfigTracker.load(item, revisionId, modules)
    }
    call.respond(LoadResultView(result.installed.map { it.name }))
}

private fun Route.putReport() = put("/report") {
    data class ReportRequest(val works: Boolean)

    val id = call.requireId()
    val works = call.receive<ReportRequest>().works
    val session = call.requireSession()
    val report = call.marketplace {
        val item = MarketplaceApi.getMarketplaceItem(id, session)
        val revisionId = item.liveRevisionId ?: error("${item.name} has nothing published")
        MarketplaceApi.putConfigReport(
            session,
            id,
            revisionId,
            works,
            LiquidBounce.clientVersion,
            mc.currentServer?.ip?.dropPort()
        )
        reportView(session, id)
    }
    call.respond(report)
}

private fun Route.deleteReport() = delete("/report") {
    val id = call.requireId()
    val session = call.requireSession()
    val report = call.marketplace {
        val item = MarketplaceApi.getMarketplaceItem(id, session)
        val revisionId = item.liveRevisionId ?: error("${item.name} has nothing published")
        MarketplaceApi.deleteConfigReport(session, id, revisionId)
        reportView(session, id)
    }
    call.respond(report)
}

private suspend fun reportView(session: OAuthSession, id: Int): ReportView {
    val item = MarketplaceApi.getMarketplaceItem(id, session)
    val report = item.liveRevisionId?.let { ownReport(session, id, it) }
    return ReportView(item.recentWorks, item.recentFails, report)
}

private fun Route.patchConfig() = patch {
    data class DetailsRequest(
        val name: String,
        val description: String,
        val tags: List<String>,
        val servers: List<String>,
        val visibility: String,
    )

    val id = call.requireId()
    val request = call.receive<DetailsRequest>()
    if (request.name.isBlank()) {
        call.badRequest("A name is required")
    }
    val visibility = visibility(request.visibility) ?: call.badRequest("Unknown visibility ${request.visibility}")

    val session = call.requireSession()
    val updated = call.marketplace {
        MarketplaceApi.updateMarketplaceItem(
            session,
            id,
            request.name.trim(),
            MarketplaceItemType.CONFIG,
            request.description,
            MarketplaceApi.ItemDetails(
                tags = tagIds(request.tags),
                targetServers = request.servers.map { it.dropPort().rootDomain() }.distinct(),
                visibility = visibility
            )
        )
    }
    withContext(MinecraftDispatcher) { ConfigTracker.renamed(updated) }
    MarketplaceConfigs.refresh()
    call.respond(call.marketplace { configRow(updated, ownUserId()) })
}

/**
 * Only unlisted configs have a share code, and only their author gets it.
 */
private fun Route.postCopyShareCode() = post("/share-code") {
    val id = call.requireId()
    val session = call.requireSession()
    val code = call.marketplace { MarketplaceApi.getMarketplaceItem(id, session).shareCode }
        ?: call.forbidden("Only the author of an unlisted config has its share code")
    mc.execute { mc.keyboardHandler.clipboard = code }
    call.respond(JsonObject().apply { addProperty("shareCode", code) })
}

private fun Route.deleteConfig() = delete {
    val id = call.requireId()
    val session = call.requireSession()
    call.marketplace { ConfigTracker.delete(session, id) }
    MarketplaceConfigs.refresh()
    call.respond(HttpStatusCode.NoContent)
}

/**
 * Works offline, without the image and ownership then.
 */
private suspend fun trackerView(): TrackerView {
    val tracked = ConfigTracker.state != ConfigTracker.State.NONE
    val own = tracked && ownUserId() == ConfigTracker.itemUid
    val image = if (tracked) {
        runCatching { serverIcon(MarketplaceApi.getMarketplaceItem(ConfigTracker.itemId).targetServers) }.getOrNull()
    } else {
        null
    }
    return TrackerView(
        state = ConfigTracker.state,
        id = ConfigTracker.itemId,
        address = ConfigTracker.address,
        image = image,
        own = own,
        backup = ConfigTracker.hasBackup,
        edited = if (ConfigTracker.state == ConfigTracker.State.EDITING) {
            ConfigTracker.changedModules().sorted()
        } else {
            emptyList()
        },
    )
}

private fun Route.getTracker() = get {
    call.respond(trackerView())
}

private fun Route.postTrackerAction(path: String, action: suspend () -> Unit) = post(path) {
    call.marketplace { action() }
    call.respond(trackerView())
}

/**
 * Overlay and Fork publish the edits made to the tracked config.
 */
private fun Route.postPublish() = post("/publish") {
    data class PublishRequest(
        val kind: String,
        val name: String,
        val description: String,
        val visibility: String,
        val servers: List<String>,
        val tags: List<String>,
    )

    val request = call.receive<PublishRequest>()
    val name = request.name.trim().takeIf(String::isNotEmpty) ?: call.badRequest("A name is required")
    val visibility = visibility(request.visibility) ?: call.badRequest("Unknown visibility ${request.visibility}")
    if (request.kind != "New" && ConfigTracker.state != ConfigTracker.State.EDITING) {
        call.badRequest("Only an edited marketplace config can be published as ${request.kind.lowercase()}")
    }

    val session = call.requireSession()
    val item = call.marketplace {
        if (request.kind == "Fork" && ownUserId() == ConfigTracker.itemUid) {
            call.forbidden("${ConfigTracker.address} is yours, update it instead")
        }

        val details = MarketplaceApi.ItemDetails(
            tags = tagIds(request.tags),
            targetServers = request.servers.map { it.dropPort().rootDomain() }.distinct(),
            visibility = visibility
        )
        when (request.kind) {
            "New" -> ConfigTracker.create(session, name, request.description, details)
            "Overlay" -> ConfigTracker.overlay(session, name, request.description, details)
            "Fork" -> ConfigTracker.fork(session, name, request.description, details)
            else -> call.badRequest("Unknown kind ${request.kind}")
        }
    }
    MarketplaceConfigs.refresh()
    call.respond(Published(item.id, item.displayAddress, item.visibility, item.shareCode))
}

private fun Route.postUpdate() = post("/update") {
    data class UpdateRequest(val changelog: String?)

    val changelog = call.receive<UpdateRequest>().changelog?.trim()?.takeIf(String::isNotEmpty)
    if (ConfigTracker.state != ConfigTracker.State.EDITING) {
        call.badRequest("Nothing changed since ${ConfigTracker.address} was loaded")
    }

    val session = call.requireSession()
    call.marketplace {
        if (ownUserId() != ConfigTracker.itemUid) {
            call.forbidden("${ConfigTracker.address} is not yours")
        }
        ConfigTracker.update(session, changelog)
    }
    call.respond(trackerView())
}

internal fun Route.marketplaceConfigRoutes() = route("/marketplace") {
    getContext()
    getTags()
    route("/configs") {
        getConfigs()
        route("/{id}") {
            getConfig()
            getPlan()
            postLoad()
            putReport()
            deleteReport()
            patchConfig()
            deleteConfig()
            postCopyShareCode()
        }
    }
    route("/tracker") {
        getTracker()
        postTrackerAction("/revert", ConfigTracker::revert)
        postTrackerAction("/restore") { ConfigTracker.restoreBackup() }
        postTrackerAction("/detach") { ConfigTracker.detach() }
        postPublish()
        postUpdate()
    }
}
