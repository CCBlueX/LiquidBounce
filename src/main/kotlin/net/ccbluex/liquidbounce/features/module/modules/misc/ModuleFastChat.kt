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
package net.ccbluex.liquidbounce.features.module.modules.misc

import net.ccbluex.liquidbounce.config.types.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.events.KeyboardKeyEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import org.lwjgl.glfw.GLFW

/**
 * Module Fast Chat.
 *
 * Use keybindings to send preset messages.
 */
object ModuleFastChat : ClientModule("FastChat", Category.MISC) {

    private val autoDisable by boolean("AutoDisable", true)

    private val shortcuts = arrayOf(
        MessageShortcut("Shortcut-1", GLFW.GLFW_KEY_KP_7, "Please defend", "Defend the base"),
        MessageShortcut("Shortcut-2", GLFW.GLFW_KEY_KP_8, "Enemy incoming", "Enemy approaching"),
        MessageShortcut("Shortcut-3", GLFW.GLFW_KEY_KP_9, "I'm defending"),
        MessageShortcut("Shortcut-4", GLFW.GLFW_KEY_KP_4, "We need enchantments", "Get enchantments"),
        MessageShortcut("Shortcut-5", GLFW.GLFW_KEY_KP_5, "I'm attacking"),
        MessageShortcut("Shortcut-6", GLFW.GLFW_KEY_KP_6, "I need help", "Help me"),
        MessageShortcut("Shortcut-7", GLFW.GLFW_KEY_KP_1, "GG", "gg"),
        MessageShortcut("Shortcut-8", GLFW.GLFW_KEY_KP_2, "Thanks", "Thank you"),
        MessageShortcut("Shortcut-9", GLFW.GLFW_KEY_KP_3, "Good job", "Well played"),
    ).onEach(::tree)

    private class MessageShortcut(
        name: String,
        bind: Int,
        vararg messages: String,
    ) : ToggleableConfigurable(ModuleFastChat, name, true) {
        val bind by bind("Key", bind)
        val messages by textArray("Messages", messages.toMutableList())
    }

    @Suppress("unused")
    private val keyHandler = handler<KeyboardKeyEvent> { event ->
        val shortcut = shortcuts.find {
            it.enabled && it.bind.boundKey == event.key
        } ?: return@handler

        if (shortcut.messages.isEmpty()) {
            return@handler
        }

        network.sendChatMessage(shortcut.messages.random())
        if (autoDisable) {
            this.enabled = false
        }
    }

}
