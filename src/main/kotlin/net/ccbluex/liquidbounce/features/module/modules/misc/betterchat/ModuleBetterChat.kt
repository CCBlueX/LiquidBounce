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
package net.ccbluex.liquidbounce.features.module.modules.misc.betterchat

import net.ccbluex.liquidbounce.event.events.KeyboardKeyEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.command.CommandManager
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.minecraft.client.gui.screen.ChatScreen
import net.minecraft.client.gui.screen.DeathScreen

/**
 * BetterChat Module
 *
 * Quality of life improvements to the in-game chat.
 */
object ModuleBetterChat : Module("BetterChat", Category.MISC, aliases = arrayOf("AntiSpam")) {

    val infiniteLength by boolean("Infinite", true)
    val antiClear by boolean("AntiClear", true)

    /**
     * Allows you to use the chat on the death screen.
     */
    private val keepAfterDeath by boolean("KeepAfterDeath", true)

    var antiChatClearPaused = false

    init {
        tree(AntiSpam)
    }

    @Suppress("unused")
    val keyboardKeyHandler = handler<KeyboardKeyEvent> {
        if (keepAfterDeath && mc.currentScreen !is DeathScreen) {
            return@handler
        }

        val options = mc.options
        val prefix = CommandManager.Options.prefix[0]
        when (it.keyCode) {
            options.chatKey.boundKey.code -> openChat("")
            options.commandKey.boundKey.code -> openChat("/")
            prefix.code -> openChat(prefix.toString())
        }
    }

    private fun openChat(text: String) {
        mc.send { mc.setScreen(ChatScreen(text)) }
    }

}
