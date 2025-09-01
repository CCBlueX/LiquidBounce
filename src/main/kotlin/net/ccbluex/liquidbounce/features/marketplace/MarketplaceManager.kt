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

    fun subscribe(itemId: Int, type: MarketplaceItemType) {
        if (isSubscribed(itemId)) {
            return
        }

        subscribedItems.add(SubscribedItem(itemId, type, null))
        ConfigSystem.storeConfigurable(this)
    }

    fun unsubscribe(itemId: Int) {
        subscribedItems.removeIf { it.id == itemId }

        val itemFolder = File(marketplaceRoot, itemId.toString())
        if (itemFolder.exists()) {
            itemFolder.deleteRecursively()
        }
        ConfigSystem.storeConfigurable(this)
    }

}
