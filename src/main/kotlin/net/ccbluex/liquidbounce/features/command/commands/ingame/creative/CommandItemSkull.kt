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
import net.ccbluex.liquidbounce.features.module.MinecraftShortcuts
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.regular
import net.ccbluex.liquidbounce.utils.client.variable
import net.ccbluex.liquidbounce.utils.item.setInventoryItemCreative
import net.minecraft.core.component.DataComponentPatch
import net.minecraft.core.component.DataComponents
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.component.ResolvableProfile
import java.util.UUID

/**
 * CommandItemSkull
 *
 * Allows you to create a player skull item with a specified name.
 */
object CommandItemSkull : Command.Factory, MinecraftShortcuts {

    override fun createCommand(): Command {
        return CommandBuilder
            .begin("skull")
            .requiresIngame()
            .parameter(
                ParameterBuilder
                    .begin<String>("name")
                    .verifiedBy(ParameterBuilder.STRING_VALIDATOR)
                    .required()
                    .build()
            )
            .handler {
                if (!player.hasInfiniteMaterials()) {
                    throw CommandException(command.result("mustBeCreative"))
                }

                val name = args[0] as String

                val itemStack = ItemStack(Items.PLAYER_HEAD)
                    .apply {
                        val profile = runCatching { UUID.fromString(name) }
                            .fold(
                                onSuccess = { ResolvableProfile.createUnresolved(it) },
                                onFailure = { ResolvableProfile.createUnresolved(name) }
                            )
                        DataComponentPatch.builder()
                            .set(DataComponents.PROFILE, profile)
                            .build()
                            .also { applyComponents(it) }
                    }

                val emptySlot = player.inventory.freeSlot
                if (emptySlot == -1) {
                    throw CommandException(command.result("noEmptySlot"))
                }

                player.setInventoryItemCreative(emptySlot, itemStack)
                chat(regular(command.result("skullGiven", variable(name))), command)
            }
            .build()
    }

}
