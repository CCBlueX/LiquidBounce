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

import net.ccbluex.liquidbounce.config.ToggleableConfigurable
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

    private object AppendPrefix : MessageModifier("AppendPrefix", false) {
        val prefix by text("Prefix", "> ")

        override fun getMessage(content: String) = prefix + content
    }

    private object AppendSuffix : MessageModifier("AppendSuffix", false) {
        val suffix by text("Suffix", " | \uD835\uDE7B\uD835\uDE92\uD835\uDE9A\uD835\uDE9E" +
            "\uD835\uDE92\uD835\uDE8D\uD835\uDE71\uD835\uDE98\uD835\uDE9E\uD835\uDE97\uD835\uDE8C\uD835\uDE8E")

        override fun getMessage(content: String) = content + suffix
    }

    init {
        tree(AppendPrefix)
        tree(AppendSuffix)
    }

    /**
     * Allows you to transform your message text to unicode.
     */
    private val forceUnicodeChat by boolean("ForceUnicodeChat", false)

    init {
        tree(AntiSpam)
    }

    var antiChatClearPaused = false

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

    fun modifyMessage(content: String): String {
        if (!enabled) {
            return content
        }

        val result = if (forceUnicodeChat) {
            applyUnicodeTransformation(content)
        } else {
            content
        }

        return AppendSuffix.modifyMessage(AppendPrefix.modifyMessage(result))
    }

    private fun applyUnicodeTransformation(content: String): String {
        return buildString {
            for (c in content) {
                if (c.code in 33..128) {
                    append(Character.toChars(c.code + 65248))
                } else {
                    append(c)
                }
            }
        }
    }

    private abstract class MessageModifier(
        name: String,
        enabled: Boolean
    ) : ToggleableConfigurable(this, name, enabled) {
        fun modifyMessage(content: String): String {
            if (!this.enabled) {
                return content
            }

            return getMessage(content)
        }

        abstract fun getMessage(content: String): String
    }

}
