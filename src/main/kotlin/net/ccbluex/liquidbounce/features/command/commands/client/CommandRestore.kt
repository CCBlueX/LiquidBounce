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

import net.ccbluex.liquidbounce.config.Configurable
import net.ccbluex.liquidbounce.config.Value
import net.ccbluex.liquidbounce.event.Listenable
import net.ccbluex.liquidbounce.event.events.RegistryChange
import net.ccbluex.liquidbounce.event.events.RegistryChangeEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.command.Command
import net.ccbluex.liquidbounce.features.command.builder.CommandBuilder
import net.ccbluex.liquidbounce.lang.translation
import net.ccbluex.liquidbounce.utils.client.MessageMetadata
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.regular
import net.ccbluex.liquidbounce.utils.client.variable

/**
 * Restore Command
 *
 * Allows you to restore original configurations.
 */
object CommandRestore : Listenable {

    var command = CommandBuilder.begin("restore").hub().build()

    fun createCommand() = command

    @Suppress("unused")
    val handleModules = handler<RegistryChangeEvent> { event ->
        when (event.change) {
            RegistryChange.ADD -> registerCommands(null, command, event.module, event.module)
            RegistryChange.REMOVE -> {
                val newSubCommands = command.subcommands.filter { it.name != event.module.name }.toTypedArray()
                command.subcommands = newSubCommands
            }
        }
    }

    private fun registerCommands(
        commandBuilder: CommandBuilder?,
        command: Command?,
        configurable: Configurable,
        module: Configurable
    ) {
        val commandBuilder1 = CommandBuilder
            .begin(configurable.name)
            .handler { _, _ -> handle(module, configurable) }
            .hub()

        configurable.inner.forEach { value ->
            if (value is Configurable) {
                registerCommands(commandBuilder1, null, value, module)
            } else {
                commandBuilder1.subcommand(
                    CommandBuilder
                        .begin(value.name)
                        .handler { _, _ -> handle(module, value) }
                        .build()
                )
            }
        }

        val command1 = commandBuilder1.build()
        commandBuilder?.subcommand(command1)
        command?.let { it.subcommands += command1 }
    }

    private fun handle(module: Configurable, value: Value<*>) {
        value.restore()

        if (value is Configurable) {
            chat(
                regular(
                    translation("liquidbounce.command.restore.configurable.result.success", variable(value.name))
                ), metadata = MessageMetadata(id = "${module.name}#restored")
            )
        } else {
            chat(
                regular(
                    translation(
                        "liquidbounce.command.restore.result.success",
                        variable(value.name),
                        variable(module.name)
                    )
                ), metadata = MessageMetadata(id = "${module.name}#restored")
            )
        }
    }

}
