/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2016 - 2021 CCBlueX
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
package net.ccbluex.liquidbounce.features.command.commands.creative

import net.ccbluex.liquidbounce.features.command.Command
import net.ccbluex.liquidbounce.features.command.CommandException
import net.ccbluex.liquidbounce.features.command.builder.CommandBuilder
import net.ccbluex.liquidbounce.features.command.builder.ParameterBuilder
import net.ccbluex.liquidbounce.utils.*
import net.ccbluex.liquidbounce.utils.extensions.createItem
import net.minecraft.network.packet.c2s.play.CreativeInventoryActionC2SPacket

object CommandItemSkull {

    fun createCommand(): Command {
        return CommandBuilder
            .begin("skull")
            .description("Allows you to give yourself player skulls")
            .parameter(
                ParameterBuilder
                    .begin<String>("name")
                    .description("Name of the player")
                    .verifiedBy(ParameterBuilder.STRING_VALIDATOR)
                    .required()
                    .build()
            )
            .handler { args ->
                val name = args[0] as String

                if (mc.interactionManager?.hasCreativeInventory() == false) {
                    throw CommandException("You need to be in creative mode.")
                }

                val itemStack = createItem("minecraft:player_head{SkullOwner:$name}", 1)
                val emptySlot = mc.player!!.inventory!!.emptySlot

                if (emptySlot == -1) {
                    throw CommandException("There are no empty slots in your inventory.")
                }

                mc.networkHandler!!.sendPacket(CreativeInventoryActionC2SPacket(if(emptySlot < 9) emptySlot + 36 else emptySlot, itemStack))
                chat(regular("Given skull of "), variable(name), dot())
            }
            .build()
    }

}
