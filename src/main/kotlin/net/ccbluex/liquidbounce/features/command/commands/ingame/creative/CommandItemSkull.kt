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
import net.ccbluex.liquidbounce.features.command.CommandException
import net.ccbluex.liquidbounce.features.command.CommandRegistrar
import net.ccbluex.liquidbounce.features.command.arguments.ClientStringArgumentType
import net.ccbluex.liquidbounce.features.command.brigadier.ClientCommandSource
import net.ccbluex.liquidbounce.features.command.brigadier.get
import net.ccbluex.liquidbounce.features.command.brigadier.register
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
object CommandItemSkull : MinecraftShortcuts, CommandRegistrar {
    override fun register(dispatcher: CommandDispatcher<ClientCommandSource>) {
        dispatcher.register("skull") {
            requires { it.isIngame }
            argument("name", ClientStringArgumentType.word()) { name ->
                exec { ctx ->
                    if (!player.hasInfiniteMaterials()) {
                        throw CommandException(t("mustBeCreative"))
                    }

                    val skullName = ctx.get(name)

                    val itemStack = ItemStack(Items.PLAYER_HEAD)
                        .apply {
                            val profile = runCatching { UUID.fromString(skullName) }
                                .fold(
                                    onSuccess = { ResolvableProfile.createUnresolved(it) },
                                    onFailure = { ResolvableProfile.createUnresolved(skullName) }
                                )
                            DataComponentPatch.builder()
                                .set(DataComponents.PROFILE, profile)
                                .build()
                                .also { applyComponents(it) }
                        }

                    val emptySlot = player.inventory.freeSlot
                    if (emptySlot == -1) {
                        throw CommandException(t("noEmptySlot"))
                    }

                    player.setInventoryItemCreative(emptySlot, itemStack)
                    chat(regular(t("skullGiven", variable(skullName))))
                    1
                }
            }
        }
    }

}
