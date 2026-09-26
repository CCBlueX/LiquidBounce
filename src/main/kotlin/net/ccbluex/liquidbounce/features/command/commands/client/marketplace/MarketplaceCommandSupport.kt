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
package net.ccbluex.liquidbounce.features.command.commands.client.marketplace

import com.mojang.brigadier.suggestion.SuggestionProvider
import net.ccbluex.liquidbounce.api.models.marketplace.MarketplaceItem
import net.ccbluex.liquidbounce.features.command.CommandException
import net.ccbluex.liquidbounce.features.command.brigadier.ClientCommandSource
import net.ccbluex.liquidbounce.features.command.brigadier.CmdI18n
import net.ccbluex.liquidbounce.features.command.brigadier.suggestions
import net.ccbluex.liquidbounce.features.command.commands.client.config.quoted
import net.ccbluex.liquidbounce.features.command.commands.client.config.single
import net.ccbluex.liquidbounce.features.marketplace.MarketplaceItems
import net.ccbluex.liquidbounce.features.marketplace.MarketplaceManager
import net.ccbluex.liquidbounce.features.marketplace.SubscribedItem
import net.ccbluex.liquidbounce.features.marketplace.autoconfig.MarketplaceConfigs.address
import net.ccbluex.liquidbounce.utils.client.variable

/**
 * `author/name` of the top items one is not subscribed to yet.
 */
internal val subscribableSuggestions: SuggestionProvider<ClientCommandSource> = suggestions {
    MarketplaceItems.index.filterNot { MarketplaceManager.isSubscribed(it.id) }.map { quoted(it.address) }
}

/**
 * `author/name` of the subscribed items. The name while the author is unknown, and the id where either is shared.
 */
internal val subscribedSuggestions: SuggestionProvider<ClientCommandSource> = suggestions {
    suggestSubscribed(MarketplaceManager.subscribedItems)
}

internal fun suggestSubscribed(items: Collection<SubscribedItem>): List<String> {
    fun Collection<String>.repeated() = groupingBy { it.lowercase() }.eachCount().filterValues { it > 1 }.keys

    val sharedNames = items.map { it.name }.repeated()
    val addresses = items.map { item ->
        item.author?.let { "$it/${item.name}" } ?: item.name.takeIf { it.lowercase() !in sharedNames }
    }
    val sharedAddresses = addresses.filterNotNull().repeated()
    return items.zip(addresses) { item, address ->
        if (address == null || address.lowercase() in sharedAddresses) item.id.toString() else quoted(address)
    }
}

internal suspend fun CmdI18n.marketplaceItem(input: String): MarketplaceItem =
    single(input, MarketplaceItems.find(input))

/**
 * The subscribed item [input] names by id or name. A name several of them share is refused with what tells them
 * apart.
 */
internal fun CmdI18n.subscribedItem(input: String): SubscribedItem {
    val matches = matchSubscribed(input, MarketplaceManager.subscribedItems)
    return when (matches.size) {
        0 -> throw CommandException(t("error.notSubscribed", variable(input)))
        1 -> matches.single()
        else -> throw CommandException(
            t("error.ambiguous", variable(input), variable(suggestSubscribed(matches).joinToString(", ")))
        )
    }
}

/**
 * The [items] with the id, the name or `author/name` [input] gives, ignoring case. Without one by that author,
 * `author/name` matches subscriptions whose author is not known yet by their name.
 */
internal fun matchSubscribed(input: String, items: Collection<SubscribedItem>): List<SubscribedItem> {
    input.toIntOrNull()?.let { id ->
        return items.filter { it.id == id }
    }

    val named = items.filter { it.name.equals(input, ignoreCase = true) }
    if (named.isNotEmpty() || '/' !in input) {
        return named
    }

    val author = input.substringBefore('/')
    val name = input.substringAfter('/')
    val sameName = items.filter { it.name.equals(name, ignoreCase = true) }
    return sameName.filter { it.author.equals(author, ignoreCase = true) }
        .ifEmpty { sameName.filter { it.author == null } }
}
