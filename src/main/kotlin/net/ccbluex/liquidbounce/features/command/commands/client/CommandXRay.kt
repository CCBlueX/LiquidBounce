/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2024 CCBlueX
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
import net.ccbluex.liquidbounce.features.command.CommandManager
import net.ccbluex.liquidbounce.features.command.builder.CommandBuilder
import net.ccbluex.liquidbounce.features.command.builder.blockParameter
import net.ccbluex.liquidbounce.features.command.builder.pageParameter
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleXRay
import net.ccbluex.liquidbounce.register.IncludeCommand
import net.ccbluex.liquidbounce.utils.client.asText
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.regular
import net.ccbluex.liquidbounce.utils.client.variable
import net.minecraft.registry.Registries
import net.minecraft.util.Formatting
import net.minecraft.util.Identifier
import kotlin.math.ceil
import kotlin.math.roundToInt

/**
 * XRay Command
 *
 * Allows you to add, remove, list, clear, and reset blocks for the XRay module.
 */
@IncludeCommand
object CommandXRay {

    fun createCommand(): Command {
        return CommandBuilder
            .begin("xray")
            .hub()
            .subcommand(
                CommandBuilder
                    .begin("add")
                    .parameter(
                        blockParameter()
                            .required()
                            .build()
                    )
                    .handler { command, args ->
                        val name = args[0] as String
                        val identifier = Identifier.tryParse(name)
                        val displayName = identifier.toString()

                        val block = Registries.BLOCK.getOrEmpty(identifier).orElseThrow {
                            throw CommandException(command.result("blockNotExists", displayName))
                        }

                        if (!ModuleXRay.blocks.add(block)) {
                            throw CommandException(command.result("blockIsPresent", displayName))
                        }

                        chat(regular(command.result("blockAdded", displayName)))
                    }
                    .build()
            )
            .subcommand(
                CommandBuilder
                    .begin("remove")
                    .parameter(
                        blockParameter()
                            .required()
                            .build()
                    )
                    .handler { command, args ->
                        val name = args[0] as String
                        val identifier = Identifier.tryParse(name)
                        val displayName = identifier.toString()

                        val block = Registries.BLOCK.getOrEmpty(identifier).orElseThrow {
                            throw CommandException(command.result("blockNotExists", displayName))
                        }

                        if (!ModuleXRay.blocks.remove(block)) {
                            throw CommandException(command.result("blockNotFound", displayName))
                        }

                        chat(regular(command.result("blockRemoved", displayName)))
                    }
                    .build()
            )
            .subcommand(
                CommandBuilder
                    .begin("list")
                    .parameter(
                        pageParameter()
                            .optional()
                            .build()
                    )
                    .handler { command, args ->
                        val page = if (args.size > 1) {
                            args[0] as Int
                        } else {
                            1
                        }.coerceAtLeast(1)

                        val blocks = ModuleXRay.blocks.sortedBy { it.translationKey }

                        // Max page
                        val maxPage = ceil(blocks.size / 8.0).roundToInt()
                        if (page > maxPage) {
                            throw CommandException(command.result("pageNumberTooLarge", maxPage))
                        }

                        // Print out help page
                        chat(command.result("list").styled { it.withColor(Formatting.RED).withBold(true) })
                        chat(regular(command.result("pageCount", variable("$page / $maxPage"))))

                        val iterPage = 8 * page
                        for (block in blocks.subList(iterPage - 8, iterPage.coerceAtMost(blocks.size))) {
                            val identifier = block.translationKey
                                .replace("block.", "")
                                .replace(".", ":")

                            chat(
                                block.name
                                    .styled { it.withColor(Formatting.GRAY) }
                                    .append(variable(" ("))
                                    .append(regular(identifier))
                                    .append(variable(")"))
                            )
                        }

                        chat(
                            "--- ".asText()
                                .styled { it.withColor(Formatting.DARK_GRAY) }
                                .append(variable("${CommandManager.Options.prefix}xray list <"))
                                .append(variable(command.result("page")))
                                .append(variable(">"))
                        )
                    }
                    .build()
            )
            .subcommand(
                CommandBuilder
                    .begin("clear")
                    .handler { command, _ ->
                        ModuleXRay.blocks.clear()
                        chat(regular(command.result("blocksCleared")))
                    }
                    .build()
            )
            .subcommand(
                CommandBuilder
                    .begin("reset")
                    .handler {command, _ ->
                        ModuleXRay.resetBlocks()
                        chat(regular(command.result("Reset the blocks to the default values")))
                    }
                    .build()
            )
            .build()
    }
}
