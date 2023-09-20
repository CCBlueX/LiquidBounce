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

package net.ccbluex.liquidbounce.features.module.modules.misc

import net.ccbluex.liquidbounce.event.KeyboardKeyEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.command.CommandManager
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.minecraft.client.gui.screen.ChatScreen
import net.minecraft.client.gui.screen.DeathScreen

/**
 * KeepChatAfterDeath module
 *
 * Allows you to use the chat on the death screen.
 */
object ModuleKeepChatAfterDeath : Module("KeepChatAfterDeath", Category.MISC) {

    val keyboardKeyHandler = handler<KeyboardKeyEvent> {
        if (mc.currentScreen !is DeathScreen) {
            return@handler
        }

        val options = mc.options
        if (options.chatKey.boundKey.code == it.keyCode) {
            openChat("")
            return@handler
        }

        if (options.commandKey.boundKey.code == it.keyCode) {
            openChat("/")
            return@handler
        }

        val prefix = CommandManager.Options.prefix[0]
        if (prefix.code == it.keyCode) {
            openChat(prefix.toString())
            return@handler
        }
    }

    private fun openChat(text: String) {
        mc.setScreen(ChatScreen(text))
        (mc.currentScreen as ChatScreen).focused = null
    }
}
