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
import net.ccbluex.liquidbounce.api.models.marketplace.MarketplaceItemStatus
import net.ccbluex.liquidbounce.api.models.marketplace.MarketplaceItemType
import net.ccbluex.liquidbounce.api.services.marketplace.MarketplaceApi
import net.ccbluex.liquidbounce.features.addon.AddonInstaller
import net.ccbluex.liquidbounce.integration.task.type.ResourceTask
import net.ccbluex.liquidbounce.lang.translation
import net.ccbluex.liquidbounce.utils.client.variable
import net.minecraft.network.chat.MutableComponent

sealed interface RevisionResult {

    data class Compatible(val revision: MarketplaceItemRevision) : RevisionResult

    data class NoneCompatible(val unavailable: UnavailableItem) : RevisionResult

}

/**
 * No revision of [name] fits this game, though it has others when [hasOtherVersions]. Otherwise the item is not
 * active or has nothing published.
 */
data class UnavailableItem(val name: String, val hasOtherVersions: Boolean) {

    fun describe() = text().string

    fun text(): MutableComponent {
        val key = "liquidbounce.marketplace.unavailable"
        val addon = variable(name)
        return if (hasOtherVersions) {
            translation("$key.version", addon, variable("v${AddonInstaller.liquidbounce}"))
        } else {
            translation("$key.nothing", addon)
        }
    }

}

class NoCompatibleRevisionException(val unavailable: UnavailableItem) : Exception(unavailable.describe())

/**
 * The newest revision, for an add-on the newest one the marketplace offers for this game. [known]
 * spares fetching the item. Callers hold [SubscribedItem.locked].
 */
internal suspend fun SubscribedItem.findCompatibleRevision(known: MarketplaceItem? = null): RevisionResult {
    val item = known ?: MarketplaceApi.getMarketplaceItem(id)
    if (item.status != MarketplaceItemStatus.ACTIVE) {
        return RevisionResult.NoneCompatible(UnavailableItem(name, false))
    }

    val revision = if (type == MarketplaceItemType.ADDON) {
        MarketplaceApi.getMarketplaceItemRevisions(id, 1, 1, AddonInstaller.minecraft, AddonInstaller.liquidbounce)
    } else {
        MarketplaceApi.getMarketplaceItemRevisions(id, 1, 1)
    }.items.firstOrNull()
    if (revision != null) {
        return RevisionResult.Compatible(revision)
    }

    val hasOtherVersions = type == MarketplaceItemType.ADDON && item.liveRevisionId != null
    return RevisionResult.NoneCompatible(UnavailableItem(name, hasOtherVersions))
}

/**
 * Unpacks the revision [findCompatibleRevision] picks unless it is installed. Callers hold [SubscribedItem.locked].
 */
internal suspend fun SubscribedItem.unpackCompatibleRevision(subTask: () -> ResourceTask? = { null }): RevisionResult {
    val result = findCompatibleRevision()
    val revisionId = (result as? RevisionResult.Compatible)?.revision?.id
    if (revisionId != null && revisionId != installedRevisionId) {
        unpack(revisionId, subTask())
    }
    return result
}
