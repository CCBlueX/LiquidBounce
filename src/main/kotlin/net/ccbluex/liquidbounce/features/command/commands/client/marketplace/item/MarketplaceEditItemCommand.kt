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
package net.ccbluex.liquidbounce.features.command.commands.client.marketplace.item

import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import net.ccbluex.liquidbounce.features.command.arguments.ClientStringArgumentType
import net.ccbluex.liquidbounce.api.models.marketplace.MarketplaceItemType
import net.ccbluex.liquidbounce.api.services.marketplace.MarketplaceApi
import net.ccbluex.liquidbounce.features.command.arguments.TaggedArgumentType
import net.ccbluex.liquidbounce.features.command.brigadier.CmdLiteralScope
import net.ccbluex.liquidbounce.features.command.brigadier.get
import net.ccbluex.liquidbounce.features.command.preset.accountOrException
import net.ccbluex.liquidbounce.features.cosmetic.ClientAccountManager
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.regular
import net.ccbluex.liquidbounce.utils.client.variable

/**
 * Edit marketplace item
 */
object MarketplaceEditItemCommand {

    fun CmdLiteralScope.edit() {
        literal("edit") {
            argument("id", IntegerArgumentType.integer(1)) { id ->
                argument("name", ClientStringArgumentType.word()) { name ->
                    argument(
                        "type",
                        TaggedArgumentType<MarketplaceItemType>("type") { it.isListable },
                    ) { type ->
                        argument("description", StringArgumentType.greedyString()) { description ->
                            execSuspend { ctx ->
                                val clientAccount = ClientAccountManager.accountOrException()

                                val response = MarketplaceApi.updateMarketplaceItem(
                                    clientAccount.takeSession(),
                                    ctx.get(id),
                                    ctx.get(name),
                                    ctx.get(type),
                                    ctx.get(description)
                                )

                                chat(
                                    regular(
                                        t("item.edit.success",
                                            variable(response.id.toString()),
                                            variable(response.name)
                                        )
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }

}
