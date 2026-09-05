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
import net.ccbluex.liquidbounce.integration.task.type.Task
import net.ccbluex.liquidbounce.utils.client.clientLogger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withContext
import net.ccbluex.liquidbounce.utils.kotlin.MinecraftDispatcher
import java.io.File

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
 * Marketplace manager for subscribing and updating items.
 */
object MarketplaceManager : Config("marketplace"), EventListener {

    private val logger = clientLogger("MarketplaceManager")

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

        // Reload the item type's manager. Must be on the render thread - this is reachable from a
        // Ktor worker via the interop server, and install() already does the same.
        withContext(MinecraftDispatcher) {
            item.type.reload()
        }
    }

}
