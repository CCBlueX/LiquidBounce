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
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.patch
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import kotlinx.coroutines.Dispatchers
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
import net.ccbluex.liquidbounce.api.models.pagination.Pagination
import net.ccbluex.liquidbounce.api.services.marketplace.MarketplaceApi
import net.ccbluex.liquidbounce.features.command.brigadier.CmdI18n
import net.ccbluex.liquidbounce.features.command.commands.client.config.isEditableBy
import net.ccbluex.liquidbounce.features.command.commands.client.config.reportInstalled
import net.ccbluex.liquidbounce.features.marketplace.autoconfig.ConfigTracker
import net.ccbluex.liquidbounce.features.marketplace.autoconfig.MarketplaceConfigs
import net.ccbluex.liquidbounce.features.marketplace.autoconfig.MarketplaceConfigs.address
import net.ccbluex.liquidbounce.features.marketplace.getDependencies
import net.ccbluex.liquidbounce.features.module.ModuleManager
import net.ccbluex.liquidbounce.integration.interop.badRequest
import net.ccbluex.liquidbounce.integration.interop.forbidden
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.copyable
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.client.regular
import net.ccbluex.liquidbounce.utils.client.variable
import net.ccbluex.liquidbounce.utils.text.dropPort
import net.ccbluex.liquidbounce.utils.text.rootDomain
import net.minecraft.network.chat.MutableComponent

private const val PAGE_SIZE = 20
private const val HISTORY_SIZE = 10

/**
 * The listing fields of a config as publishing and editing send them. Only publishing has a [kind].
 */
private data class ConfigDetailsRequest(
    val name: String,
    val description: String,
    val tags: List<String>,
    val servers: List<String>,
    val visibility: String,
    val kind: String? = null,
)

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

private suspend fun ApplicationCall.requireVisibility(request: ConfigDetailsRequest): MarketplaceItemVisibility {
    if (request.name.isBlank()) {
        badRequest("A name is required")
    }
    return MarketplaceItemVisibility.entries.find { it.name.equals(request.visibility, true) }
        ?: badRequest("Unknown visibility ${request.visibility}")
}

private suspend fun ConfigDetailsRequest.toItemDetails(visibility: MarketplaceItemVisibility) =
    MarketplaceApi.ItemDetails(
        tags = findTagIds(tags),
        targetServers = servers.map { it.dropPort().rootDomain() }.distinct(),
        visibility = visibility
    )

/**
 * The config and its newest revision, the one loads and reports go to.
 */
private suspend fun getLiveConfig(id: Int, session: OAuthSession?): Pair<MarketplaceItem, Int> {
    val item = MarketplaceApi.getMarketplaceItem(id, session)
    return item to (item.liveRevisionId ?: error("${item.name} has nothing published"))
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

        val session = getSessionOrNull()
        val tagIds = findTagIds(tags)
        suspend fun page(featured: Boolean?, limit: Int = PAGE_SIZE) = MarketplaceConfigs.list(
            page = list.page,
            limit = limit,
            query = list.query,
            tags = tagIds,
            targetServer = server,
            featured = featured,
            sort = list.sort,
            session = session
        )

        val response = page(featured = if (featuredOnly) true else null)
        val unfeatured = if (featuredOnly && response.items.isEmpty()) {
            page(featured = null, limit = 1).pagination.items
        } else {
            0
        }
        val configs = response.items.toConfigData(getOwnUserIdOrNull())
        ConfigPageData(configs, response.pagination, code = false, unfeatured)
    }
}

/**
 * A share code also finds unlisted configs.
 */
private suspend fun findSharedConfig(code: String): ConfigPageData {
    val item = orNotFound { MarketplaceApi.getMarketplaceItemByCode(code) }
    val items = listOfNotNull(item?.takeIf { it.type == MarketplaceItemType.CONFIG })
    val configs = items.toConfigData(getOwnUserIdOrNull())
    return ConfigPageData(configs, Pagination(1, 1, items.size), code = true, unfeatured = 0)
}

private fun Route.getConfig() = get {
    val id = call.requireId()
    call.respondMarketplace { getConfigDetail(id) }
}

private suspend fun getConfigDetail(id: Int): ConfigDetailData = coroutineScope {
    val session = getSessionOrNull()
    val item = MarketplaceApi.getMarketplaceItem(id, session)
    val liveRevisionId = item.liveRevisionId
    val config = async { item.toConfigData(getOwnUserIdOrNull()) }
    val dependencies = async { getDependencies(id) }
    val configs = async {
        // Fetched again: a linked item lacks the servers the icon comes from.
        dependencies.await().configs
            .map { async { MarketplaceApi.getMarketplaceItem(it.item.id).toLinkedConfigData() } }
            .awaitAll()
    }
    val installs = async { dependencies.await().installables.map { async { it.item.toItemData() } }.awaitAll() }
    val revisions = async { getRevisionHistory(item) }
    val forkOf = async {
        item.forkedFromItemId?.let { forkedFrom ->
            runCatching { MarketplaceApi.getMarketplaceItem(forkedFrom).toLinkedConfigData() }.getOrNull()
        }
    }
    val report = async {
        if (session == null || liveRevisionId == null) {
            return@async null
        }
        orNotFound { MarketplaceApi.getOwnConfigReport(session, id, liveRevisionId) }?.works
    }
    val isOverlay = config.await().overlayOn != null

    ConfigDetailData(
        config = config.await(),
        description = item.description,
        createdAt = parseEpochMillis(item.createdAt),
        shareCode = item.shareCode,
        forkOf = forkOf.await(),
        configs = configs.await(),
        installs = installs.await(),
        revisions = revisions.await(),
        changes = liveRevisionId?.takeIf { isOverlay }?.let { ConfigTracker.getModuleNames(id, it) },
        report = report.await(),
    )
}

private suspend fun getRevisionHistory(item: MarketplaceItem): List<ConfigRevisionData> = coroutineScope {
    val reports = async { MarketplaceApi.getConfigReports(item.id).associateBy { it.revisionId } }
    val revisions = MarketplaceApi.getMarketplaceItemRevisions(item.id, 1, HISTORY_SIZE)
    val loadedRevisionId = ConfigTracker.revisionId.takeIf { ConfigTracker.trackedItemId == item.id }
    revisions.items.mapIndexed { index, revision ->
        val summary = reports.await()[revision.id]
        ConfigRevisionData(
            id = revision.id,
            createdAt = parseEpochMillis(revision.createdAt),
            changelog = revision.changelog?.takeIf(String::isNotBlank),
            works = summary?.works ?: 0,
            fails = summary?.fails ?: 0,
            latest = revision.id == item.liveRevisionId,
            loaded = revision.id == loadedRevisionId,
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
        val (item, revisionId) = getLiveConfig(id, getSessionOrNull())
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

        val (item, revisionId) = getLiveConfig(id, getSessionOrNull())
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
        val (item, revisionId) = getLiveConfig(id, session)
        if (works == null) {
            MarketplaceApi.deleteConfigReport(session, id, revisionId)
        } else {
            val server = mc.currentServer?.ip?.dropPort()
            MarketplaceApi.putConfigReport(session, id, revisionId, works, LiquidBounce.clientVersion, server)
            sendFeedback(configI18n.t("report.reported", variable(item.name)))
        }
        MarketplaceApi.getMarketplaceItem(id, session).toConfigData(getOwnUserIdOrNull())
    }
}

private fun Route.patchConfig() = patch {
    call.respondMarketplace {
        val id = call.requireId()
        val request = call.receive<ConfigDetailsRequest>()
        val visibility = call.requireVisibility(request)
        val session = call.requireSession()
        val updated = MarketplaceApi.updateMarketplaceItem(
            session,
            id,
            request.name.trim(),
            MarketplaceItemType.CONFIG,
            request.description,
            request.toItemDetails(visibility)
        )
        withContext(Dispatchers.Main) { ConfigTracker.renamed(updated) }
        MarketplaceConfigs.refresh()
        sendFeedback(configI18n.t("set.updated", variable(updated.name)))
        updated.toConfigData(getOwnUserIdOrNull())
    }
}

private fun Route.deleteConfig() = delete {
    call.handleMarketplaceErrors {
        val id = call.requireId()
        val session = call.requireSession()
        val name = MarketplaceApi.getMarketplaceItem(id, session).name
        ConfigTracker.delete(session, id)
        MarketplaceConfigs.refresh()
        sendFeedback(configI18n.t("delete.deleted", variable(name)))
    }
    call.respond(HttpStatusCode.NoContent)
}

/**
 * Copies the share code to the clipboard. Only unlisted configs have one, and only their author gets it.
 */
private fun Route.postShareCode() = post("/share-code") {
    call.handleMarketplaceErrors {
        val id = call.requireId()
        val session = call.requireSession()
        val code = MarketplaceApi.getMarketplaceItem(id, session).shareCode
            ?: call.forbidden("Only the author of an unlisted config has its share code")
        mc.execute { mc.keyboardHandler.clipboard = code }
        sendFeedback(configI18n.t("info.shareCode", variable(code).copyable()))
    }
    call.respond(HttpStatusCode.NoContent)
}

private fun Route.getTracker() = get {
    call.respond(getTrackerData())
}

/**
 * Runs the tracker [action], which returns what to tell the player.
 */
private fun Route.postTrackerAction(path: String, action: suspend () -> MutableComponent) = post(path) {
    call.handleMarketplaceErrors { sendFeedback(action()) }
    call.respond(getTrackerData())
}

private fun Route.postRevert() = postTrackerAction("/revert") {
    ConfigTracker.revert()
    configI18n.t("revert.reverted", variable(ConfigTracker.itemName))
}

private fun Route.postRestore() = postTrackerAction("/restore") {
    ConfigTracker.restoreBackup()
    configI18n.t("restore.restored")
}

private fun Route.postDetach() = postTrackerAction("/detach") {
    val name = ConfigTracker.itemName
    ConfigTracker.detach()
    configI18n.t("detach.detached", variable(name))
}

/**
 * `kind` is `New`, `Overlay` or `Fork`. Overlay and Fork publish the edits made to the tracked config.
 */
private fun Route.postPublish() = post("/publish") {
    call.respondMarketplace {
        val request = call.receive<ConfigDetailsRequest>()
        val kind = request.kind
        val visibility = call.requireVisibility(request)
        if (kind != "New" && ConfigTracker.state != ConfigTracker.State.EDITING) {
            call.badRequest("Only an edited marketplace config can be published as ${kind?.lowercase()}")
        }

        val session = call.requireSession()
        val name = request.name.trim()
        if (kind == "Fork" && getOwnUserIdOrNull() == ConfigTracker.itemUid) {
            call.forbidden("${ConfigTracker.address} is yours, update it instead")
        }

        val details = request.toItemDetails(visibility)
        val item = when (kind) {
            "New" -> ConfigTracker.create(session, name, request.description, details)
            "Overlay" -> ConfigTracker.overlay(session, name, request.description, details)
            "Fork" -> ConfigTracker.fork(session, name, request.description, details)
            else -> call.badRequest("Unknown kind $kind")
        }
        MarketplaceConfigs.refresh()
        sendFeedback(configI18n.t("publish.published", variable(item.name)))
        item.shareCode?.let { code ->
            mc.execute { chat(regular(configI18n.t("info.shareCode", variable(code).copyable()))) }
        }
        PublishedConfigData(item.id, item.address, item.shareCode)
    }
}

private fun Route.postUpdate() = post("/update") {
    data class UpdateRequest(val changelog: String?)

    call.handleMarketplaceErrors {
        val changelog = call.receive<UpdateRequest>().changelog?.trim()?.takeIf(String::isNotEmpty)
        if (ConfigTracker.state != ConfigTracker.State.EDITING) {
            call.badRequest("Nothing changed since ${ConfigTracker.address} was loaded")
        }

        val session = call.requireSession()
        val item = MarketplaceApi.getMarketplaceItem(ConfigTracker.itemId, session)
        if (!item.isEditableBy(getOwnUserIdOrNull())) {
            call.forbidden(configI18n.t("error.notEditor", variable(item.name)).string)
        }
        ConfigTracker.update(session, changelog)
        sendFeedback(configI18n.t("update.updated", variable(ConfigTracker.itemName)))
    }
    call.respond(getTrackerData())
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
            patchConfig()
            deleteConfig()
            postShareCode()
        }
    }
    route("/tracker") {
        getTracker()
        postRevert()
        postRestore()
        postDetach()
        postPublish()
        postUpdate()
    }
}
