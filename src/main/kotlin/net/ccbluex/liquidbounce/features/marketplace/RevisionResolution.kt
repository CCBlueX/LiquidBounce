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

sealed interface RevisionResolution {

    /**
     * [revision] is the one to have installed.
     */
    data class Compatible(val revision: MarketplaceItemRevision) : RevisionResolution

    data class NoneCompatible(val unavailable: Unavailable) : RevisionResolution

}

/**
 * No revision of [name] fits this game, though it has [published] ones. Without them, the item is not
 * active or has nothing published.
 */
data class Unavailable(val name: String, val published: Boolean) {

    fun describe() = text().string

    fun text(): MutableComponent {
        val key = "liquidbounce.marketplace.unavailable"
        val addon = variable(name)
        return if (published) {
            translation("$key.version", addon, variable("v${AddonInstaller.liquidbounce}"))
        } else {
            translation("$key.nothing", addon)
        }
    }

}

class NoCompatibleRevisionException(val unavailable: Unavailable) : Exception(unavailable.describe())

/**
 * The revision this item should have installed: the newest one, for an add-on the newest one the
 * marketplace offers for this game. [known] spares fetching the item when the caller has it. Callers
 * hold [SubscribedItem.locked].
 */
internal suspend fun SubscribedItem.resolveRevision(known: MarketplaceItem? = null): RevisionResolution {
    val item = known ?: MarketplaceApi.getMarketplaceItem(id)
    if (item.status != MarketplaceItemStatus.ACTIVE) {
        return RevisionResolution.NoneCompatible(Unavailable(name, false))
    }

    val fitting = if (type == MarketplaceItemType.ADDON) {
        MarketplaceApi.getMarketplaceItemRevisions(id, 1, 1, AddonInstaller.minecraft, AddonInstaller.liquidbounce)
    } else {
        MarketplaceApi.getMarketplaceItemRevisions(id, 1, 1)
    }.items.firstOrNull()
    if (fitting != null) {
        return RevisionResolution.Compatible(fitting)
    }

    val published = type == MarketplaceItemType.ADDON && item.liveRevisionId != null
    return RevisionResolution.NoneCompatible(Unavailable(name, published))
}

/**
 * Unpacks the revision [resolveRevision] picks unless it is installed. Callers hold [SubscribedItem.locked].
 */
internal suspend fun SubscribedItem.unpackResolved(subTask: () -> ResourceTask? = { null }): RevisionResolution {
    val resolution = resolveRevision()
    val revisionId = (resolution as? RevisionResolution.Compatible)?.revision?.id
    if (revisionId != null && revisionId != installedRevisionId) {
        unpack(revisionId, subTask())
    }
    return resolution
}
