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

import net.ccbluex.liquidbounce.api.models.marketplace.MarketplaceItemType
import net.ccbluex.liquidbounce.config.ConfigSystem
import net.ccbluex.liquidbounce.config.types.nesting.Configurable
import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.integration.task.type.Task
import net.ccbluex.liquidbounce.utils.client.logger
import java.io.File

/**
 * Marketplace manager for subscribing and updating items.
 */
object MarketplaceManager : Configurable("marketplace"), EventListener {

    private val subscribedItems by value("subscribed", mutableListOf<SubscribedItem>())

    val marketplaceRoot = File(ConfigSystem.rootFolder, "marketplace").apply {
        mkdirs()
    }

    fun isSubscribed(itemId: Int) = subscribedItems.any { it.id == itemId }

    suspend fun updateAll(task: Task) {
         subscribedItems.forEach { item ->
             runCatching {
                 val updateRevisionId = item.checkUpdate() ?: return@forEach
                 val subTask = task.getOrCreateFileTask(item.id.toString())
                 item.install(updateRevisionId, subTask)
                 subTask.isCompleted = true
             }.onFailure {
                 logger.error("Failed to update item ${item.id}", it)
             }
         }
    }

    suspend fun subscribe(itemId: Int, type: MarketplaceItemType) {
        check(type.isSubscribable) { "Type $type is not subscribable" }

        if (isSubscribed(itemId)) {
            return
        }

        val item = SubscribedItem(itemId, type, null)
        item.install(item.getNewestRevisionId() ?: return)
        subscribedItems.add(item)
        ConfigSystem.storeConfigurable(this)
    }

    suspend fun unsubscribe(itemId: Int) {
        val item = subscribedItems.find { item -> item.id == itemId } ?: error("Item $itemId not found")

        check(!item.itemDir.exists() || item.itemDir.deleteRecursively()) { "Failed to delete item directory" }

        subscribedItems.remove(item)
        ConfigSystem.storeConfigurable(this)
    }

}
