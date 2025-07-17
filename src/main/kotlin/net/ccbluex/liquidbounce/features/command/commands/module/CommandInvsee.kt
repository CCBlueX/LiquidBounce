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
package net.ccbluex.liquidbounce.features.command.commands.module

import com.mojang.blaze3d.systems.RenderSystem
import net.ccbluex.liquidbounce.features.command.Command
import net.ccbluex.liquidbounce.features.command.CommandException
import net.ccbluex.liquidbounce.features.command.CommandFactory
import net.ccbluex.liquidbounce.features.command.builder.CommandBuilder
import net.ccbluex.liquidbounce.features.command.builder.Parameters
import net.ccbluex.liquidbounce.features.module.modules.misc.ModuleInventoryTracker
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.client.network
import net.ccbluex.liquidbounce.utils.client.world
import net.ccbluex.liquidbounce.utils.inventory.ViewedInventoryScreen
import java.util.*

/**
 * Command Invsee
 *
 * ???
 *
 * Module: [ModuleInventoryTracker]
 */
object CommandInvsee : CommandFactory {

    var viewedPlayer: UUID? = null

    override fun createCommand(): Command {
        return CommandBuilder
            .begin("invsee")
            .requiresIngame()
            .parameter(
                Parameters.playerName()
                    .required()
                    .build()
            )
            .handler { command, args ->
                val inputName = args[0] as String
                val playerID = network.playerList.find { it.profile.name.equals(inputName, true) }?.profile?.id
                val player = { world.getPlayerByUuid(playerID) ?: ModuleInventoryTracker.playerMap[playerID] }

                if (playerID == null || player() == null) {
                    throw CommandException(command.result("playerNotFound", inputName))
                }

                RenderSystem.recordRenderCall {
                    mc.setScreen(ViewedInventoryScreen(player))
                }

                viewedPlayer = playerID
            }
            .build()
    }
}
