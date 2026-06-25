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
package net.ccbluex.liquidbounce.features.command.commands.module.teleport

import net.ccbluex.liquidbounce.features.command.Command
import net.ccbluex.liquidbounce.features.command.builder.CommandBuilder
import net.ccbluex.liquidbounce.features.command.builder.ParameterBuilder
import net.ccbluex.liquidbounce.features.module.modules.movement.ModuleTeleport
import net.ccbluex.liquidbounce.utils.client.player

/**
 * Teleport Command
 *
 * Allows you to teleport.
 *
 * Module: [ModuleTeleport]
 */
object CommandTeleport : Command.Factory {

    override fun createCommand(): Command {
        return CommandBuilder
            .begin("teleport")
            .alias("tp")
            .requiresIngame()
            .parameter(
                ParameterBuilder
                    .begin<Float>("x")
                    .required()
                    .verifiedBy(ParameterBuilder.FLOAT_VALIDATOR)
                    .build(),
            )
            .parameter(
                ParameterBuilder
                    .begin<Float>("y|z")
                    .required()
                    .verifiedBy(ParameterBuilder.FLOAT_VALIDATOR)
                    .build()
            )
            .parameter(
                ParameterBuilder
                    .begin<Float>("z")
                    .optional()
                    .verifiedBy(ParameterBuilder.FLOAT_VALIDATOR)
                    .build()
            )
            .handler {
                val x = args[0] as Float
                val yOrZ = args[1] as Float
                val z = if (args.size > 2) args[2] as Float else yOrZ
                val y = if (args.size > 2) {
                    yOrZ.toDouble()
                } else if (ModuleTeleport.highTp) {
                    ModuleTeleport.highTpAmount.toDouble()
                } else {
                    player.y
                }

                ModuleTeleport.indicateTeleport(x.toDouble(), y, z.toDouble())
            }
            .build()
    }
}
