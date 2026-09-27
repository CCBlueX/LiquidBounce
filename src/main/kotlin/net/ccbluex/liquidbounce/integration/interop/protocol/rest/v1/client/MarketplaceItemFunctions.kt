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

import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import net.ccbluex.liquidbounce.api.models.marketplace.MarketplaceItem
import net.ccbluex.liquidbounce.api.models.marketplace.MarketplaceItemRevision
import net.ccbluex.liquidbounce.api.models.marketplace.MarketplaceItemType
import net.ccbluex.liquidbounce.api.services.marketplace.MarketplaceApi
import net.ccbluex.liquidbounce.config.ConfigSystem
import net.ccbluex.liquidbounce.event.events.NotificationEvent
import net.ccbluex.liquidbounce.features.addon.AddonInstaller
import net.ccbluex.liquidbounce.features.command.brigadier.CmdI18n
import net.ccbluex.liquidbounce.features.marketplace.MarketplaceManager
import net.ccbluex.liquidbounce.features.marketplace.NoCompatibleRevisionException
import net.ccbluex.liquidbounce.features.marketplace.UpdateResult
import net.ccbluex.liquidbounce.features.marketplace.installWithDependencies
import net.ccbluex.liquidbounce.integration.interop.badRequest
import net.ccbluex.liquidbounce.integration.interop.notFound
import net.ccbluex.liquidbounce.integration.theme.ThemeManager
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.client.regular
import net.ccbluex.liquidbounce.utils.client.variable

private const val PAGE_SIZE = 20
private const val REVISION_PAGE_SIZE = 50

/**
 * The messages of `.marketplace`, which the tab tells the same way.
 */
private val marketplaceCommand = object : CmdI18n {
    override val path = "marketplace"
}

private fun Route.getItems() = get {
    val name = call.queryParameters["type"]
    val type = MarketplaceItemType.entries.find { it.tag.equals(name, true) && it.isSubscribable }
        ?: call.badRequest("Unknown type $name")
    val list = call.listQuery

    call.respondMarketplace {
        val response = MarketplaceApi.getMarketplaceItems(
            page = list.page,
            limit = PAGE_SIZE,
            query = list.query,
            type = type,
            filter = MarketplaceApi.Filter(sort = list.sort)
        )
        val items = coroutineScope { response.items.map { async { itemView(it) } }.awaitAll() }
        PageView(items, response.pagination)
    }
}

private fun Route.getItem() = get {
    val id = call.requireId()
    call.respondMarketplace {
        val item = MarketplaceApi.getMarketplaceItem(id)
        ItemDetailView(itemView(item), item.description, "v${AddonInstaller.liquidbounce}", versions(item))
    }
}

/**
 * Every revision, newest first. For an add-on, only those the marketplace offers for this game fit.
 */
private suspend fun versions(item: MarketplaceItem): List<VersionView> {
    val revisions = revisions(item.id)
    val fitting = if (item.type == MarketplaceItemType.ADDON) {
        revisions(item.id, AddonInstaller.minecraft, AddonInstaller.liquidbounce).mapTo(HashSet()) { it.id }
    } else {
        null
    }
    val installed = MarketplaceManager.getItem(item.id)?.installedRevisionId

    return revisions.map { revision ->
        VersionView(revision.view(), revision.id == installed, fitting == null || revision.id in fitting)
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

/**
 * Installs the item after what it needs, as `.marketplace subscribe` does.
 */
private fun Route.postInstall() = post("/install") {
    call.marketplaceAction {
        val id = call.requireId()
        val (installed, unavailable) = installWithDependencies(MarketplaceApi.getMarketplaceItem(id))
        if (installed.none { it.id == id }) {
            throw NoCompatibleRevisionException(unavailable.first())
        }

        tellPlayer(marketplaceCommand.t("subscribe.success", variable(id.toString())))
        val needed = installed.filter { it.id != id }
        if (needed.isNotEmpty()) {
            val names = needed.joinToString(", ") { it.name }
            mc.execute { chat(regular(marketplaceCommand.t("subscribe.dependencies", variable(names)))) }
        }
    }
    call.respond(HttpStatusCode.NoContent)
}

private fun Route.postUpdate() = post("/update") {
    val view = call.marketplaceAction {
        val id = call.requireId()
        val subscribed = MarketplaceManager.getItem(id) ?: call.notFound(id.toString(), "Not installed")
        when (val result = MarketplaceManager.update(subscribed)) {
            is UpdateResult.Updated -> tellPlayer(
                marketplaceCommand.t("update.success", variable(id.toString()), variable(result.revisionId.toString()))
            )
            is UpdateResult.NoUpdate -> tellPlayer(
                marketplaceCommand.t("update.noUpdate", variable(id.toString())),
                NotificationEvent.Severity.INFO
            )
            is UpdateResult.Incompatible -> call.badRequest(result.unavailable.text().string)
            is UpdateResult.Failed -> throw marketplaceFailure(result.error as? Exception ?: Exception(result.error))
        }
        itemView(MarketplaceApi.getMarketplaceItem(id))
    }
    call.respond(view)
}

private fun Route.postRemove() = post("/remove") {
    call.marketplaceAction {
        val id = call.requireId()
        if (!MarketplaceManager.isSubscribed(id)) {
            call.notFound(id.toString(), "Not installed")
        }

        MarketplaceManager.unsubscribe(id)
        tellPlayer(marketplaceCommand.t("unsubscribe.success", variable(id.toString())))
    }
    call.respond(HttpStatusCode.NoContent)
}

private fun Route.postApply() = post("/apply") {
    call.marketplaceAction {
        val id = call.requireId()
        val theme = ThemeManager.marketplaceThemes[id] ?: call.notFound(id.toString(), "Theme not loaded")

        withContext(Dispatchers.Main) {
            ThemeManager.theme = theme
            ConfigSystem.store(ThemeManager)
        }
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
