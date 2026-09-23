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
@file:Suppress("TooManyFunctions")

package net.ccbluex.liquidbounce.features.command.commands.client.config

import com.mojang.brigadier.suggestion.SuggestionProvider
import kotlinx.coroutines.CancellationException
import net.ccbluex.liquidbounce.api.models.auth.OAuthSession
import net.ccbluex.liquidbounce.api.models.marketplace.MarketplaceItem
import net.ccbluex.liquidbounce.api.models.marketplace.MarketplaceItemVisibility
import net.ccbluex.liquidbounce.api.services.marketplace.MarketplaceApi
import net.ccbluex.liquidbounce.config.autoconfig.AutoConfigMetadata
import net.ccbluex.liquidbounce.features.command.CommandException
import net.ccbluex.liquidbounce.features.command.brigadier.ClientCommandSource
import net.ccbluex.liquidbounce.features.command.brigadier.CmdI18n
import net.ccbluex.liquidbounce.features.command.brigadier.suggestions
import net.ccbluex.liquidbounce.features.command.preset.accountOrException
import net.ccbluex.liquidbounce.features.cosmetic.ClientAccountManager
import net.ccbluex.liquidbounce.features.marketplace.autoconfig.ConfigTracker
import net.ccbluex.liquidbounce.features.marketplace.autoconfig.MarketplaceConfigs
import net.ccbluex.liquidbounce.features.marketplace.autoconfig.MarketplaceConfigs.address
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.logger
import net.ccbluex.liquidbounce.utils.client.regular
import net.ccbluex.liquidbounce.utils.client.variable
import java.time.LocalDateTime

internal val configSuggestions: SuggestionProvider<ClientCommandSource> = suggestions {
    MarketplaceConfigs.index.map { quoted(it.address) }
}

internal val visibilitySuggestions: SuggestionProvider<ClientCommandSource> =
    suggestions(MarketplaceItemVisibility.entries.map { it.name.lowercase() })

internal fun formatDate(dateTime: String): String =
    runCatching { LocalDateTime.parse(dateTime).format(AutoConfigMetadata.FORMATTER) }
        .getOrDefault(dateTime.substringBefore('T'))

internal suspend fun session(): OAuthSession = ClientAccountManager.accountOrException().takeSession()

internal suspend fun ownUserId(): String? {
    val account = ClientAccountManager.accountOrException()
    if (account.userInformation == null) {
        account.updateInfo()
    }
    return account.userInformation?.userId
}

internal suspend fun CmdI18n.resolveConfig(input: String): MarketplaceItem =
    single(input, MarketplaceConfigs.find(input))

/**
 * The one item [input] names. A bare name that several authors use is refused with their
 * `author/name` forms, so a config never loads by accident.
 */
internal fun CmdI18n.single(input: String, matches: List<MarketplaceItem>): MarketplaceItem = when (matches.size) {
    0 -> throw CommandException(t("error.notFound", variable(input)))
    1 -> matches.single()
    else -> throw CommandException(
        t("error.ambiguous", variable(input), variable(matches.joinToString(", ") { quoted(it.address) }))
    )
}

internal fun quoted(argument: String) = if (argument.any(Char::isWhitespace)) "\"$argument\"" else argument

internal fun CmdI18n.requireTracked() {
    if (ConfigTracker.state == ConfigTracker.State.NONE) {
        throw CommandException(t("error.notTracking"))
    }
}

internal suspend fun CmdI18n.requireOwnTracked() {
    requireTracked()
    if (ownUserId() != ConfigTracker.itemUid) {
        throw CommandException(t("error.notOwner", variable(ConfigTracker.itemName)))
    }
}

internal fun CmdI18n.parseVisibility(input: String?): MarketplaceItemVisibility =
    if (input == null) {
        MarketplaceItemVisibility.PUBLIC
    } else {
        MarketplaceItemVisibility.entries.find { it.name.equals(input, ignoreCase = true) }
            ?: throw CommandException(t("error.invalidVisibility", variable(input)))
    }

internal suspend fun CmdI18n.tagIds(names: Collection<String>): List<Int> {
    if (names.isEmpty()) {
        return emptyList()
    }

    val tags = request { MarketplaceApi.getTags() }
    return names.map { name ->
        tags.find { it.name.equals(name, ignoreCase = true) }?.id
            ?: throw CommandException(t("error.unknownTag", variable(name)))
    }
}

internal fun CmdI18n.reportInstalled(result: ConfigTracker.LoadResult) {
    if (result.installed.isEmpty()) {
        return
    }

    val names = result.installed.joinToString(", ") { it.name }
    chat(regular(t("load.installed", variable(names))))
    if (result.restartRequired) {
        chat(regular(t("load.restart")))
    }
}

/**
 * Turns an API failure into a chat error instead of a stack trace.
 */
internal suspend inline fun <T> CmdI18n.request(block: () -> T): T = try {
    block()
} catch (e: CancellationException) {
    throw e
} catch (e: CommandException) {
    throw e
} catch (e: Exception) {
    logger.error("Marketplace config request failed", e)
    throw CommandException(t("error.request", variable(e.message ?: e.javaClass.simpleName)), e)
}
