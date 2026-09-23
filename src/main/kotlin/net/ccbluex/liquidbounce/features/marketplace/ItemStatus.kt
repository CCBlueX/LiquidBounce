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
 * Where an add-on, script or theme stands on this client. [fitting] is the newest revision offered
 * for this game. An item that is not [subscribed] installs unless it or something it needs is
 * [unavailable], [notFor] this LiquidBounce when it is for want of a fitting revision.
 */
internal data class ItemStatus(
    val subscribed: Boolean,
    val installed: MarketplaceItemRevision?,
    val fitting: MarketplaceItemRevision?,
    val unavailable: Boolean,
    val notFor: Boolean,
    val restartRequired: Boolean,
) {
    val hasUpdate get() = subscribed && fitting != null && fitting.id != installed?.id

    val installable get() = !subscribed && fitting != null && !unavailable
}

internal suspend fun MarketplaceItem.checkStatus(): ItemStatus {
    val subscribed = MarketplaceManager.getItem(id) ?: return plannedStatus()

    val resolution = subscribed.locked { subscribed.resolveRevision(known = this) }
    val compatible = resolution as? RevisionResolution.Compatible
    val installed = subscribed.installedRevisionId?.let { revisionId ->
        // Null when taken down since it was installed.
        compatible?.revision?.takeIf { it.id == revisionId }
            ?: orNotFound { MarketplaceApi.getMarketplaceItemRevision(id, revisionId) }
    }
    val runtime = if (type == MarketplaceItemType.SCRIPT) runtimeOf(resolveDependencies(id)) else null

    return ItemStatus(
        subscribed = true,
        installed = installed,
        fitting = compatible?.revision,
        unavailable = compatible == null,
        notFor = (resolution as? RevisionResolution.NoneCompatible)?.unavailable?.published == true,
        restartRequired = restartRequired(runtime),
    )
}

private suspend fun MarketplaceItem.plannedStatus(): ItemStatus {
    val dependencies = resolveDependencies(id)
    val plan = planInstalls(dependencies.installables + Installable(this, dependencies.needs))

    return ItemStatus(
        subscribed = false,
        installed = null,
        fitting = plan.installs.find { it.item.id == id }?.resolution?.revision,
        unavailable = plan.leftOut.isNotEmpty(),
        notFor = plan.leftOut.any { it.unavailable.published },
        restartRequired = restartRequired(null),
    )
}

private fun runtimeOf(dependencies: Dependencies) = dependencies.installables
    .map { it.item }
    .firstOrNull { it.id in dependencies.needs && it.type == MarketplaceItemType.ADDON }

/**
 * An add-on takes effect at the next start, installed or removed; a script once the add-on it runs
 * on loads.
 */
private fun MarketplaceItem.restartRequired(runtime: MarketplaceItem?) = when (type) {
    MarketplaceItemType.ADDON -> AddonManager.isRestartRequired(id)
    MarketplaceItemType.SCRIPT -> runtime != null && !MarketplaceManager.hasHandler(type) &&
        MarketplaceManager.isSubscribed(runtime.id) && MarketplaceManager.isSubscribed(id)
    else -> false
}
