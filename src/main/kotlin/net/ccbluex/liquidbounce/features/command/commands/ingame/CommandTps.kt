/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2025 CCBlueX
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
package net.ccbluex.liquidbounce.features.command.commands.ingame

import net.ccbluex.liquidbounce.features.command.Command
import net.ccbluex.liquidbounce.features.command.CommandFactory
import net.ccbluex.liquidbounce.features.command.builder.CommandBuilder
import net.ccbluex.liquidbounce.utils.client.*
import net.minecraft.network.packet.s2c.play.WorldTimeUpdateS2CPacket

/**
 * TPS (ticks per second) Command
 *
 * Allows you to see the current TPS.
 *
 * This will not work on all servers as some servers modify the [WorldTimeUpdateS2CPacket] behavior.
 *
 * @author ccetl
 */
object CommandTps : CommandFactory {

    override fun createCommand(): Command {
        return CommandBuilder
            .begin("tps")
            .requiresIngame()
            .handler { command, _ ->
                val tps = ServerObserver.tps
                chat(regular(command.result("tpsCheck", variable(
                    if (tps.isNaN()) command.result("nan").string else tps.roundToDecimalPlaces(2).toString()
                ))), command)
            }
            .build()
    }

}

