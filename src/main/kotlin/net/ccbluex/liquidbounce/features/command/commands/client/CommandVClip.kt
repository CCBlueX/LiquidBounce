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
import net.ccbluex.liquidbounce.utils.client.MovePacketType
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.regular
import net.ccbluex.liquidbounce.utils.client.variable
import java.text.DecimalFormat
import kotlin.math.abs
import kotlin.math.floor

/**
 * VClip Command
 *
 * Allows you to clip through blocks.
 */
object CommandVClip : QuickImports {

    private val decimalFormat = DecimalFormat("##0.000")

    fun createCommand(): Command {
        return CommandBuilder
            .begin("vclip")
            .parameter(
                ParameterBuilder
                    .begin<Float>("distance")
                    .required()
                    .build()
            )
            .handler { command, args ->
                val y =
                    (args[0] as String).toDoubleOrNull() ?: throw CommandException(command.result("invalidDistance"))

                repeat((floor(abs(y) / 10) - 1).toInt()) {
                    network.sendPacket(MovePacketType.POSITION_AND_ON_GROUND.generatePacket())
                }

                network.sendPacket(MovePacketType.POSITION_AND_ON_GROUND.generatePacket().apply { this.y += y })
                player.updatePosition(player.x, player.y + y, player.z)
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
