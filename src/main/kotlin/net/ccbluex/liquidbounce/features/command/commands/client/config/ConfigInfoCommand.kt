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

import net.ccbluex.liquidbounce.api.models.auth.ClientAccount
import net.ccbluex.liquidbounce.api.models.marketplace.MarketplaceItem
import net.ccbluex.liquidbounce.api.services.marketplace.MarketplaceApi
import net.ccbluex.liquidbounce.features.command.CommandManager
import net.ccbluex.liquidbounce.features.command.arguments.ClientStringArgumentType
import net.ccbluex.liquidbounce.features.command.brigadier.CmdI18n
import net.ccbluex.liquidbounce.features.command.brigadier.CmdLiteralScope
import net.ccbluex.liquidbounce.features.command.brigadier.get
import net.ccbluex.liquidbounce.features.cosmetic.ClientAccountManager
import net.ccbluex.liquidbounce.features.marketplace.autoconfig.ConfigTracker
import net.ccbluex.liquidbounce.utils.client.MessageMetadata
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.copyable
import net.ccbluex.liquidbounce.utils.client.highlight
import net.ccbluex.liquidbounce.utils.client.onClick
import net.ccbluex.liquidbounce.utils.client.regular
import net.ccbluex.liquidbounce.utils.client.variable
import net.minecraft.network.chat.ClickEvent

/**
 * Shows a marketplace config, by default the tracked one
 */
object ConfigInfoCommand {

    private const val DESCRIPTION_PREVIEW = 200

    fun CmdLiteralScope.info() {
        literal("info") {
            optional(
                "config",
                ClientStringArgumentType.string(),
                default = null,
                suggests = configSuggestions
            ) { config ->
                execSuspend { ctx ->
                    val input = ctx.get(config)
                    val id = if (input == null) {
                        requireTracked()
                        ConfigTracker.itemId
                    } else {
                        resolveConfig(input).id
                    }

                    val loggedIn = ClientAccountManager.clientAccount != ClientAccount.EMPTY_ACCOUNT
                    val item = request { MarketplaceApi.getMarketplaceItem(id, if (loggedIn) session() else null) }
                    printInfo(item)
                }
            }
        }
    }

    private suspend fun CmdI18n.printInfo(item: MarketplaceItem) {
        val metadata = MessageMetadata(prefix = false)
        val tracked = ConfigTracker.state != ConfigTracker.State.NONE && ConfigTracker.itemId == item.id

        chat(highlight(item.name), metadata = metadata)
        item.author?.let { chat(regular(t("list.by", variable(it))), metadata = metadata) }
        if (tracked) {
            val state = t("state.${ConfigTracker.state.tag.lowercase()}")
            chat(regular(t("info.state", variable(state))), metadata = metadata)
        }

        item.description.lineSequence().firstOrNull { it.isNotBlank() }?.let {
            chat(regular(it.take(DESCRIPTION_PREVIEW)), metadata = metadata)
        }
        item.tags?.takeIf { it.isNotEmpty() }?.let { tags ->
            chat(regular(t("info.tags", variable(tags.joinToString(", ") { it.name }))), metadata = metadata)
        }
        item.targetServers?.takeIf { it.isNotEmpty() }?.let {
            chat(regular(t("info.servers", variable(it.joinToString(", ")))), metadata = metadata)
        }
        item.forkedFromItemId?.let { source ->
            chat(
                regular(t("info.forkedFrom", variable(source.toString())))
                    .onClick(ClickEvent.SuggestCommand("${CommandManager.GlobalSettings.prefix}config info $source")),
                metadata = metadata
            )
        }
        if (item.includesBinds == true) {
            chat(regular(t("info.binds")), metadata = metadata)
        }
        chat(
            regular(t("info.reports", variable(item.recentWorks.toString()), variable(item.recentFails.toString()))),
            metadata = metadata
        )
        item.liveRevisionId?.let { revisionId ->
            val revision = request { MarketplaceApi.getMarketplaceItemRevision(item.id, revisionId) }
            chat(regular(t("info.updated", variable(formatDate(revision.createdAt)))), metadata = metadata)
        }
        item.shareCode?.let { code ->
            chat(regular(t("info.shareCode", variable(code).copyable())), metadata = metadata)
        }
    }

}
