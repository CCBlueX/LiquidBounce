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
import net.ccbluex.liquidbounce.api.models.marketplace.MarketplaceItemType
import net.ccbluex.liquidbounce.config.ConfigSystem
import net.ccbluex.liquidbounce.config.types.Config
import net.ccbluex.liquidbounce.config.types.ValueType
import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.features.addon.AddonApi
import net.ccbluex.liquidbounce.integration.task.type.Task
import net.ccbluex.liquidbounce.utils.client.clientLogger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withContext
import net.ccbluex.liquidbounce.utils.kotlin.MinecraftDispatcher
import java.io.File
import java.util.EnumMap

/**
 * Outcome of a single [MarketplaceManager.update] call.
 */
sealed interface UpdateResult {
    /** The item was (re-)installed to revision [revisionId]. */
    data class Updated(val item: SubscribedItem, val revisionId: Int) : UpdateResult

    /** The item is already on its newest revision. */
    data class NoUpdate(val item: SubscribedItem) : UpdateResult

    /** The update failed with [error]; the item was left untouched on its old revision. */
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

/**
 * Marketplace manager for subscribing and updating items.
 */
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

    internal fun reloadHandled(type: MarketplaceItemType) {
        val handler = handlers[type] ?: return
        runCatching { handler.reload(getSubscribedItemsOfType(type)) }
            .onFailure { logger.error("Failed to reload $type items", it) }
    }

    /**
     * Hands every handler its subscribed items once they are loaded, before module settings are,
     * so what the items register gets its settings back.
     */
    internal fun reloadHandlers() = handlers.keys.forEach(::reloadHandled)

    val subscribedItems by list("subscribed", mutableListOf<SubscribedItem>(), ValueType.SUBSCRIBED_ITEM)

    val marketplaceRoot = File(ConfigSystem.rootFolder, "marketplace").apply {
        mkdirs()
    }

    fun getSubscribedItemsOfType(itemType: MarketplaceItemType) = subscribedItems.filter { it.type == itemType }

    fun getItem(itemId: Int) = subscribedItems.find { it.id == itemId }

    fun isSubscribed(itemId: Int) = subscribedItems.any { it.id == itemId }

    /**
     * Updates every subscribed item; one failing item does not abort the batch. Every
     * item yields exactly one [UpdateResult] (including [UpdateResult.Failed]), so the
     * caller can report successes and failures separately.
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

    /**
     * Checks and installs the newest revision of [item], returning what happened.
     *
     * @throws Exception when checking or installing fails; callers decide how to surface it.
     */
    suspend fun update(item: SubscribedItem, task: Task? = null): UpdateResult {
        logger.info("Checking for updates for item ${item.id} (${item.type})")
        val updateRevisionId = item.checkUpdate() ?: return UpdateResult.NoUpdate(item)

        logger.info("Updating item ${item.id} (${item.type})...")
        val subTask = task?.getOrCreateFileTask(item.id.toString())
        item.install(updateRevisionId, subTask)
        subTask?.isCompleted = true
        logger.info("Successfully updated item ${item.id} (${item.type})")

        return UpdateResult.Updated(item, updateRevisionId)
    }

    suspend fun subscribe(item: MarketplaceItem) {
        if (isSubscribed(item.id)) {
            return
        }

        val item = SubscribedItem(item)
        subscribedItems.add(item)
        item.install(item.getNewestRevisionId() ?: return)
        ConfigSystem.store(this)
    }

    suspend fun unsubscribe(itemId: Int) {
        val item = subscribedItems.find { item -> item.id == itemId } ?: error("Item $itemId not found")

        check(!item.itemDir.exists() || item.itemDir.deleteRecursively()) { "Failed to delete item directory" }

        subscribedItems.remove(item)
        ConfigSystem.store(this)

        // Reload the item type's manager. Also reached from Ktor workers, hence the render thread.
        withContext(MinecraftDispatcher) {
            item.type.reload()
        }
    }

}
