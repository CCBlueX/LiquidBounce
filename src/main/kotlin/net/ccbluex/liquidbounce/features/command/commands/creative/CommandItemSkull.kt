/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2016 - 2022 CCBlueX
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
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.client.regular
import net.ccbluex.liquidbounce.utils.client.variable
import net.ccbluex.liquidbounce.utils.item.createItem
import net.minecraft.network.packet.c2s.play.CreativeInventoryActionC2SPacket

object CommandItemSkull {

    fun createCommand(): Command {
        return CommandBuilder
            .begin("skull")
            .parameter(
                ParameterBuilder
                    .begin<String>("name")
                    .verifiedBy(ParameterBuilder.STRING_VALIDATOR)
                    .required()
                    .build()
            )
            .handler { command, args ->
                val name = args[0] as String

                if (mc.interactionManager?.hasCreativeInventory() == false) {
                    throw CommandException(command.result("mustBeCreative"))
                }

                val itemStack = createItem("minecraft:player_head{SkullOwner:$name}")
                val emptySlot = mc.player!!.inventory!!.emptySlot

                if (emptySlot == -1) {
                    throw CommandException(command.result("noEmptySlot"))
                }

                mc.player!!.inventory!!.setStack(emptySlot, itemStack)
                mc.networkHandler!!.sendPacket(CreativeInventoryActionC2SPacket(if (emptySlot < 9) emptySlot + 36 else emptySlot, itemStack))
                chat(regular(command.result("skullGiven", variable(name))))
            }
            .build()
    }

}
