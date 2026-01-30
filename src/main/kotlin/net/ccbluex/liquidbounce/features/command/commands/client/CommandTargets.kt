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
package net.ccbluex.liquidbounce.features.command.commands.client

import net.ccbluex.liquidbounce.config.types.list.MultiChoiceListValue
import net.ccbluex.liquidbounce.features.command.Command
import net.ccbluex.liquidbounce.features.command.builder.CommandBuilder
import net.ccbluex.liquidbounce.features.command.builder.ParameterBuilder
import net.ccbluex.liquidbounce.features.command.builder.enumChoice
import net.ccbluex.liquidbounce.features.global.GlobalSettingsTarget
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleClickGui
import net.ccbluex.liquidbounce.utils.client.MessageMetadata
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.regular
import net.ccbluex.liquidbounce.utils.combat.Targets

/**
 * Enemy Command
 *
 * Provides subcommands for enemy configuration.
 */
object CommandTargets : Command.Factory {

    override fun createCommand() = CommandBuilder
        .begin("targets")
        .alias("target", "enemies", "enemy")
        .subcommand(
            CommandBuilder
                .begin("combat")
                .fromTargets(GlobalSettingsTarget.combatChoices)
                .build()
        )
        .subcommand(
            CommandBuilder
                .begin("visual")
                .fromTargets(GlobalSettingsTarget.visualChoices)
                .build()
        )
        .hub()
        .build()

    private fun CommandBuilder.fromTargets(targets: MultiChoiceListValue<Targets>): CommandBuilder {
        this.parameter(
            ParameterBuilder
                .enumChoice<Targets>("category") { it in targets.choices }
                .required()
                .build()
        ).handler {
            val entry = args[0] as Targets

            val state = targets.toggle(entry)
            val localizedState = if (state) {
                "enabled"
            } else {
                "disabled"
            }
            chat(
                regular(command.result(localizedState,
                    entry.name.lowercase().replaceFirstChar { it.uppercase() })
                ),
                metadata = MessageMetadata(id = "CTargets#info")
            )

            ModuleClickGui.sync()
        }

        return this
    }

}
