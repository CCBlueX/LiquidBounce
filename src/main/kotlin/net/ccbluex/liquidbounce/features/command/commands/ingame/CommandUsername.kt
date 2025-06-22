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
package net.ccbluex.liquidbounce.features.command.commands.ingame

import net.ccbluex.liquidbounce.features.command.Command
import net.ccbluex.liquidbounce.features.command.CommandFactory
import net.ccbluex.liquidbounce.features.command.builder.CommandBuilder
import net.ccbluex.liquidbounce.features.module.MinecraftShortcuts
import net.ccbluex.liquidbounce.utils.client.*
import org.lwjgl.glfw.GLFW

/**
 * CommandUsername
 *
 * Displays the current username.
 */
object CommandUsername : CommandFactory, MinecraftShortcuts {

    override fun createCommand(): Command {
        return CommandBuilder
            .begin("username")
            .requiresIngame()
            .handler { command, _ ->
                val username = player.name.string
                val formattedUsernameWithEvents = variable(username)
                    .bypassNameProtection()
                    .copyable(copyContent = username)
                    .italic(true)
                    .underline(true)

                chat(regular(command.result("username", formattedUsernameWithEvents)), command)
                GLFW.glfwSetClipboardString(mc.window.handle, username)
            }
            .build()
    }

}
