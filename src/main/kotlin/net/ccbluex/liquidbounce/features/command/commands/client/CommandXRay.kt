/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2016 - 2021 CCBlueX
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
import net.ccbluex.liquidbounce.features.command.builder.ParameterBuilder
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleXRay
import net.ccbluex.liquidbounce.utils.client.asText
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.regular
import net.ccbluex.liquidbounce.utils.client.variable
import net.minecraft.text.TranslatableText
import net.minecraft.util.Formatting
import net.minecraft.util.Identifier
import net.minecraft.util.registry.Registry
import kotlin.math.ceil
import kotlin.math.roundToInt

object CommandXRay {

    fun createCommand(): Command {
        return CommandBuilder
            .begin("xray")
            .parameter(
                ParameterBuilder
                    .begin<String>("option")
                    .verifiedBy(ParameterBuilder.STRING_VALIDATOR)
                    .required()
                    .build()
            )
            .parameter(
                ParameterBuilder
                    .begin<String>("id")
                    .verifiedBy(ParameterBuilder.STRING_VALIDATOR)
                    .required()
                    .build()
            )
            .subcommand(subCommandList())
            .handler { command, args ->
                val option = args[0] as String
                val id = args[1] as String

                if ("add".equals(option, true)) {
                    val mcId = Identifier.tryParse(id)
                    val block = Registry.BLOCK.get(mcId)
                    ModuleXRay.blocks.add(block)
                    chat(regular("Added ${mcId.toString()}"))
                    return@handler
                }

                if ("remove".equals(option, true)) {
                    val mcId = Identifier.tryParse(id)
                    val block = Registry.BLOCK.get(mcId)
                    ModuleXRay.blocks.remove(block)
                    chat(regular("Removed ${mcId.toString()}"))
                    return@handler
                }
                throw CommandException(command.result("valueNotFound", id))
            }
            .build()
    }

    private fun subCommandList(): Command {
        return CommandBuilder
            .begin("list")
            .parameter(
                ParameterBuilder
                    .begin<Int>("page")
                    .verifiedBy(ParameterBuilder.INTEGER_VALIDATOR)
                    .optional()
                    .build()
            )
            .handler { command, args ->
                val page = if (args.size > 1) {
                    args[0] as Int
                } else {
                    1
                }.coerceAtLeast(1)

                val blocks = ModuleXRay.blocks.sortedBy { Registry.BLOCK.getId(it).toString() }

                // Max page
                val maxPage = ceil(blocks.size / 8.0).roundToInt()
                if (page > maxPage) {
                    throw CommandException(TranslatableText("liquidbounce.command.help.result.pageNumberTooLarge", maxPage))
                }

                // Print out help page
                chat("Xray List".asText().styled { it.withColor(Formatting.RED).withBold(true) })
                chat(regular(TranslatableText("liquidbounce.command.help.result.pageCount", variable("$page / $maxPage"))))

                val iterPage = 8 * page
                for (block in blocks.subList(iterPage - 8, iterPage.coerceAtMost(blocks.size))) {
                    chat(
                        block.translationKey.replaceFirst("block.", "").asText()
                            .styled { it.withColor(Formatting.GRAY) }
                    )
                }

                chat(
                    "--- ".asText()
                        .styled { it.withColor(Formatting.DARK_GRAY) }
                        .append(variable("${CommandManager.Options.prefix}xray list <"))
                        .append(variable(TranslatableText("liquidbounce.command.help.result.page")))
                        .append(variable(">"))
                )
            }
            .build()
    }
}
