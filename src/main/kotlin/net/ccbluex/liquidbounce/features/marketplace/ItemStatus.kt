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

import net.ccbluex.liquidbounce.api.core.orNotFound
import net.ccbluex.liquidbounce.api.models.marketplace.MarketplaceItem
import net.ccbluex.liquidbounce.api.models.marketplace.MarketplaceItemRevision
import net.ccbluex.liquidbounce.api.models.marketplace.MarketplaceItemType
import net.ccbluex.liquidbounce.api.services.marketplace.MarketplaceApi
import net.ccbluex.liquidbounce.features.addon.AddonManager

/**
 * Where an add-on, script or theme stands on this client.
 *
 * @param fitting the newest revision offered for this game
 * @param installable not subscribed, and it and everything it needs has a fitting revision
 * @param incompatible it or something it needs has revisions, but none for this LiquidBounce
 */
internal class ItemStatus(
    val subscribed: Boolean,
    val installed: MarketplaceItemRevision?,
    val fitting: MarketplaceItemRevision?,
    val installable: Boolean,
    val incompatible: Boolean,
    val restartRequired: Boolean,
) {
    val hasUpdate get() = subscribed && fitting != null && fitting.id != installed?.id
}

internal suspend fun MarketplaceItem.itemStatus(): ItemStatus {
    val subscribed = MarketplaceManager.getItem(id) ?: return unsubscribedStatus()

    val resolution = subscribed.locked { subscribed.resolveRevision(known = this) }
    val fitting = (resolution as? RevisionResolution.Compatible)?.revision
    val installed = subscribed.installedRevisionId?.let { revisionId ->
        // Null when taken down since it was installed.
        fitting?.takeIf { it.id == revisionId }
            ?: orNotFound { MarketplaceApi.getMarketplaceItemRevision(id, revisionId) }
    }

    return ItemStatus(
        subscribed = true,
        installed = installed,
        fitting = fitting,
        installable = false,
        incompatible = (resolution as? RevisionResolution.NoneCompatible)?.unavailable?.published == true,
        restartRequired = restartRequired(subscribed = true),
    )
}

/**
 * Walks what the item needs like installing it does, without installing anything.
 */
private suspend fun MarketplaceItem.unsubscribedStatus(): ItemStatus {
    val dependencies = dependenciesOf(id)
    var fitting: MarketplaceItemRevision? = null
    val leftOut = leftOut(dependencies.installables + Installable(this, dependencies.needs)) { item ->
        val subscribed = SubscribedItem(item)
        when (val resolution = subscribed.locked { subscribed.resolveRevision(known = item) }) {
            is RevisionResolution.Compatible -> {
                if (item.id == id) {
                    fitting = resolution.revision
                }
                null
            }

            is RevisionResolution.NoneCompatible -> resolution.unavailable
        }
    }

    return ItemStatus(
        subscribed = false,
        installed = null,
        fitting = fitting,
        installable = fitting != null && leftOut.isEmpty(),
        incompatible = leftOut.any { it.unavailable.published },
        restartRequired = restartRequired(subscribed = false),
    )
}

private suspend fun MarketplaceItem.restartRequired(subscribed: Boolean) = when (type) {
    // Also pending after it was removed.
    MarketplaceItemType.ADDON -> AddonManager.isRestartRequired(id)
    // Runs once the add-on that runs scripts is loaded.
    MarketplaceItemType.SCRIPT -> subscribed && !MarketplaceManager.hasHandler(type) &&
        scriptRuntime()?.let { MarketplaceManager.isSubscribed(it.id) } == true
    else -> false
}

private suspend fun MarketplaceItem.scriptRuntime(): MarketplaceItem? {
    val dependencies = dependenciesOf(id)
    return dependencies.installables
        .map { it.item }
        .firstOrNull { it.id in dependencies.needs && it.type == MarketplaceItemType.ADDON }
}
