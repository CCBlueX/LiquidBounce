/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2016 - 2020 CCBlueX
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

package net.ccbluex.liquidbounce.command.commands

import net.ccbluex.liquidbounce.command.Command
import net.ccbluex.liquidbounce.command.builder.CommandBuilder
import net.ccbluex.liquidbounce.command.builder.ParameterBuilder
import net.ccbluex.liquidbounce.utils.chat
import net.ccbluex.liquidbounce.utils.mc
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket

object HurtCommand {

    @JvmStatic
    fun createCommand(): Command {
        return CommandBuilder.begin("hurt")
            .description("Does damage to the player")
            .parameter(
                ParameterBuilder.begin<Int>("damage")
                    .description("The damage that will be done. (1 damage = 1/2 heart)")
                    .verifiedBy(ParameterBuilder.POSITIVE_INTEGER_VALIDATOR)
                    .required()
                    .build()
            )
            .handler(this::onCommand)
            .build()
    }

    @JvmStatic
    private fun onCommand(it: List<Any>): Boolean {
        val damage = it[0] as Int

        // Latest NoCheatPlus damage exploit
        val thePlayer = mc.player ?: return false

        val x = thePlayer.x
        val y = thePlayer.y
        val z = thePlayer.z

        val networkHandler = mc.networkHandler!!

        for (i in 0 until 65 * damage) {
            networkHandler.sendPacket(PlayerMoveC2SPacket.PositionOnly(x, y + 0.049, z, false))
            networkHandler.sendPacket(PlayerMoveC2SPacket.PositionOnly(x, y, z, false))
        }

        networkHandler.sendPacket(PlayerMoveC2SPacket.PositionOnly(x, y, z, true))

        // Output message
        chat("You were damaged.")

        return true
    }
}