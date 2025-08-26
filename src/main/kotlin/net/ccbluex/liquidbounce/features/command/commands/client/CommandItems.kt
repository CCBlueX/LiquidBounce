/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2025 CCBlueX
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
package net.ccbluex.liquidbounce.features.command.commands.client

import net.ccbluex.liquidbounce.features.command.Command
import net.ccbluex.liquidbounce.features.command.CommandException
import net.ccbluex.liquidbounce.features.command.CommandFactory
import net.ccbluex.liquidbounce.features.command.builder.CommandBuilder
import net.ccbluex.liquidbounce.features.command.builder.Parameters
import net.ccbluex.liquidbounce.features.command.preset.pagedQuery
import net.ccbluex.liquidbounce.utils.client.bold
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.copyable
import net.ccbluex.liquidbounce.utils.client.regular
import net.ccbluex.liquidbounce.utils.client.variable
import net.ccbluex.liquidbounce.utils.client.withColor
import net.ccbluex.liquidbounce.utils.inventory.DISALLOWED_BLOCKS_TO_PLACE
import net.minecraft.block.Block
import net.minecraft.registry.Registries
import net.minecraft.util.Formatting

/**
 * Items Command
 *
 * Allows you to add, remove, list, and clear blocks from the disallowed list.
 */
object CommandItems : CommandFactory {

    override fun createCommand(): Command {
        return CommandBuilder
            .begin("DISALLOWED_BLOCKS_TO_PLACE")
            .hub()
            .subcommand(addSubcommand())
            .subcommand(removeSubcommand())
            .subcommand(listSubcommand())
            .subcommand(clearSubcommand())
            .build()
    }

    private fun clearSubcommand() = CommandBuilder
        .begin("clear")
        .handler { command, _ ->
            DISALLOWED_BLOCKS_TO_PLACE.clear()
            chat(regular(command.result("blocksCleared")))
        }
        .build()

    private fun listSubcommand() = CommandBuilder
        .begin("list")
        .pagedQuery(
            pageSize = 8,
            header = {
                result("list").withColor(Formatting.RED).bold(true)
            },
            items = {
                DISALLOWED_BLOCKS_TO_PLACE.sortedBy { it.translationKey }
            },
            eachRow = { _, block ->
                regular("\u2B25 ")
                    .append(variable(block.name).copyable())
                    .append(regular(" ("))
                    .append(variable(Registries.BLOCK.getId(block).toString()).copyable())
                    .append(regular(")"))
            }
        )

    private fun removeSubcommand() = CommandBuilder
        .begin("remove")
        .parameter(
            Parameters.block()
                .required()
                .build()
        )
        .handler { command, args ->
            val block = args[0] as Block
            if (!DISALLOWED_BLOCKS_TO_PLACE.remove(block)) {
                throw CommandException(command.result("blockNotFound", block.name))
            }

            chat(regular(command.result("blockRemoved", block.name)))
        }
        .build()

    private fun addSubcommand() = CommandBuilder
        .begin("add")
        .parameter(
            Parameters.block()
                .required()
                .build()
        )
        .handler { command, args ->
            val block = args[0] as Block
            if (!DISALLOWED_BLOCKS_TO_PLACE.add(block)) {
                throw CommandException(command.result("blockIsPresent", block.name))
            }

            chat(regular(command.result("blockAdded", block.name)))
        }
        .build()
}
