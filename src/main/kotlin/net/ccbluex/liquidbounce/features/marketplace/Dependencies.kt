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

/**
 * What an item depends on: configs in the order they are applied, and add-ons and scripts after the
 * ones they need. [needs] are the add-ons and scripts the item depends on itself.
 */
internal class Dependencies(
    val configs: List<ConfigDependency>,
    val installables: List<Installable>,
    val needs: Set<Int>
)

internal class ConfigDependency(val item: MarketplaceItem, val revision: MarketplaceItemRevision)

internal class Installable(val item: MarketplaceItem, val needs: Set<Int>)

internal data class Installed(val installed: List<MarketplaceItem>, val unavailable: List<Unavailable>)

internal class LeftOut(val item: MarketplaceItem, val unavailable: Unavailable)

/**
 * Whether installing it takes effect only after a restart: for an add-on always, for a script while
 * nothing runs scripts.
 */
internal val MarketplaceItem.installNeedsRestart
    get() = type == MarketplaceItemType.ADDON ||
        type == MarketplaceItemType.SCRIPT && !MarketplaceManager.hasHandler(type)

/**
 * Walks the dependencies of [rootId] depth-first. Config dependencies come out in the order they are
 * applied, installables after the ones they need, so an add-on is checked with those installed.
 */
internal suspend fun dependenciesOf(rootId: Int): Dependencies {
    val configs = linkedMapOf<Int, ConfigDependency>()
    val installables = linkedMapOf<Int, Installable>()
    val visiting = hashSetOf<Int>()
    val needsOf = hashMapOf<Int, Set<Int>>()

    suspend fun visit(id: Int): Set<Int> {
        needsOf[id]?.let { return it }
        if (!visiting.add(id)) {
            return emptySet()
        }

        val needs = linkedSetOf<Int>()
        for (dependency in MarketplaceApi.getItemDependencies(id)) {
            val item = dependency.item.copy(
                author = dependency.author ?: dependency.item.author,
                liveRevisionId = dependency.liveRevision?.id ?: dependency.item.liveRevisionId
            )
            when (item.type) {
                MarketplaceItemType.CONFIG -> {
                    visit(item.id)
                    val revision = dependency.liveRevision
                        ?: error("Config dependency ${item.name} has nothing published")
                    configs.putIfAbsent(item.id, ConfigDependency(item, revision))
                }

                MarketplaceItemType.ADDON, MarketplaceItemType.SCRIPT -> {
                    installables.putIfAbsent(item.id, Installable(item, visit(item.id)))
                    needs += item.id
                }

                else -> {}
            }
        }

        visiting.remove(id)
        needsOf[id] = needs
        return needs
    }

    val needs = visit(rootId)
    return Dependencies(configs.values.toList(), installables.values.toList(), needs)
}

/**
 * Subscribes to the [installables] that are missing. One that does not fit this game does not
 * stop the others; it and what needs it are left out, and [Installed.unavailable] names it.
 */
internal suspend fun installDependencies(installables: Collection<Installable>): Installed {
    val installed = mutableListOf<MarketplaceItem>()
    val skipped = leftOut(installables) { item ->
        try {
            MarketplaceManager.subscribe(item)
            installed += item
            null
        } catch (e: NoCompatibleRevisionException) {
            e.unavailable
        }
    }
    return Installed(installed, skipped.map { it.unavailable }.distinct())
}

/**
 * Subscribes to [item] after what it needs, as [installDependencies] does.
 */
internal suspend fun installWithDependencies(item: MarketplaceItem): Installed {
    val dependencies = dependenciesOf(item.id)
    return installDependencies(dependencies.installables + Installable(item, dependencies.needs))
}

/**
 * What of the [installables] is left out, running [install] for each missing one: the inactive ones, the ones
 * [install] gives a reason for, and what needs any of those, with its reason.
 */
private suspend fun leftOut(
    installables: Collection<Installable>,
    install: suspend (MarketplaceItem) -> Unavailable?
): List<LeftOut> {
    val leftOut = linkedMapOf<Int, LeftOut>()
    for (installable in installables) {
        val item = installable.item
        if (MarketplaceManager.isSubscribed(item.id)) {
            continue
        }

        val reason = installable.needs.firstNotNullOfOrNull { leftOut[it]?.unavailable }
            ?: Unavailable(item.name, false).takeIf { item.status != MarketplaceItemStatus.ACTIVE }
            ?: install(item)
        if (reason != null) {
            leftOut[item.id] = LeftOut(item, reason)
        }
    }
    return leftOut.values.toList()
}
