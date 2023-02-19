/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2016 - 2023 CCBlueX
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
import net.ccbluex.liquidbounce.features.command.Command
import net.ccbluex.liquidbounce.features.command.builder.CommandBuilder
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.regular
import net.ccbluex.liquidbounce.utils.combat.globalEnemyConfigurable

object CommandEnemy {

    fun createCommand(): Command {
        return CommandBuilder
            .begin("enemy")
            .hub()
            .subcommand(
                CommandBuilder
                    .begin("players")
                    .handler { command, _ ->
                        globalEnemyConfigurable.players = !globalEnemyConfigurable.players

                        chat(
                            regular(
                                command.result(
                                    if (globalEnemyConfigurable.players) {
                                        "enabled"
                                    } else {
                                        "disabled"
                                    }
                                )
                            )
                        )

                        ConfigSystem.storeConfigurable(globalEnemyConfigurable)
                    }
                    .build()
            )
            .subcommand(
                CommandBuilder
                    .begin("players")
                    .handler { command, _ ->
                        globalEnemyConfigurable.mobs = !globalEnemyConfigurable.mobs

                        chat(
                            regular(
                                command.result(
                                    if (globalEnemyConfigurable.mobs) {
                                        "enabled"
                                    } else {
                                        "disabled"
                                    }
                                )
                            )
                        )

                        ConfigSystem.storeConfigurable(globalEnemyConfigurable)
                    }
                    .build()
            )
            .subcommand(
                CommandBuilder
                    .begin("animals")
                    .handler { command, _ ->
                        globalEnemyConfigurable.animals = !globalEnemyConfigurable.animals

                        chat(
                            regular(
                                command.result(
                                    if (globalEnemyConfigurable.animals) {
                                        "enabled"
                                    } else {
                                        "disabled"
                                    }
                                )
                            )
                        )

                        ConfigSystem.storeConfigurable(globalEnemyConfigurable)
                    }
                    .build()
            )
            .subcommand(
                CommandBuilder
                    .begin("invisible")
                    .handler { command, _ ->
                        globalEnemyConfigurable.invisible = !globalEnemyConfigurable.invisible

                        chat(
                            regular(
                                command.result(
                                    if (globalEnemyConfigurable.invisible) {
                                        "enabled"
                                    } else {
                                        "disabled"
                                    }
                                )
                            )
                        )

                        ConfigSystem.storeConfigurable(globalEnemyConfigurable)
                    }
                    .build()
            )
            .subcommand(
                CommandBuilder
                    .begin("dead")
                    .handler { command, _ ->
                        globalEnemyConfigurable.dead = !globalEnemyConfigurable.dead

                        chat(
                            regular(
                                command.result(
                                    if (globalEnemyConfigurable.dead) {
                                        "enabled"
                                    } else {
                                        "disabled"
                                    }
                                )
                            )
                        )

                        ConfigSystem.storeConfigurable(globalEnemyConfigurable)
                    }
                    .build()
            )
            .subcommand(
                CommandBuilder
                    .begin("friends")
                    .handler { command, _ ->
                        globalEnemyConfigurable.friends = !globalEnemyConfigurable.friends

                        chat(
                            regular(
                                command.result(
                                    if (globalEnemyConfigurable.friends) {
                                        "enabled"
                                    } else {
                                        "disabled"
                                    }
                                )
                            )
                        )

                        ConfigSystem.storeConfigurable(globalEnemyConfigurable)
                    }
                    .build()
            )
            .subcommand(
                CommandBuilder
                    .begin("teammates")
                    .handler { command, _ ->
                        globalEnemyConfigurable.teamMates = !globalEnemyConfigurable.teamMates

                        chat(
                            regular(
                                command.result(
                                    if (globalEnemyConfigurable.teamMates) {
                                        "enabled"
                                    } else {
                                        "disabled"
                                    }
                                )
                            )
                        )

                        ConfigSystem.storeConfigurable(globalEnemyConfigurable)
                    }
                    .build()
            )
            .build()
    }
}
