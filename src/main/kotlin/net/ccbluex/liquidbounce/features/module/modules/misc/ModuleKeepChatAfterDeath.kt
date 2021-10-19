/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2016 - 2021 CCBlueX
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
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.minecraft.client.gui.screen.ChatScreen
import org.lwjgl.glfw.GLFW

/**
 * KeepChatAfterDeath module
 *
 * Allows you to use the chat on the death screen.
 */
object ModuleKeepChatAfterDeath : Module("KeepChatAfterDeath", Category.MISC) {

    var chatOpen = false

    val packetHandler = handler<KeyboardKeyEvent> { event ->
        if (player.isDead) {
            val key = event.keyCode
            val options = mc.options!!
            if (!chatOpen && (key == options.keyChat!!.boundKey.code || key == options.keyCommand!!.boundKey.code)) {
                // BUG: first clicked key (default T or /) is added to the chat field
                mc.setScreen(ChatScreen(""))
                chatOpen = true
                return@handler
            }
            if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_ESCAPE) {
                chatOpen = false
            }
        }
    }

    override fun disable() {
        if (player.isDead) {
            player.setShowsDeathScreen(true)
            if (chatOpen) {
                mc.setScreen(null)
            }
        }
        chatOpen = false
    }
}
