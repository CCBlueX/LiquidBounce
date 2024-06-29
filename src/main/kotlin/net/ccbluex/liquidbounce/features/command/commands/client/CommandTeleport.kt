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
import net.ccbluex.liquidbounce.features.command.builder.CommandBuilder
import net.ccbluex.liquidbounce.features.command.builder.ParameterBuilder
import net.ccbluex.liquidbounce.features.module.QuickImports
import net.ccbluex.liquidbounce.features.module.modules.combat.ModuleCriticals
import net.ccbluex.liquidbounce.features.module.modules.movement.ModuleTeleport
import net.ccbluex.liquidbounce.utils.client.MovePacketType
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.regular
import net.ccbluex.liquidbounce.utils.client.variable
import java.text.DecimalFormat
import kotlin.math.abs
import kotlin.math.floor

/**
 * Teleport Command
 *
 * Allows you to teleport.
 */
object CommandTeleport : QuickImports {

    private val decimalFormat = DecimalFormat("##0.000")

    fun createCommand(): Command {
        return CommandBuilder
            .begin("teleport")
            .alias("tp")
            .parameter(
                ParameterBuilder
                    .begin<Float>("x")
                    .required()
                    .build(),
            )
            .parameter(
                ParameterBuilder
                    .begin<Float>("y|z")
                    .required()
                    .build()
            )
            .parameter(
                ParameterBuilder
                    .begin<Float>("z")
                    .optional()
                    .build()
            )
            .handler { command, args ->
                val x =
                    (args[0] as String).toDoubleOrNull()
                val z =
                    (args[args.size - 1] as String).toDoubleOrNull()
                val y = if (args.size == 3) {
                    (args[1] as String).toDoubleOrNull()
                } else {
                    if (ModuleTeleport.highTp) {
                        ModuleTeleport.highTpAmount
                    } else {
                        player.y
                    }
                }

                if (x == null || y == null || z == null) {
                    throw CommandException(command.result("invalidCoordinates"))
                }

                ModuleTeleport.indicateTeleport(x, y.toDouble(), z)

                chat(
                    regular(
                        command.result(
                            "positionUpdated",
                            variable(decimalFormat.format(player.x)),
                            variable(decimalFormat.format(player.y)),
                            variable(decimalFormat.format(player.z))
                        )
                    )
                )
            }
            .build()
    }
}
