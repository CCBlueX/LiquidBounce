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
package net.ccbluex.liquidbounce.features.marketplace

import net.ccbluex.liquidbounce.api.models.marketplace.MarketplaceItem
import net.ccbluex.liquidbounce.api.models.marketplace.MarketplaceItemRevision
import net.ccbluex.liquidbounce.api.models.marketplace.MarketplaceItemType
import net.ccbluex.liquidbounce.api.services.marketplace.MarketplaceApi
import net.ccbluex.liquidbounce.config.ConfigSystem
import net.ccbluex.liquidbounce.config.types.Config
import net.ccbluex.liquidbounce.config.types.ValueType
import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.features.addon.AddonApi
import net.ccbluex.liquidbounce.integration.task.type.Task
import net.ccbluex.liquidbounce.utils.client.clientLogger
import kotlinx.coroutines.CancellationException
import java.io.File
import java.util.EnumMap

sealed interface UpdateResult {
    data class Updated(val item: SubscribedItem, val revision: MarketplaceItemRevision) : UpdateResult

    data class NoUpdate(val item: SubscribedItem) : UpdateResult

    data class Incompatible(val item: SubscribedItem, val unavailable: UnavailableItem) : UpdateResult

    /** The item stays on its old revision. */
    data class Failed(val item: SubscribedItem, val error: Throwable) : UpdateResult
}

/**
 * Runs subscribed items of a type the client does not handle itself. Called on the render thread
 * with every subscribed item of that type once at startup and whenever one is installed, updated
 * or removed.
 */
@AddonApi
fun interface MarketplaceItemHandler {
    fun reload(items: List<SubscribedItem>)
}

@Suppress("TooManyFunctions")
object MarketplaceManager : Config("marketplace"), EventListener {

    private val logger = clientLogger("MarketplaceManager")

    private val handlers = EnumMap<MarketplaceItemType, MarketplaceItemHandler>(MarketplaceItemType::class.java)

    @AddonApi
    fun registerHandler(type: MarketplaceItemType, handler: MarketplaceItemHandler) {
        check(handlers.putIfAbsent(type, handler) == null) { "Items of type $type are handled already" }
    }

    @AddonApi
    fun unregisterHandler(type: MarketplaceItemType, handler: MarketplaceItemHandler) {
        handlers.remove(type, handler)
    }

    @AddonApi
    fun hasHandler(type: MarketplaceItemType) = type in handlers

    internal fun reloadHandler(type: MarketplaceItemType) {
        val handler = handlers[type] ?: return
        runCatching { handler.reload(getSubscribedItemsOfType(type)) }
            .onFailure { logger.error("Failed to reload $type items", it) }
    }

    /**
     * Runs before module settings load, so what the items register gets its settings back.
     */
    internal fun reloadHandlers() = handlers.keys.forEach(::reloadHandler)

    val subscribedItems by list("subscribed", mutableListOf<SubscribedItem>(), ValueType.SUBSCRIBED_ITEM)

    val marketplaceRoot = File(ConfigSystem.rootFolder, "marketplace").apply {
        mkdirs()
    }

    fun getSubscribedItemsOfType(itemType: MarketplaceItemType) = subscribedItems.filter { it.type == itemType }

    fun getItem(itemId: Int) = subscribedItems.find { it.id == itemId }

    fun isSubscribed(itemId: Int) = subscribedItems.any { it.id == itemId }

    /**
     * A failing item does not abort the batch; it yields [UpdateResult.Failed].
     */
    suspend fun updateAll(task: Task? = null): List<UpdateResult> =
        subscribedItems.toTypedArray().map { item ->
            try {
                update(item, task)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logger.error("Failed to update item ${item.id}", e)
                UpdateResult.Failed(item, e)
            }
        }

    suspend fun update(item: SubscribedItem, task: Task? = null): UpdateResult = item.locked {
        fun fileTask() = task?.getOrCreateFileTask(item.id.toString())

        logger.info("Checking for updates for item ${item.id} (${item.type})")
        val installed = item.installedRevisionId
        val result = item.unpackCompatibleRevision(::fileTask)
        val compatible = when (result) {
            is RevisionResult.Compatible -> result
            is RevisionResult.NoneCompatible -> return@locked handleIncompatible(item, result.unavailable)
        }

        val revisionId = compatible.revision.id
        if (revisionId == installed) {
            return@locked UpdateResult.NoUpdate(item)
        }

        item.reload()
        fileTask()?.isCompleted = true
        logger.info("Updated item ${item.id} (${item.type}) to revision $revisionId")

        UpdateResult.Updated(item, compatible.revision)
    }

    private fun handleIncompatible(item: SubscribedItem, unavailable: UnavailableItem): UpdateResult {
        if (!unavailable.hasOtherVersions) {
            return UpdateResult.NoUpdate(item)
        }

        logger.warn("Not installing item ${item.id}: ${unavailable.describe()}")
        return UpdateResult.Incompatible(item, unavailable)
    }

    /**
     * @throws NoCompatibleRevisionException when [item] has no revision that fits this game.
     */
    suspend fun subscribe(item: MarketplaceItem) {
        val subscribed = SubscribedItem(item)
        val added = subscribed.locked {
            if (isSubscribed(item.id)) {
                return@locked false
            }

            val result = subscribed.unpackCompatibleRevision()
            if (result is RevisionResult.NoneCompatible) {
                throw NoCompatibleRevisionException(result.unavailable)
            }
            subscribedItems.add(subscribed)
        }
        if (!added) {
            return
        }

        ConfigSystem.store(this)
        subscribed.reload()
    }

    /**
     * Subscriptions saved before the author was kept lack it.
     */
    internal suspend fun updateMissingAuthors() {
        val unknown = subscribedItems.filter { it.author == null }
        if (unknown.isEmpty()) {
            return
        }

        for (item in unknown) {
            runCatching { MarketplaceApi.getMarketplaceItem(item.id) }
                .onSuccess { item.author = it.author }
                .onFailure { logger.warn("Failed to look up the author of item ${item.id}", it) }
        }
        ConfigSystem.store(this)
    }

    suspend fun unsubscribe(itemId: Int) {
        val item = getItem(itemId) ?: error("Item $itemId not found")

        item.locked {
            check(!item.itemDir.exists() || item.itemDir.deleteRecursively()) { "Failed to delete item directory" }
        }

        subscribedItems.remove(item)
        ConfigSystem.store(this)
        item.reload()
    }

}
