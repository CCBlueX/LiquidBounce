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
package net.ccbluex.liquidbounce.features.command.commands.ingame.teleport

import com.mojang.brigadier.CommandDispatcher
import net.ccbluex.liquidbounce.features.command.CommandRegistrar
import net.ccbluex.liquidbounce.features.command.arguments.ClientStringArgumentType
import net.ccbluex.liquidbounce.features.command.arguments.WorldPlayerArgumentType
import net.ccbluex.liquidbounce.features.command.brigadier.ClientCommandSource
import net.ccbluex.liquidbounce.features.command.brigadier.get
import net.ccbluex.liquidbounce.features.command.brigadier.register
import net.ccbluex.liquidbounce.features.module.MinecraftShortcuts
import net.ccbluex.liquidbounce.features.global.GlobalSettingsTeleport
import net.minecraft.client.player.AbstractClientPlayer
import kotlin.math.floor

/**
 * Teleport Command
 *
 * Allows you to teleport.
 *
 * Settings: [GlobalSettingsTeleport]
 */
object CommandPlayerTeleport : MinecraftShortcuts, CommandRegistrar {
    override fun register(dispatcher: CommandDispatcher<ClientCommandSource>) {
        dispatcher.register("playerteleport", aliases = listOf("playertp", "ptp")) {
            requires { it.isIngame }
            argument("player", WorldPlayerArgumentType) { player ->
                optional("copy", ClientStringArgumentType.word(), default = null) { copy ->
                    exec { ctx ->
                        teleportTo(ctx.get(player), copy = ctx.get(copy) == "copy")
                        1
                    }
                }
            }
        }
    }

    private fun teleportTo(target: AbstractClientPlayer, copy: Boolean) {
        val y = if (GlobalSettingsTeleport.highTp) {
            GlobalSettingsTeleport.highTpAmount
        } else {
            target.y
        }

        if (copy) {
            val clipboard = ".teleport ${floor(target.x).toInt()} " +
                "${floor(y.toDouble()).toInt()} ${floor(target.z).toInt()}"

            mc.keyboardHandler.clipboard = clipboard
            return
        }

        GlobalSettingsTeleport.teleport(target.x, y.toDouble(), target.z)
    }

}
