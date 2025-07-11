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
import net.ccbluex.liquidbounce.features.module.MinecraftShortcuts
import net.ccbluex.liquidbounce.utils.client.MessageMetadata
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.regular
import net.ccbluex.liquidbounce.utils.item.addEnchantment
import net.ccbluex.liquidbounce.utils.item.clearEnchantments
import net.ccbluex.liquidbounce.utils.item.isNothing
import net.ccbluex.liquidbounce.utils.item.removeEnchantment
import net.minecraft.enchantment.Enchantment
import net.minecraft.item.ItemStack
import net.minecraft.network.packet.c2s.play.CreativeInventoryActionC2SPacket
import net.minecraft.registry.RegistryKeys
import net.minecraft.registry.entry.RegistryEntry
import net.minecraft.util.Hand
import net.minecraft.util.Identifier
import kotlin.math.min

/**
 * ItemEnchant Command
 *
 * Allows you to add, remove, clear, and enchant all possible enchantments on an item.
 */
object CommandItemEnchant : CommandFactory, MinecraftShortcuts {

    private val levelParameter = ParameterBuilder
        .begin<String>("level")
        .verifiedBy(ParameterBuilder.STRING_VALIDATOR)
        .autocompletedWith { begin, _ ->
            mutableListOf("max", "1", "2", "3", "4", "5").filter { it.startsWith(begin) }
        }
        .required()

    @Suppress("LongMethod")
    override fun createCommand(): Command {
        return CommandBuilder
            .begin("enchant")
            .requiresIngame()
            .hub()
            .subcommand(
                CommandBuilder
                    .begin("add")
                    .parameter(Parameters.enchantment().required().build())
                    .parameter(levelParameter.build())
                    .handler { command, args ->
                        val enchantmentName = args[0] as String
                        val level = getLevel(args[1] as String)

                        creativeOrThrow(command)
                        val itemStack = getItemOrThrow(command)

                        val enchantment = enchantmentByName(enchantmentName, command)
                        enchantAnyLevel(itemStack, enchantment, level)

                        sendItemPacket(itemStack)
                        chat(
                            regular(command.resultWithTree("enchantedItem", enchantment.idAsString, level ?: "max")),
                            metadata = MessageMetadata(id = "CItemEnchant#info")
                        )
                    }
                    .build()
            )
            .subcommand(
                CommandBuilder
                    .begin("remove")
                    .parameter(Parameters.enchantment().required().build())
                    .handler { command, args ->
                        val enchantmentName = args[0] as String

                        creativeOrThrow(command)
                        val itemStack = getItemOrThrow(command)

                        val enchantment = enchantmentByName(enchantmentName, command)
                        removeEnchantment(itemStack, enchantment)

                        sendItemPacket(itemStack)
                        chat(
                            regular(command.resultWithTree("unenchantedItem", enchantment.idAsString)),
                            metadata = MessageMetadata(id = "CItemEnchant#info")
                        )
                    }
                    .build()

            )
            .subcommand(
                CommandBuilder
                    .begin("clear")
                    .handler { command, _ ->
                        creativeOrThrow(command)
                        val itemStack = getItemOrThrow(command)

                        clearEnchantments(itemStack)

                        sendItemPacket(itemStack)
                    }
                    .build()
            )
            .subcommand(
                CommandBuilder
                    .begin("all")
                    .parameter(levelParameter.build())
                    .handler { command, args ->
                        creativeOrThrow(command)
                        val itemStack = getItemOrThrow(command)

                        val level = getLevel(args[0] as String)

                        enchantAll(itemStack, false, level)

                        sendItemPacket(itemStack)
                        chat(
                            regular(command.resultWithTree("enchantedItem", "all", level ?: "Max")),
                            metadata = MessageMetadata(id = "CItemEnchant#info")
                        )
                    }
                    .build()
            )
            .subcommand(
                CommandBuilder
                    .begin("all_possible")
                    .parameter(levelParameter.build())
                    .handler { command, args ->
                        creativeOrThrow(command)
                        val itemStack = getItemOrThrow(command)

                        val level = getLevel(args[0] as String)
                        enchantAll(itemStack, true, level)

                        sendItemPacket(itemStack)
                        chat(
                            regular(command.resultWithTree("enchantedItem", "all_possible", level ?: "Max")),
                            metadata = MessageMetadata(id = "CItemEnchant#info")
                        )
                    }
                    .build()
            )


            .build()
    }

    private fun getLevel(arg: String) =
        if (arg == "max") {
            null
        } else {
            arg.toInt()
        }


    private fun sendItemPacket(itemStack: ItemStack?) {
        network.sendPacket(
            CreativeInventoryActionC2SPacket(
                36 + player.inventory.selectedSlot, itemStack
            )
        )
    }

    private fun creativeOrThrow(command: Command) {
        if (mc.interactionManager?.hasCreativeInventory() == false) {
            throw CommandException(command.resultWithTree("mustBeCreative"))
        }
    }

    private fun getItemOrThrow(command: Command): ItemStack {
        val itemStack = player.getStackInHand(Hand.MAIN_HAND)

        if (itemStack.isNothing()) {
            throw CommandException(command.resultWithTree("mustHoldItem"))
        }

        return itemStack!!
    }

    private fun enchantmentByName(enchantmentName: String, command: Command): RegistryEntry<Enchantment> {
        val identifier = Identifier.tryParse(enchantmentName)
        val registry = world.registryManager.getOrThrow(RegistryKeys.ENCHANTMENT)
        val enchantment = registry.getEntry(identifier).orElseThrow {
            CommandException(command.resultWithTree("enchantmentNotExists", enchantmentName))
        }

        return enchantment
    }

    private fun enchantAnyLevel(item: ItemStack, enchantment: RegistryEntry<Enchantment>, level: Int?) {
        if (level == null || level <= 255) {
            addEnchantment(item, enchantment, level ?: enchantment.value().maxLevel)
        } else {
            var next = level

            while (next > 255) {
                addEnchantment(item, enchantment, min(next, 255))
                next -= 255
            }
        }
    }

    private fun enchantAll(item: ItemStack, onlyAcceptable: Boolean, level: Int?) {
        world.registryManager.getOrThrow(RegistryKeys.ENCHANTMENT).indexedEntries.forEach { enchantment ->
            if (!enchantment.value().isAcceptableItem(item) && onlyAcceptable) {
                return@forEach
            }

            enchantAnyLevel(item, enchantment, level)
        }
    }

}
