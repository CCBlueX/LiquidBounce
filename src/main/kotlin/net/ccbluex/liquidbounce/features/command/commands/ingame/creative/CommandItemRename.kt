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
import com.mojang.brigadier.arguments.StringArgumentType
import net.ccbluex.liquidbounce.features.command.CommandException
import net.ccbluex.liquidbounce.features.command.CommandRegistrar
import net.ccbluex.liquidbounce.features.command.brigadier.ClientCommandSource
import net.ccbluex.liquidbounce.features.command.brigadier.CmdI18n
import net.ccbluex.liquidbounce.features.command.brigadier.get
import net.ccbluex.liquidbounce.features.command.brigadier.register
import net.ccbluex.liquidbounce.utils.client.MessageMetadata
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.player
import net.ccbluex.liquidbounce.utils.client.regular
import net.ccbluex.liquidbounce.utils.client.variable
import net.ccbluex.liquidbounce.utils.item.setInventoryItemCreative
import net.ccbluex.liquidbounce.utils.text.asPlainText
import net.ccbluex.liquidbounce.utils.text.translateColorCodes
import net.minecraft.core.component.DataComponents
import net.minecraft.world.InteractionHand

/**
 * ItemRename Command
 *
 * Allows you to rename an item held in the player's hand.
 */
object CommandItemRename : CommandRegistrar {
    override fun register(dispatcher: CommandDispatcher<ClientCommandSource>) {
        dispatcher.register("rename") {
            requires { it.isIngame }
            exec {
                // Omitting the name resets the custom name.
                rename("")
            }
            argument("name", StringArgumentType.greedyString()) { name ->
                exec { ctx ->
                    rename(ctx.get(name))
                }
            }
        }
    }

    private fun CmdI18n.rename(name: String): Int {
        if (!player.hasInfiniteMaterials()) {
            throw CommandException(t("mustBeCreative"))
        }

        val itemStack = player.getItemInHand(InteractionHand.MAIN_HAND)
        if (itemStack.isEmpty) {
            throw CommandException(t("mustHoldItem"))
        }

        when (name) {
            "" -> {
                itemStack.remove(DataComponents.CUSTOM_NAME)
                chat(
                    regular(t("nameReset")),
                    metadata = MessageMetadata(id = "Crename#info")
                )
            }
            else -> {
                itemStack.set(DataComponents.CUSTOM_NAME, name.translateColorCodes().asPlainText())
                chat(
                    regular(
                        t("renamedItem",
                            itemStack.itemName,
                            variable(name)
                        )
                    ),
                    metadata = MessageMetadata(id = "Crename#info")
                )
            }
        }
        player.setInventoryItemCreative(itemStack = itemStack, animation = false)

        return 1
    }

}
