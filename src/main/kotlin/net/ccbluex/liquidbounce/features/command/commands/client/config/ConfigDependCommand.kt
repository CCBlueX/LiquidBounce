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
import net.ccbluex.liquidbounce.api.services.marketplace.MarketplaceApi
import net.ccbluex.liquidbounce.features.command.CommandException
import net.ccbluex.liquidbounce.features.command.arguments.ClientStringArgumentType
import net.ccbluex.liquidbounce.features.command.brigadier.CmdI18n
import net.ccbluex.liquidbounce.features.command.brigadier.CmdLiteralScope
import net.ccbluex.liquidbounce.features.command.brigadier.get
import net.ccbluex.liquidbounce.features.marketplace.autoconfig.ConfigTracker
import net.ccbluex.liquidbounce.features.marketplace.autoconfig.MarketplaceConfigs
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.regular
import net.ccbluex.liquidbounce.utils.client.variable

/**
 * Adds and removes what the tracked config depends on: add-ons, scripts and other configs
 */
object ConfigDependCommand {

    fun CmdLiteralScope.depend() {
        literal("depend") {
            literal("add") {
                argument("item", ClientStringArgumentType.string(), suggests = configSuggestions) { input ->
                    execSuspend { ctx ->
                        requireOwnTracked()
                        val dependency = resolveDependency(ctx.get(input))
                        request { MarketplaceApi.addItemDependency(session(), ConfigTracker.itemId, dependency.id) }
                        chat(regular(t("depend.added", variable(ConfigTracker.itemName), variable(dependency.name))))
                    }
                }
            }
            literal("remove") {
                argument("item", ClientStringArgumentType.string(), suggests = configSuggestions) { input ->
                    execSuspend { ctx ->
                        requireOwnTracked()
                        val dependency = resolveDependency(ctx.get(input))
                        request { MarketplaceApi.removeItemDependency(session(), ConfigTracker.itemId, dependency.id) }
                        chat(regular(t("depend.removed", variable(ConfigTracker.itemName), variable(dependency.name))))
                    }
                }
            }
        }
    }

    private suspend fun CmdI18n.resolveDependency(input: String): MarketplaceItem =
        MarketplaceConfigs.findDependency(input) ?: throw CommandException(t("error.notFound", variable(input)))

}
