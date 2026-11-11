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
package net.ccbluex.liquidbounce.features.command.commands.client.config

import net.ccbluex.liquidbounce.api.models.marketplace.MarketplaceItem
import net.ccbluex.liquidbounce.features.command.CommandExecutor
import net.ccbluex.liquidbounce.features.command.CommandManager
import net.ccbluex.liquidbounce.features.command.brigadier.CmdI18n
import net.ccbluex.liquidbounce.features.marketplace.autoconfig.ConfigTracker
import net.ccbluex.liquidbounce.features.marketplace.autoconfig.MarketplaceConfigs.address
import net.ccbluex.liquidbounce.utils.client.MessageMetadata
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.highlight
import net.ccbluex.liquidbounce.utils.client.onClick
import net.ccbluex.liquidbounce.utils.client.onClickRun
import net.ccbluex.liquidbounce.utils.client.onHover
import net.ccbluex.liquidbounce.utils.client.protocolVersion
import net.ccbluex.liquidbounce.utils.client.regular
import net.ccbluex.liquidbounce.utils.client.variable
import net.ccbluex.liquidbounce.utils.text.asPlainText
import net.ccbluex.liquidbounce.utils.text.asText
import net.ccbluex.liquidbounce.utils.text.joinToText
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.ClickEvent
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.ComponentUtils.DEFAULT_SEPARATOR
import net.minecraft.network.chat.HoverEvent
import net.minecraft.network.chat.MutableComponent
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit

internal val plain = MessageMetadata(prefix = false)

private const val SEPARATOR = " · "

internal fun header(title: Component, detail: Component? = null) {
    chat(regular(""), metadata = plain)
    chat(
        highlight(title.copy()).apply {
            detail?.let { append(regular("  ")).append(regular(it.copy())) }
        },
        metadata = plain
    )
}

internal fun CmdI18n.addressText(item: MarketplaceItem): MutableComponent = regular("")
    .apply { item.author?.let { append(regular("$it/")) } }
    .append(variable(item.name))
    .onClick(ClickEvent.SuggestCommand("${CommandManager.GlobalSettings.prefix}config load ${quoted(item.address)}"))
    .onHover(HoverEvent.ShowText(regular(t("list.hover", variable(item.address)))))

internal fun isTracked(item: MarketplaceItem) =
    ConfigTracker.state != ConfigTracker.State.NONE && ConfigTracker.itemId == item.id

internal fun CmdI18n.trackedMarker(item: MarketplaceItem): MutableComponent? = if (isTracked(item)) {
    " ●".asText().withStyle(ChatFormatting.GREEN)
        .onHover(HoverEvent.ShowText(regular(t("state.${ConfigTracker.state.tag.lowercase()}"))))
} else {
    null
}

/**
 * The reports of the last 30 days, each a button to report the config.
 */
internal fun CmdI18n.votes(item: MarketplaceItem): MutableComponent =
    vote(item, works = true).append(regular(" ")).append(vote(item, works = false))

private fun CmdI18n.vote(item: MarketplaceItem, works: Boolean): MutableComponent {
    val verdict = if (works) "works" else "broken"
    val button = if (works) {
        "[✔ ${item.recentWorks}]".asText().withStyle(ChatFormatting.GREEN)
    } else {
        "[✘ ${item.recentFails}]".asText().withStyle(ChatFormatting.RED)
    }

    return button
        .onHover(HoverEvent.ShowText(regular(t("vote.$verdict", variable(item.address)))))
        .onClickRun {
            runCatching {
                CommandManager.execute("config report $verdict ${quoted(item.address)}")
            }.onFailure(CommandExecutor::handleExceptions)
        }
}

internal fun protocolText(item: MarketplaceItem): Component? {
    val name = item.protocolName ?: return null
    val matches = item.protocolVersion == null || item.protocolVersion == protocolVersion.version
    return name.asPlainText(if (matches) ChatFormatting.GREEN else ChatFormatting.RED)
}

private const val DAYS_PER_WEEK = 7
private const val DAYS_SHOWN_RELATIVE = 28

/**
 * [dateTime] is in UTC, as the API sends it.
 */
internal fun CmdI18n.ago(dateTime: String): MutableComponent {
    val exact = formatDate(dateTime)
    val days = runCatching {
        ChronoUnit.DAYS.between(LocalDateTime.parse(dateTime).toLocalDate(), LocalDate.now(ZoneOffset.UTC))
    }.getOrNull()

    val text = when {
        days == null || days > DAYS_SHOWN_RELATIVE -> variable(exact)
        days <= 0L -> variable(t("date.today"))
        days == 1L -> variable(t("date.yesterday"))
        days < DAYS_PER_WEEK -> variable(t("date.days", days))
        days < 2 * DAYS_PER_WEEK -> variable(t("date.week"))
        else -> variable(t("date.weeks", days / DAYS_PER_WEEK))
    }
    return text.onHover(HoverEvent.ShowText(variable(exact)))
}

internal fun CmdI18n.details(item: MarketplaceItem): MutableComponent? {
    val parts = listOfNotNull(
        item.targetServers?.takeIf { it.isNotEmpty() }?.joinToText(DEFAULT_SEPARATOR, ::variable),
        protocolText(item),
        item.tags?.takeIf { it.isNotEmpty() }?.let { tags -> tags.joinToText(DEFAULT_SEPARATOR) { regular(it.name) } },
        item.updatedAt?.let { regular(t("list.updated", ago(it))) },
    )
    if (parts.isEmpty()) {
        return null
    }

    return parts.drop(1).fold(regular("   ").append(parts.first())) { line, part ->
        line.append(regular(SEPARATOR)).append(part)
    }
}
