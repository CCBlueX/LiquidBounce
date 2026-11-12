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
 * @param compatibleRevision the newest revision offered for this game
 * @param installable not subscribed, and it and everything it needs has a compatible revision
 * @param incompatible it or something it needs has revisions, but none for this LiquidBounce
 */
internal class InstallState(
    val subscribed: Boolean,
    val installedRevision: MarketplaceItemRevision?,
    val compatibleRevision: MarketplaceItemRevision?,
    val installable: Boolean,
    val incompatible: Boolean,
    val restartRequired: Boolean,
) {
    val hasUpdate get() = subscribed && compatibleRevision != null && compatibleRevision.id != installedRevision?.id
}

internal suspend fun MarketplaceItem.getInstallState(): InstallState {
    val subscribed = MarketplaceManager.getItem(id) ?: return checkInstallable()

    val result = subscribed.locked { subscribed.findCompatibleRevision(known = this) }
    val compatibleRevision = (result as? RevisionResult.Compatible)?.revision
    val installedRevision = subscribed.installedRevisionId?.let { revisionId ->
        // Null when taken down since it was installed.
        compatibleRevision?.takeIf { it.id == revisionId }
            ?: orNotFound { MarketplaceApi.getMarketplaceItemRevision(id, revisionId) }
    }

    return InstallState(
        subscribed = true,
        installedRevision = installedRevision,
        compatibleRevision = compatibleRevision,
        installable = false,
        incompatible = (result as? RevisionResult.NoneCompatible)?.unavailable?.hasOtherVersions == true,
        restartRequired = isRestartRequired(subscribed = true),
    )
}

/**
 * Walks what the item needs like installing it does, without installing anything.
 */
private suspend fun MarketplaceItem.checkInstallable(): InstallState {
    val dependencies = getDependencies(id)
    var compatibleRevision: MarketplaceItemRevision? = null
    val installables = dependencies.installables + InstallableItem(this, dependencies.requiredIds)
    val skipped = installInOrder(installables) { item ->
        val subscribed = SubscribedItem(item)
        when (val result = subscribed.locked { subscribed.findCompatibleRevision(known = item) }) {
            is RevisionResult.Compatible -> {
                if (item.id == id) {
                    compatibleRevision = result.revision
                }
                null
            }

            is RevisionResult.NoneCompatible -> result.unavailable
        }
    }

    return InstallState(
        subscribed = false,
        installedRevision = null,
        compatibleRevision = compatibleRevision,
        installable = compatibleRevision != null && skipped.isEmpty(),
        incompatible = skipped.any { it.unavailable.hasOtherVersions },
        restartRequired = isRestartRequired(subscribed = false),
    )
}

private suspend fun MarketplaceItem.isRestartRequired(subscribed: Boolean) = when (type) {
    // Also pending after it was removed.
    MarketplaceItemType.ADDON -> AddonManager.isRestartRequired(id)
    // Runs once the add-on that runs scripts is loaded.
    MarketplaceItemType.SCRIPT -> subscribed && !MarketplaceManager.hasHandler(type) &&
        findScriptRuntime()?.let { MarketplaceManager.isSubscribed(it.id) } == true
    else -> false
}

private suspend fun MarketplaceItem.findScriptRuntime(): MarketplaceItem? {
    val dependencies = getDependencies(id)
    return dependencies.installables
        .map { it.item }
        .firstOrNull { it.id in dependencies.requiredIds && it.type == MarketplaceItemType.ADDON }
}
