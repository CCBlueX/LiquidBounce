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

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.IntegerArgumentType
import net.ccbluex.liquidbounce.features.command.CommandException
import net.ccbluex.liquidbounce.features.command.CommandRegistrar
import net.ccbluex.liquidbounce.features.command.arguments.itemArgument
import net.ccbluex.liquidbounce.features.command.brigadier.ClientCommandSource
import net.ccbluex.liquidbounce.features.command.brigadier.CmdI18n
import net.ccbluex.liquidbounce.features.command.brigadier.get
import net.ccbluex.liquidbounce.features.command.brigadier.register
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.player
import net.ccbluex.liquidbounce.utils.client.regular
import net.ccbluex.liquidbounce.utils.client.variable
import net.ccbluex.liquidbounce.utils.item.setInventoryItemCreative
import net.minecraft.client.player.LocalPlayer
import net.minecraft.commands.arguments.item.ItemArgument
import net.minecraft.world.item.ItemStack
import kotlin.math.min

/**
 * ItemGive Command
 *
 * Allows you to give items to the player.
 */
object CommandItemGive : CommandRegistrar {
    override fun register(dispatcher: CommandDispatcher<ClientCommandSource>) {
        dispatcher.register("give") {
            requires { it.isIngame }
            argument("item", itemArgument()) { item ->
                optional("amount", IntegerArgumentType.integer(1), default = null) { amount ->
                    exec { ctx ->
                        giveItem(
                            ItemArgument.getItem(ctx, item.name)
                                .createItemStack(ctx.get(amount) ?: 1),
                        )
                    }
                }
            }
        }
    }

    private fun CmdI18n.giveItem(itemStack: ItemStack): Int {
        if (!player.hasInfiniteMaterials()) {
            throw CommandException(t("mustBeCreative"))
        }

        val giveAmount = player.giveItem(itemStack, itemStack.count)
        if (giveAmount == 0) throw CommandException(t("noEmptySlot"))

        chat(
            regular(
                t("itemGiven",
                    itemStack.displayName,
                    variable(giveAmount.toString())
                )
            )
        )
        return 1
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
