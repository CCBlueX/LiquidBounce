/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2024 CCBlueX
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
package net.ccbluex.liquidbounce.features.command.commands.utility

import net.ccbluex.liquidbounce.features.command.Command
import net.ccbluex.liquidbounce.features.command.builder.CommandBuilder
import net.ccbluex.liquidbounce.features.module.QuickImports
import net.ccbluex.liquidbounce.register.IncludeCommand
import net.ccbluex.liquidbounce.utils.client.bypassNameProtection
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.regular
import net.ccbluex.liquidbounce.utils.client.variable
import net.minecraft.text.ClickEvent
import net.minecraft.text.HoverEvent
import org.lwjgl.glfw.GLFW

/**
 * CommandUsername
 *
 * Displays the current username.
 */
@IncludeCommand
object CommandUsername : QuickImports {

    fun createCommand(): Command {
        return CommandBuilder
            .begin("username")
            .handler { command, _ ->
                val username = player.name.string
                val formattedUsername = bypassNameProtection(variable(username))
                val formattedUsernameWithEvents = formattedUsername.styled {
                    it
                        .withItalic(true)
                        .withUnderline(true)
                        .withHoverEvent(HoverEvent(HoverEvent.Action.SHOW_TEXT, regular("Copy username")))
                        .withClickEvent(ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, username))
                }

                chat(regular(command.result("username", formattedUsernameWithEvents)))
                GLFW.glfwSetClipboardString(mc.window.handle, username)
            }
            .build()
    }

}
