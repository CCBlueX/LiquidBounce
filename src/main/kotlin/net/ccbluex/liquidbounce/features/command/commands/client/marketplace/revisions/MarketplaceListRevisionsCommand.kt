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
package net.ccbluex.liquidbounce.features.command.commands.client.marketplace.revisions

import com.mojang.brigadier.arguments.IntegerArgumentType
import net.ccbluex.liquidbounce.api.models.marketplace.MarketplaceItemStatus
import net.ccbluex.liquidbounce.api.services.marketplace.MarketplaceApi
import net.ccbluex.liquidbounce.features.command.brigadier.CmdLiteralScope
import net.ccbluex.liquidbounce.features.command.brigadier.get
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.regular
import net.ccbluex.liquidbounce.utils.client.variable

/**
 * List marketplace item revisions
 */
object MarketplaceListRevisionsCommand {

    fun CmdLiteralScope.revisionsList() {
        literal("list") {
            argument("id", IntegerArgumentType.integer(1)) { id ->
                execSuspend { ctx ->
                    val itemId = ctx.get(id)

                    val response = MarketplaceApi.getMarketplaceItemRevisions(itemId)

                    // Filter out pending revisions
                    val activeRevisions = response.items.filter { it.status != MarketplaceItemStatus.PENDING }

                    if (activeRevisions.isEmpty()) {
                        chat(regular(t("revisions.list.noRevisions")))
                        return@execSuspend
                    }

                    chat(regular(t("revisions.list.header", variable(itemId.toString()))))

                    for (revision in activeRevisions) {
                        chat(
                            regular(
                                t("revisions.list.revision",
                                    variable(revision.version),
                                    variable(revision.createdAt)
                                )
                            )
                        )
                    }
                }
            }
        }
    }

}
