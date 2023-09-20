/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2023 CCBlueX
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
import net.ccbluex.liquidbounce.utils.item.isNothing
import net.minecraft.enchantment.Enchantment
import net.minecraft.enchantment.EnchantmentHelper
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NbtElement
import net.minecraft.nbt.NbtList
import net.minecraft.network.packet.c2s.play.CreativeInventoryActionC2SPacket
import net.minecraft.registry.Registries
import net.minecraft.util.Hand
import net.minecraft.util.Identifier
import kotlin.math.absoluteValue

object CommandItemEnchant {

    fun createCommand(): Command {
        return CommandBuilder.begin("enchant").parameter(
            ParameterBuilder.begin<String>("enchantment").verifiedBy(ParameterBuilder.STRING_VALIDATOR)
                .autocompletedWith {
                    Registries.ENCHANTMENT.map {
                        it.translationKey.removePrefix("enchantment.").replace('.', ':')
                    }
                }.required().build()
        ).parameter(
            ParameterBuilder.begin<Int>("level").verifiedBy(ParameterBuilder.POSITIVE_INTEGER_VALIDATOR).required()
                .build()
        ).handler { command, args ->
            val enchantmentName = args[0] as String
            val level = args[1] as Int

            if (mc.interactionManager?.hasCreativeInventory() == false) {
                throw CommandException(command.result("mustBeCreative"))
            }

            val itemStack = mc.player?.getStackInHand(Hand.MAIN_HAND)
            if (itemStack.isNothing()) {
                throw CommandException(command.result("mustHoldItem"))
            }

            val identifier = Identifier.tryParse(enchantmentName)
            val enchantment = Registries.ENCHANTMENT.getOrEmpty(identifier).orElseThrow {
                throw CommandException(command.result("enchantmentNotExists", enchantmentName))
            }

            if (level <= 255) {
                enchant(itemStack!!, enchantment, level)
            } else {
                var next = level
                while (true) {
                    next -= 255
                    if (next <= 0) {
                        next = 255 - next.absoluteValue
                        enchant(itemStack!!, enchantment, next)
                        break
                    }
                    enchant(itemStack!!, enchantment, 255)
                }
            }

            mc.networkHandler!!.sendPacket(
                CreativeInventoryActionC2SPacket(
                    36 + mc.player!!.inventory.selectedSlot, itemStack
                )
            )
            chat(regular(command.result("enchantedItem", identifier.toString(), level)))
        }.build()
    }

    private fun enchant(item: ItemStack, enchantment: Enchantment, level: Int) {
        val nbt = item.orCreateNbt
        if (nbt?.contains(ItemStack.ENCHANTMENTS_KEY, NbtElement.LIST_TYPE.toInt()) == false) {
            nbt.put(ItemStack.ENCHANTMENTS_KEY, NbtList())
        }
        val nbtList = nbt?.getList(ItemStack.ENCHANTMENTS_KEY, NbtElement.COMPOUND_TYPE.toInt())
        nbtList?.add(EnchantmentHelper.createNbt(EnchantmentHelper.getEnchantmentId(enchantment), level))
    }
}
