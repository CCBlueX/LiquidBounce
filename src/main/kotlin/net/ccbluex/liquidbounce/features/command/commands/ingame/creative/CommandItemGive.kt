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
package net.ccbluex.liquidbounce.features.command.commands.ingame.creative

import net.ccbluex.liquidbounce.features.command.Command
import net.ccbluex.liquidbounce.features.command.CommandException
import net.ccbluex.liquidbounce.features.command.CommandFactory
import net.ccbluex.liquidbounce.features.command.builder.CommandBuilder
import net.ccbluex.liquidbounce.features.command.builder.ParameterBuilder
import net.ccbluex.liquidbounce.features.command.builder.Parameters
import net.ccbluex.liquidbounce.utils.client.*
import net.ccbluex.liquidbounce.utils.item.createItem
import net.minecraft.network.packet.c2s.play.CreativeInventoryActionC2SPacket

/**
 * ItemGive Command
 *
 * Allows you to give items to the player.
 */
object CommandItemGive : CommandFactory {

    override fun createCommand(): Command {
        return CommandBuilder
            .begin("give")
            .requiresIngame()
            .parameter(Parameters.item().required().build())
            .parameter(
                ParameterBuilder
                    .begin<Int>("amount")
                    .verifiedBy(ParameterBuilder.POSITIVE_INTEGER_VALIDATOR)
                    .optional()
                    .build()
            )
            .handler { command, args ->
                if (!interaction.hasCreativeInventory()) {
                    throw CommandException(command.result("mustBeCreative"))
                }

                val item = args[0] as String
                val amount = args.getOrElse(1, defaultValue = { 1 }) as Int // default one

                val itemStack = createItem(item, amount.coerceIn(1..64))
                val emptySlot = player.inventory.emptySlot

                if (emptySlot == -1) {
                    throw CommandException(command.result("noEmptySlot"))
                }

                player.inventory.setStack(emptySlot, itemStack)
                network.sendPacket(CreativeInventoryActionC2SPacket(if (emptySlot < 9) emptySlot + 36 else emptySlot,
                    itemStack))
                chat(
                    regular(command.result("itemGiven", itemStack.toHoverableText(),
                        variable(itemStack.count.toString()))),
                    command
                )
            }
            .build()
    }

}
