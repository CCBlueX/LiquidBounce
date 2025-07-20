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
package net.ccbluex.liquidbounce.features.command.commands.client.client

import net.ccbluex.liquidbounce.features.command.builder.CommandBuilder
import net.ccbluex.liquidbounce.features.command.builder.ParameterBuilder
import net.ccbluex.liquidbounce.integration.theme.component.ComponentOverlay
import net.ccbluex.liquidbounce.integration.theme.component.components
import net.ccbluex.liquidbounce.integration.theme.component.customComponents
import net.ccbluex.liquidbounce.integration.theme.component.types.ImageComponent
import net.ccbluex.liquidbounce.integration.theme.component.types.TextComponent
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.regular

object CommandClientComponentSubcommand {
    fun componentCommand() = CommandBuilder.begin("component")
        .hub()
        .subcommand(listSubcommand())
        .subcommand(addSubcommand())
        .subcommand(removeSubcommand())
        .subcommand(clearSubcommand())
        .subcommand(updateSubcommand())
        .build()

    private fun updateSubcommand() = CommandBuilder.begin("update")
        .handler { command, args ->
            ComponentOverlay.fireComponentsUpdate()

            chat("Successfully updated components.")
        }.build()

    private fun clearSubcommand() = CommandBuilder.begin("clear")
        .handler { command, args ->
            customComponents.clear()
            ComponentOverlay.fireComponentsUpdate()

            chat("Successfully cleared components.")
        }.build()

    private fun removeSubcommand() = CommandBuilder.begin("remove")
        .parameter(
            ParameterBuilder.begin<Int>("id")
                .verifiedBy(ParameterBuilder.INTEGER_VALIDATOR).required()
                .build()
        ).handler { command, args ->
            val index = args[-1] as Int
            val component = customComponents.getOrNull(index)

            if (component == null) {
                chat(regular("Component ID is out of range."))
                return@handler
            }

            customComponents -= component
            ComponentOverlay.fireComponentsUpdate()
            chat("Successfully removed component.")
        }.build()

    private fun addSubcommand() = CommandBuilder.begin("add")
        .hub()
        .subcommand(
            CommandBuilder.begin("text")
                    .parameter(
                        ParameterBuilder.begin<String>("text")
                            .vararg()
                            .verifiedBy(ParameterBuilder.STRING_VALIDATOR).required()
                            .build()
                    ).handler { command, args ->
                        val arg = (args[-1] as Array<*>).joinToString(" ") { it as String }
                        customComponents += TextComponent(arg)
                        ComponentOverlay.fireComponentsUpdate()

                    chat("Successfully added text component.")
                    }.build()
        )
        .subcommand(
            CommandBuilder.begin("image")
                    .parameter(
                        ParameterBuilder.begin<String>("url")
                            .vararg()
                            .verifiedBy(ParameterBuilder.STRING_VALIDATOR).required()
                            .build()
                    ).handler { command, args ->
                        val arg = (args[-1] as Array<*>).joinToString(" ") { it as String }
                        customComponents += ImageComponent(arg)
                        ComponentOverlay.fireComponentsUpdate()

                    chat("Successfully added image component.")
                    }.build()
        )
        .build()

    private fun listSubcommand() = CommandBuilder.begin("list")
        .handler { command, args ->
            chat(regular("In-built:"))
            for (component in components) {
                chat(regular("-> ${component.name}"))
            }

            chat(regular("Custom:"))
            for ((index, component) in customComponents.withIndex()) {
                chat(regular("-> ${component.name} (#$index}"))
            }
        }.build()
}
