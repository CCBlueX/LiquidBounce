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
import net.minecraft.item.Items
import net.minecraft.nbt.NbtCompound
import net.minecraft.nbt.NbtElement
import net.minecraft.nbt.NbtList
import net.minecraft.network.packet.c2s.play.CreativeInventoryActionC2SPacket
import net.minecraft.registry.Registries
import net.minecraft.util.Hand
import net.minecraft.util.Identifier
import kotlin.math.absoluteValue

object CommandItemEnchant {


    val levelParameter= ParameterBuilder
        .begin<String>("level")
        .verifiedBy(ParameterBuilder.STRING_VALIDATOR)
        .autocompletedWith {
            mutableListOf("max", "1", "2", "3", "4", "5")
        }
        .required()

    val enchantmentParameter =
        ParameterBuilder.begin<String>("enchantment").verifiedBy(ParameterBuilder.STRING_VALIDATOR)
        .autocompletedWith {
            Registries.ENCHANTMENT.map {
                it.translationKey.removePrefix("enchantment.").replace('.', ':')
            }
        }.required()

    fun createCommand(): Command {
        return CommandBuilder
            .begin("enchant")
            .hub()
            .subcommand(
                CommandBuilder
                    .begin("add")
                    .parameter(enchantmentParameter.build())
                    .parameter(levelParameter.build())
                    .handler { command, args ->
                        val enchantmentName = args[0] as String
                        val level = getLevel(args[1] as String)

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

                        if (level == null || level <= 255) {
                            addEnchantment(itemStack!!, enchantment, level)
                        } else {
                            var next = level!!
                            while (true) {
                                next -= 255
                                if (next <= 0) {
                                    next = 255 - next.absoluteValue
                                    addEnchantment(itemStack!!, enchantment, next)
                                    break
                                }
                                addEnchantment(itemStack!!, enchantment, 255)
                            }
                        }

                        sendItemPacket(itemStack)
                        chat(regular(command.result("enchantedItem", identifier.toString(), level ?: "max")))
                    }
                    .build()
            )
            .subcommand(
                CommandBuilder
                    .begin("remove")
                    .parameter(enchantmentParameter.build())
                    .handler {command, args ->
                        val enchantmentName = args[0] as String

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
                        removeEnchantment(itemStack!!, enchantment)

                        sendItemPacket(itemStack)
                        chat(regular(command.result("unenchantedItem", identifier.toString())))
                    }
                    .build()

            )
            .subcommand(
                CommandBuilder
                    .begin("clear")
                    .handler {command, _ ->
                        if (mc.interactionManager?.hasCreativeInventory() == false) {
                            throw CommandException(command.result("mustBeCreative"))
                        }

                        val itemStack = mc.player?.getStackInHand(Hand.MAIN_HAND)
                        if (itemStack.isNothing()) {
                            throw CommandException(command.result("mustHoldItem"))
                        }

                        clearEnchantments(itemStack!!)

                        sendItemPacket(itemStack)
                    }
                    .build()
            )
            .subcommand(
                CommandBuilder
                    .begin("all")
                    .parameter(levelParameter.build())
                    .handler { command, args ->


                        if (mc.interactionManager?.hasCreativeInventory() == false) {
                            throw CommandException(command.result("mustBeCreative"))
                        }

                        val itemStack = mc.player?.getStackInHand(Hand.MAIN_HAND)
                        if (itemStack.isNothing()) {
                            throw CommandException(command.result("mustHoldItem"))
                        }

                        val level = getLevel(args[0] as String)

                        enchantAll(itemStack!!, false, level)

                        sendItemPacket(itemStack)
                        chat(regular(command.result("enchantedItem", level ?: "Max")))
                    }
                    .build()
            )
            .subcommand(
                CommandBuilder
                    .begin("all_possible")
                    .parameter(levelParameter.build())
                    .handler { command, args ->
                        if (mc.interactionManager?.hasCreativeInventory() == false) {
                            throw CommandException(command.result("mustBeCreative"))
                        }

                        val itemStack = mc.player?.getStackInHand(Hand.MAIN_HAND)
                        if (itemStack.isNothing()) {
                            throw CommandException(command.result("mustHoldItem"))
                        }

                        val level = getLevel(args[0] as String)


                        enchantAll(itemStack!!, true, level)


                        sendItemPacket(itemStack)
                        chat(regular(command.result("enchantedItem", level ?: "Max")))
                    }
                    .build()
            )


            .build()
    }

    private fun getLevel(arg: String) =
        if(arg == "max")
            null
        else
            arg.toInt()


    private fun sendItemPacket(itemStack: ItemStack?) {
        mc.networkHandler!!.sendPacket(
            CreativeInventoryActionC2SPacket(
                36 + mc.player!!.inventory.selectedSlot, itemStack
            )
        )
    }

    private fun enchantAll(item: ItemStack, onlyAcceptable: Boolean, level: Int?) {
        Registries.ENCHANTMENT.forEach {enchantment ->
            if(!enchantment.isAcceptableItem(item) && onlyAcceptable) return@forEach

            if (level == null || level <= 255) {
                addEnchantment(item, enchantment, level)
            } else {
                var next = level!!
                while (true) {
                    next -= 255
                    if (next <= 0) {
                        next = 255 - next.absoluteValue
                        addEnchantment(item, enchantment, next)
                        break
                    }
                    addEnchantment(item, enchantment, 255)
                }
            }
        }
    }

//    private fun getNbtlEnchantmentslist(item: ItemStack) =



    private fun addEnchantment(item: ItemStack, enchantment: Enchantment, level: Int?) {
        val nbt = item.orCreateNbt
        if (nbt?.contains(ItemStack.ENCHANTMENTS_KEY, NbtElement.LIST_TYPE.toInt()) == false) {
            nbt.put(ItemStack.ENCHANTMENTS_KEY, NbtList())
        }
        val nbtList = nbt?.getList(ItemStack.ENCHANTMENTS_KEY, NbtElement.COMPOUND_TYPE.toInt())
        nbtList?.add(EnchantmentHelper.createNbt(EnchantmentHelper.getEnchantmentId(enchantment), level ?: enchantment.maxLevel))
    }

    private fun removeEnchantment(item: ItemStack, enchantment: Enchantment){
        val nbt = item.nbt ?: return
        if (!nbt.contains(ItemStack.ENCHANTMENTS_KEY, NbtElement.LIST_TYPE.toInt())) {
            return
        }
        val nbtList = nbt.getList(ItemStack.ENCHANTMENTS_KEY, NbtElement.COMPOUND_TYPE.toInt())
        nbtList.removeIf { (it as NbtCompound).getString("id") == EnchantmentHelper.getEnchantmentId(enchantment).toString() }
    }

    private fun clearEnchantments(item: ItemStack) {
        val nbt = item.nbt ?: return
        nbt.remove(ItemStack.ENCHANTMENTS_KEY)
    }
}
