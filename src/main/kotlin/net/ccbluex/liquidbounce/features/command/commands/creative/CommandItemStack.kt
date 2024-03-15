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
package net.ccbluex.liquidbounce.features.command.commands.creative

import net.ccbluex.liquidbounce.features.command.Command
import net.ccbluex.liquidbounce.features.command.CommandException
import net.ccbluex.liquidbounce.features.command.builder.CommandBuilder
import net.ccbluex.liquidbounce.features.command.builder.ParameterBuilder
import net.ccbluex.liquidbounce.features.module.QuickImports
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.regular
import net.ccbluex.liquidbounce.utils.client.variable
import net.minecraft.network.packet.c2s.play.CreativeInventoryActionC2SPacket

object CommandItemStack : QuickImports {

    private val amountParameter = ParameterBuilder
        .begin<Int>("amount")
        .verifiedBy(ParameterBuilder.INTEGER_VALIDATOR)
        .autocompletedWith { begin ->
            mutableListOf("16", "32", "64").filter { it.startsWith(begin) }
        }
        .optional()
        .build()


    @Suppress("detekt:ThrowsCount")
    fun createCommand(): Command {
        return CommandBuilder
            .begin("stack")
            .parameter(amountParameter)
            .handler { command, args ->
                if (mc.interactionManager?.hasCreativeInventory() == false) {
                    throw CommandException(command.result("mustBeCreative"))
                }

                val mainHandStack = player.mainHandStack
                if (mainHandStack.isEmpty) {
                    throw CommandException(command.result("noItem"))
                }

                val amount = args[0] as? Int ?: 64

                if (amount < 1 || amount > 64) {
                    throw CommandException(command.result("invalidAmount"))
                }


                if (mainHandStack.count == amount) {
                    chat(regular(command.result("hasAlreadyAmount", variable(amount.toString()))))
                    return@handler
                }

                mainHandStack.count = amount
                player.inventory!!.setStack(player.inventory.selectedSlot, mainHandStack)
                mc.networkHandler!!.sendPacket(
                    CreativeInventoryActionC2SPacket(
                        36 + player.inventory.selectedSlot,
                        mainHandStack
                    )
                )
                chat(regular(command.result("amountChanged", variable(amount.toString()))))
            }
            .build()
    }

}
