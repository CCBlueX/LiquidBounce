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

import com.mojang.brigadier.arguments.StringArgumentType
import net.ccbluex.liquidbounce.features.command.CommandException
import net.ccbluex.liquidbounce.features.command.brigadier.CmdLiteralScope
import net.ccbluex.liquidbounce.features.command.brigadier.get
import net.ccbluex.liquidbounce.features.marketplace.autoconfig.ConfigTracker
import net.ccbluex.liquidbounce.features.marketplace.autoconfig.MarketplaceConfigs
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.regular
import net.ccbluex.liquidbounce.utils.client.variable

/**
 * Changes the loaded config on the marketplace, for its author only
 */
object ConfigEditCommand {

    fun CmdLiteralScope.edit() {
        literal("edit") {
            update()
            with(ConfigSetCommand) { set() }
            with(ConfigDependCommand) { depend() }
            delete()
        }
    }

    private fun CmdLiteralScope.update() {
        literal("update") {
            optional("changelog", StringArgumentType.greedyString(), default = null) { changelog ->
                execSuspend { ctx ->
                    requireOwnTracked()
                    if (ConfigTracker.state != ConfigTracker.State.EDITING) {
                        throw CommandException(t("update.notEditing", variable(ConfigTracker.itemName)))
                    }

                    request { ConfigTracker.update(session(), ctx.get(changelog)) }
                    chat(regular(t("update.updated", variable(ConfigTracker.itemName))))
                }
            }
        }
    }

    private fun CmdLiteralScope.delete() {
        literal("delete") {
            execSuspend {
                requireOwnTracked()
                val name = ConfigTracker.itemName
                request { ConfigTracker.delete(session()) }
                MarketplaceConfigs.refresh()
                chat(regular(t("delete.deleted", variable(name))))
            }
        }
    }

}
