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
import net.ccbluex.liquidbounce.api.models.marketplace.MarketplaceItemType
import net.ccbluex.liquidbounce.api.services.marketplace.MarketplaceApi

/**
 * [configs] in the order they are applied, [installables] after the ones they need. [requiredIds] are the
 * add-ons and scripts the item depends on itself, [unpublished] the ones needed that have nothing published.
 */
internal class ItemDependencies(
    val configs: List<ConfigDependency>,
    val installables: List<InstallableItem>,
    val requiredIds: Set<Int>,
    val unpublished: Map<Int, UnavailableItem>
)

internal class ConfigDependency(val item: MarketplaceItem, val revision: MarketplaceItemRevision)

internal class InstallableItem(val item: MarketplaceItem, val requiredIds: Set<Int>)

internal data class InstallResult(val installed: List<MarketplaceItem>, val unavailable: List<UnavailableItem>)

/**
 * An add-on always loads on the next start, a script only while nothing runs scripts.
 */
internal val MarketplaceItem.shouldRestartAfterInstall
    get() = type == MarketplaceItemType.ADDON ||
        type == MarketplaceItemType.SCRIPT && !MarketplaceManager.hasHandler(type)

private val MarketplaceItemType.isInstallable
    get() = this == MarketplaceItemType.ADDON || this == MarketplaceItemType.SCRIPT

private val MarketplaceItem.requiredIds
    get() = dependencies.filter { it.type.isInstallable }.mapTo(linkedSetOf()) { it.id }

/**
 * The marketplace lists what an item requires with a dependency before what needs it, so installables come
 * after the ones they need and an add-on is checked with those installed.
 */
internal suspend fun getDependencies(item: MarketplaceItem): ItemDependencies {
    fun nothingPublished(name: String): Nothing = error("Config dependency $name has nothing published")

    // A list leaves out what its items require; one without dependencies requires nothing.
    val root = item.takeIf { it.requires != null || it.dependencies.isEmpty() }
        ?: MarketplaceApi.getMarketplaceItem(item.id)
    val required = root.requires.orEmpty()
    val published = required.mapTo(hashSetOf()) { it.id }
    // shortcut: the marketplace names 50 required items at most, so in a larger tree the rest reads as
    // unpublished. Walk their dependencies once an item needs that many.
    val unpublished = (required + root).flatMap { it.dependencies }.filter { it.id !in published }
    unpublished.firstOrNull { it.type == MarketplaceItemType.CONFIG }?.let { nothingPublished(it.name) }

    return ItemDependencies(
        configs = required.filter { it.type == MarketplaceItemType.CONFIG }
            .map { ConfigDependency(it, it.liveRevision ?: nothingPublished(it.name)) },
        installables = required.filter { it.type.isInstallable }.map { InstallableItem(it, it.requiredIds) },
        requiredIds = root.requiredIds,
        unpublished = unpublished.filter { it.type.isInstallable }
            .associate { it.id to UnavailableItem(it.name, false) }
    )
}

/**
 * Subscribes to the missing installables of [dependencies] and then to [root]. One that does not fit this
 * game is skipped with what needs it, and named in [InstallResult.unavailable].
 */
internal suspend fun installDependencies(
    dependencies: ItemDependencies,
    root: MarketplaceItem? = null
): InstallResult {
    val installed = mutableListOf<MarketplaceItem>()
    val unavailable = installInOrder(dependencies, root) { item ->
        try {
            MarketplaceManager.subscribe(item)
            installed += item
            null
        } catch (e: NoCompatibleRevisionException) {
            e.unavailable
        }
    }
    return InstallResult(installed, unavailable.distinct())
}

internal suspend fun installWithDependencies(item: MarketplaceItem): InstallResult =
    installDependencies(getDependencies(item), item)

/**
 * Runs [install] for each missing installable of [dependencies] and then for [root]. Skipped are the
 * unpublished ones, the ones [install] gives a reason for, and what needs any of those, with that reason.
 *
 * @return the reason of each skipped item
 */
internal suspend fun installInOrder(
    dependencies: ItemDependencies,
    root: MarketplaceItem?,
    install: suspend (MarketplaceItem) -> UnavailableItem?
): List<UnavailableItem> {
    val skipped = LinkedHashMap(dependencies.unpublished.filterKeys { !MarketplaceManager.isSubscribed(it) })
    val installables = dependencies.installables +
        listOfNotNull(root?.let { InstallableItem(it, dependencies.requiredIds) })
    for (installable in installables) {
        val item = installable.item
        if (MarketplaceManager.isSubscribed(item.id)) {
            continue
        }

        val reason = installable.requiredIds.firstNotNullOfOrNull(skipped::get) ?: install(item)
        if (reason != null) {
            skipped[item.id] = reason
        }
    }
    return skipped.values.toList()
}
