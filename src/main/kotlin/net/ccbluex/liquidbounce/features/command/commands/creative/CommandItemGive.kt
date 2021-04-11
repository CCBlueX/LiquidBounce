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

object CommandItemGive {

    fun createCommand(): Command {
        return CommandBuilder
            .begin("give")
            .description("Allows you to give yourself items")
            .parameter(
                ParameterBuilder
                    .begin<String>("item")
                    .description("Item")
                    .verifiedBy(ParameterBuilder.STRING_VALIDATOR)
                    .required()
                    .build()
            )
            .parameter(
                ParameterBuilder
                    .begin<Int>("amount")
                    .description("Item amount")
                    .verifiedBy(ParameterBuilder.INTEGER_VALIDATOR)
                    .optional()
                    .build()
            )
            .handler { command, args ->
                val item = args[0] as String
                val amount = if (args.size > 2) args[1] as Int else 1 // default one

                if (mc.interactionManager?.hasCreativeInventory() == false) {
                    throw CommandException("You need to be in creative mode.")
                }

                val itemStack = createItem(item, amount)
                val emptySlot = mc.player!!.inventory!!.emptySlot

                if (emptySlot == -1) {
                    throw CommandException("There are no empty slots in your inventory.")
                }

                mc.networkHandler!!.sendPacket(CreativeInventoryActionC2SPacket(if(emptySlot < 9) emptySlot + 36 else emptySlot, itemStack))
                chat(regular("Given "), itemStack.toHoverableText().copy(), regular(" * "),
                    variable(itemStack.count.toString()), dot())
            }
            .build()
    }

}
