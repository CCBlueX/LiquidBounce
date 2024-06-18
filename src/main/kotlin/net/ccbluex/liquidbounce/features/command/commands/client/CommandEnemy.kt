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

import net.ccbluex.liquidbounce.config.ConfigSystem
import net.ccbluex.liquidbounce.config.ValueType
import net.ccbluex.liquidbounce.features.command.Command
import net.ccbluex.liquidbounce.features.command.builder.CommandBuilder
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.regular
import net.ccbluex.liquidbounce.utils.combat.globalEnemyConfigurable

/**
 * Enemy Command
 *
 * Provides subcommands for enemy configuration.
 */
object CommandEnemy {

    fun createCommand(): Command {
        val hubCommand = CommandBuilder
            .begin("enemy")
            .alias("enemies", "target", "targets")
            .hub()

        // Create sub-command for each value entry
        for (entry in globalEnemyConfigurable.inner) {
            // Should not happen, but I prefer to check it for the future in case of changes
            if (entry.valueType != ValueType.BOOLEAN) {
                continue
            }

            hubCommand.subcommand(
                CommandBuilder
                    .begin(entry.loweredName)
                    .handler { command, _ ->
                        // Since we know it is a boolean, we will cast it and flip the value
                        val state = !(entry.get() as Boolean)
                        // Hacky way to update the value, but it works
                        entry.setByString(state.toString())

                        val localizedState = if (state) {
                            "enabled"
                        } else {
                            "disabled"
                        }
                        chat(regular(command.result(localizedState)))
                        ConfigSystem.storeConfigurable(globalEnemyConfigurable)
                    }
                    .build()
            )
        }

        return hubCommand.build()
    }
}
