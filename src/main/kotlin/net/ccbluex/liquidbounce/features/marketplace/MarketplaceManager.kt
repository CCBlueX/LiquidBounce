/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2024 CCBlueX
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
import net.ccbluex.liquidbounce.config.types.ValueType
import net.ccbluex.liquidbounce.config.types.nesting.Configurable
import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.features.command.Command
import net.ccbluex.liquidbounce.integration.task.type.Task
import net.ccbluex.liquidbounce.lang.translation
import net.ccbluex.liquidbounce.utils.client.*
import java.io.File

/**
 * Marketplace manager for subscribing and updating items.
 */
object MarketplaceManager : Configurable("marketplace"), EventListener {

    val subscribedItems by list("subscribed", mutableListOf<SubscribedItem>(), ValueType.SUBSCRIBED_ITEM)

    val marketplaceRoot = File(ConfigSystem.rootFolder, "marketplace").apply {
        mkdirs()
    }

    fun getSubscribedItemsOfType(itemType: MarketplaceItemType) = subscribedItems.filter { it.type == itemType }

    fun getItem(itemId: Int) = subscribedItems.find { it.id == itemId }

    fun isSubscribed(itemId: Int) = subscribedItems.any { it.id == itemId }

    suspend fun updateAll(task: Task? = null, command: Command? = null) {
         subscribedItems.forEach { item ->
             update(item, task, command)
         }
    }

    suspend fun update(item: SubscribedItem, task: Task? = null, command: Command? = null) = runCatching {
        logger.info("Checking for updates for item ${item.id} (${item.type})")
        val updateRevisionId = item.checkUpdate() ?: run {
            command?.run { chat(regular(command.result("noUpdate", variable(item.id.toString())))) }
            return@runCatching
        }
        logger.info("Updating item ${item.id} (${item.type})...")
        command?.run { chat(regular(command.result("updating", variable(item.id.toString())))) }
        val subTask = task?.getOrCreateFileTask(item.id.toString())
        item.install(updateRevisionId, subTask)
        subTask?.isCompleted = true
        logger.info("Successfully updated item ${item.id} (${item.type})")
        command?.run {
            chat(
                regular(
                    command.result(
                        "success",
                        variable(item.id.toString()),
                        variable(updateRevisionId.toString())
                    )
                )
            )
        }
    }.onFailure { e ->
        logger.error("Failed to update item ${item.id}", e)
        if (command != null) {
            chat(
                markAsError(
                    (translation(
                        "liquidbounce.command.marketplace.error.updateFailed",
                        item.id,
                        e.message ?: "Unknown error"
                    ))
                )
            )
        }
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

        // Reload the item type's manager.
        item.type.reload()
    }

}
