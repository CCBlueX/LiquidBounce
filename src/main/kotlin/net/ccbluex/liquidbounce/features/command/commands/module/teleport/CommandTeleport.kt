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
package net.ccbluex.liquidbounce.features.command.commands.module.teleport

import com.mojang.brigadier.CommandDispatcher
import net.ccbluex.liquidbounce.features.command.CommandRegistrar
import net.ccbluex.liquidbounce.features.command.arguments.BooleanArgumentType
import net.ccbluex.liquidbounce.features.command.arguments.Vec3ArgumentType
import net.ccbluex.liquidbounce.features.command.brigadier.ClientCommandSource
import net.ccbluex.liquidbounce.features.command.brigadier.get
import net.ccbluex.liquidbounce.features.command.brigadier.register
import net.ccbluex.liquidbounce.features.module.modules.movement.ModuleTeleport

/**
 * Teleport Command
 *
 * Allows you to teleport.
 *
 * Accepts vanilla coordinate semantics: absolute (`100 64 -200`), relative to the
 * player (`~ ~ ~`, `~5 ~-2 ~`) and local to the look direction (`^ ^ ^5`). An optional
 * trailing boolean (`true`/`yes`/`on`) replaces Y with the module's HighTP amount.
 *
 * Module: [ModuleTeleport]
 */
object CommandTeleport : CommandRegistrar {

    override fun register(dispatcher: CommandDispatcher<ClientCommandSource>) {
        dispatcher.register("teleport", aliases = listOf("tp")) {
            requires { it.isIngame }
            argument("pos", Vec3ArgumentType(centerCorrect = false)) { pos ->
                optional("highTp", BooleanArgumentType("highTp"), default = false) { highTp ->
                    exec { ctx ->
                        val position = Vec3ArgumentType.getPosition(ctx, pos.name)
                        val y = if (ctx.get(highTp)) {
                            ModuleTeleport.highTpAmount.toDouble()
                        } else {
                            position.y
                        }

                        ModuleTeleport.indicateTeleport(position.x, y, position.z)
                        1
                    }
                }
            }
        }
    }

}
