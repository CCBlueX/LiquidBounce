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

import com.mojang.brigadier.arguments.IntegerArgumentType
import net.ccbluex.liquidbounce.features.command.CommandException
import net.ccbluex.liquidbounce.features.command.brigadier.CmdI18n
import net.ccbluex.liquidbounce.features.command.brigadier.CmdLiteralScope
import net.ccbluex.liquidbounce.features.command.brigadier.get
import net.ccbluex.liquidbounce.features.command.brigadier.suggestions
import net.ccbluex.liquidbounce.features.marketplace.MarketplaceManager
import net.ccbluex.liquidbounce.features.marketplace.SubscribedItem
import net.ccbluex.liquidbounce.features.marketplace.UpdateResult
import net.ccbluex.liquidbounce.utils.client.MessageMetadata
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.logger
import net.ccbluex.liquidbounce.utils.client.markAsError
import net.ccbluex.liquidbounce.utils.client.regular
import net.ccbluex.liquidbounce.utils.client.variable

/**
 * Subscribe to marketplace item
 */
object MarketplaceUpdateCommand {

    private const val MESSAGE_ID = "CMarketplace#update"

    fun CmdLiteralScope.update() {
        literal("update") {
            optional(
                "id",
                IntegerArgumentType.integer(1),
                default = null,
                suggestions { MarketplaceManager.subscribedItems.map { it.id.toString() } },
            ) { id ->
                execSuspend { ctx ->
                    this@update.update(ctx.get(id))
                }
            }
        }
    }

    private suspend fun CmdI18n.update(id: Int?) {
        if (id != null) {
            val item = MarketplaceManager.getItem(id)
                ?: throw CommandException(
                    t("error.itemNotFound",
                        variable(id.toString())
                    )
                )

            updateSingle(item)
        } else {
            updateAll()
        }
    }

    private suspend fun CmdI18n.updateAll() {
        if (MarketplaceManager.subscribedItems.isEmpty()) {
            throw CommandException(
                t("update.noSubscribedItems")
            )
        }

        chat(regular(t("update.updatingAll")), metadata = MessageMetadata(id = MESSAGE_ID))
        val results = MarketplaceManager.updateAll().onEach { report(it) }
        val failed = results.count { it is UpdateResult.Failed }
        if (failed > 0) {
            throw CommandException(t("update.updatedAllWithFailures", failed, results.size))
        }
        chat(regular(t("update.updatedAll")), metadata = MessageMetadata(id = MESSAGE_ID))
    }

    private suspend fun CmdI18n.updateSingle(item: SubscribedItem) {
        try {
            report(MarketplaceManager.update(item))
        } catch (@Suppress("SwallowedException") e: Exception) {
            logger.error("Failed to update item ${item.id}", e)

            throw CommandException(
                t("error.updateFailed",
                    variable(item.id.toString()),
                    variable(e.message ?: "Unknown error")
                )
            )
        }
    }

    private fun CmdI18n.report(result: UpdateResult) {
        when (result) {
            is UpdateResult.Updated -> chat(
                regular(
                    t("update.success",
                        variable(result.item.id.toString()),
                        variable(result.revisionId.toString())
                    )
                ),
                metadata = MessageMetadata(id = "$MESSAGE_ID#${result.item.id}")
            )
            is UpdateResult.NoUpdate -> chat(
                regular(t("update.noUpdate", variable(result.item.id.toString()))),
                metadata = MessageMetadata(id = "$MESSAGE_ID#${result.item.id}")
            )
            is UpdateResult.Failed -> chat(
                markAsError(
                    t("error.updateFailed",
                        variable(result.item.id.toString()),
                        variable(result.error.message ?: "Unknown error")
                    )
                ),
                metadata = MessageMetadata(id = "$MESSAGE_ID#${result.item.id}")
            )
        }
    }

}
