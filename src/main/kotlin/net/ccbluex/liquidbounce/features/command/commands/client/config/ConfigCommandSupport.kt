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
import net.ccbluex.liquidbounce.api.core.orNotFound
import net.ccbluex.liquidbounce.api.models.auth.ClientAccount
import net.ccbluex.liquidbounce.api.models.auth.OAuthSession
import net.ccbluex.liquidbounce.api.models.marketplace.MarketplaceItem
import net.ccbluex.liquidbounce.api.models.marketplace.MarketplaceItemType
import net.ccbluex.liquidbounce.api.models.marketplace.MarketplaceItemVisibility
import net.ccbluex.liquidbounce.api.models.user.UserInformation
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
import net.ccbluex.liquidbounce.utils.client.warning
import net.ccbluex.liquidbounce.utils.text.joinToText
import net.minecraft.commands.SharedSuggestionProvider
import net.minecraft.network.chat.ComponentUtils.DEFAULT_SEPARATOR
import java.time.OffsetDateTime

internal val configSuggestions: SuggestionProvider<ClientCommandSource> = suggestions {
    MarketplaceConfigs.index.map { quoted(it.address) }
}

internal val tagSuggestions: SuggestionProvider<ClientCommandSource> = suggestions {
    MarketplaceConfigs.tags.map { quoted(it.name) }
}

/**
 * Completes the tag after the last comma of a comma-separated list.
 */
internal val tagListSuggestions = SuggestionProvider<ClientCommandSource> { _, builder ->
    val last = builder.remaining.lastIndexOf(',') + 1
    val offset = last + builder.remaining.substring(last).takeWhile(Char::isWhitespace).length
    SharedSuggestionProvider.suggest(
        MarketplaceConfigs.tags.map { it.name },
        builder.createOffset(builder.start + offset)
    )
}

internal val visibilitySuggestions: SuggestionProvider<ClientCommandSource> =
    suggestions(MarketplaceItemVisibility.entries.map { it.name.lowercase() })

internal fun formatDate(dateTime: String): String =
    runCatching { OffsetDateTime.parse(dateTime).format(AutoConfigMetadata.FORMATTER) }
        .getOrDefault(dateTime.substringBefore('T'))

internal suspend fun session(): OAuthSession = ClientAccountManager.accountOrException().takeSession()

internal suspend fun sessionOrNull(): OAuthSession? {
    if (ClientAccountManager.clientAccount == ClientAccount.EMPTY_ACCOUNT) {
        return null
    }

    return try {
        session()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        logger.warn("No marketplace session, using the public view", e)
        null
    }
}

/**
 * The share code of an unlisted config. Only its owner is told.
 */
internal suspend fun MarketplaceItem.findShareCode(): String? {
    if (visibility != MarketplaceItemVisibility.UNLISTED) {
        return null
    }

    val session = sessionOrNull() ?: return null
    return orNotFound { MarketplaceApi.getOwnMarketplaceItem(session, id) }?.shareCode
}

internal suspend fun ownUser(): UserInformation? {
    val account = ClientAccountManager.accountOrException()
    if (account.userInformation == null) {
        account.updateInfo()
    }
    return account.userInformation
}

internal suspend fun ownUserId(): String? = ownUser()?.uid

internal suspend fun CmdI18n.findConfig(input: String): MarketplaceItem =
    single(input, MarketplaceConfigs.find(input))

internal fun CmdI18n.single(input: String, matches: List<MarketplaceItem>): MarketplaceItem = when (matches.size) {
    0 -> throw CommandException(t("error.notFound", variable(input)))
    1 -> matches.single()
    else -> throw CommandException(
        t("error.ambiguous", variable(input), matches.joinToText(DEFAULT_SEPARATOR) { variable(quoted(it.address)) })
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

/**
 * The owner, a moderator, or anyone signed in once the owner opened a public config to the community.
 */
internal fun MarketplaceItem.isEditableBy(user: UserInformation?): Boolean = when {
    user == null || type != MarketplaceItemType.CONFIG -> false
    owner.uid == user.uid || "moderate" in user.permissions -> true
    else -> communityEditable && visibility == MarketplaceItemVisibility.PUBLIC
}

/**
 * The session to publish the tracked config with, if the user may edit it.
 */
internal suspend fun CmdI18n.requireEditableTracked(): OAuthSession {
    requireTracked()
    val user = request { ownUser() }
    val session = request { session() }
    val item = request { MarketplaceApi.getMarketplaceItem(ConfigTracker.itemId) }
    if (!item.isEditableBy(user)) {
        throw CommandException(t("error.notEditor", variable(item.name)))
    }
    return session
}

internal fun CmdI18n.parseVisibility(input: String?): MarketplaceItemVisibility {
    input ?: return MarketplaceItemVisibility.PUBLIC

    return MarketplaceItemVisibility.entries.find { it.name.equals(input, ignoreCase = true) }
        ?: throw CommandException(t("error.invalidVisibility", variable(input)))
}

internal suspend fun CmdI18n.tagIds(names: Collection<String>): List<Int> {
    if (names.isEmpty()) {
        return emptyList()
    }

    val tags = request { MarketplaceApi.getTags() }
    return names.map { name ->
        tags.find { it.name.equals(name, ignoreCase = true) }?.id
            ?: throw CommandException(
                t("error.unknownTag", variable(name), tags.joinToText(DEFAULT_SEPARATOR) { variable(it.name) })
            )
    }
}

internal fun CmdI18n.reportInstalled(result: ConfigTracker.LoadResult) {
    result.unavailable.forEach { chat(warning(it.text())) }

    if (result.installed.isEmpty()) {
        return
    }

    val names = result.installed.joinToText(DEFAULT_SEPARATOR) { variable(it.name) }
    chat(regular(t("load.installed", names)))
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
