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
package net.ccbluex.liquidbounce.features.command.commands.ingame.creative

import net.ccbluex.liquidbounce.features.command.Command
import net.ccbluex.liquidbounce.features.command.CommandException
import net.ccbluex.liquidbounce.features.command.builder.CommandBuilder
import net.ccbluex.liquidbounce.features.command.builder.ParameterBuilder
import net.ccbluex.liquidbounce.features.command.builder.item
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.player
import net.ccbluex.liquidbounce.utils.client.regular
import net.ccbluex.liquidbounce.utils.client.variable
import net.ccbluex.liquidbounce.utils.client.world
import net.ccbluex.liquidbounce.utils.item.createItem
import net.ccbluex.liquidbounce.utils.item.setInventoryItemCreative
import net.minecraft.client.player.LocalPlayer
import net.minecraft.world.item.ItemStack
import kotlin.math.min

/**
 * ItemGive Command
 *
 * Allows you to give items to the player.
 */
object CommandItemGive : Command.Factory {

    override fun createCommand(): Command {
        return CommandBuilder
            .begin("give")
            .requiresIngame()
            .parameter(ParameterBuilder.item().required().build())
            .parameter(
                ParameterBuilder
                    .begin<Int>("amount")
                    .verifiedBy(ParameterBuilder.POSITIVE_INTEGER_VALIDATOR)
                    .optional()
                    .build()
            )
            .handler {
                if (!player.hasInfiniteMaterials()) {
                    throw CommandException(command.result("mustBeCreative"))
                }

                val item = args[0] as String
                val amount = args.getOrElse(1, defaultValue = { 1 }) as Int // default one

                val itemStack = world.createItem(item)
                val giveAmount = player.giveItem(itemStack, amount)
                if (giveAmount == 0) throw CommandException(command.result("noEmptySlot"))

                chat(
                    regular(
                        command.result(
                            "itemGiven",
                            itemStack.displayName,
                            variable(giveAmount.toString())
                        )
                    )
                )
            }
            .build()
    }

    fun LocalPlayer.giveItem(item: ItemStack, amount: Int): Int {
        var remaining = amount

        while (remaining > 0) {
            val slot = inventory.getSlotWithRemainingSpace(item).takeUnless { it == -1 }
                ?: inventory.freeSlot.takeUnless { it == -1 }
                ?: break

            val selectItemStack = inventory.getItem(slot)
                .takeUnless { it.isEmpty }
                ?: item.copyWithCount(0).also { inventory.setItem(slot, it) }

            val maxToAdd = inventory.getMaxStackSize(selectItemStack) - selectItemStack.count
            val toAdd = min(maxToAdd, remaining)

            if (toAdd > 0) {
                remaining -= toAdd
                selectItemStack.grow(toAdd)
                selectItemStack.popTime = 5
            }

            setInventoryItemCreative(slot, selectItemStack)
        }

        return amount - remaining
    }

}
