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
package net.ccbluex.liquidbounce.features.module.modules.misc.betterchat

import net.ccbluex.liquidbounce.config.types.NamedChoice
import net.ccbluex.liquidbounce.config.types.nesting.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.events.ChatReceiveEvent
import net.ccbluex.liquidbounce.event.events.KeyboardKeyEvent
import net.ccbluex.liquidbounce.event.events.NotificationEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.suspendHandler
import net.ccbluex.liquidbounce.features.command.CommandManager
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.modules.client.ModuleTranslation
import net.ccbluex.liquidbounce.utils.client.*
import net.ccbluex.liquidbounce.utils.collection.Pools
import net.minecraft.client.gui.hud.ChatHudLine
import net.minecraft.client.gui.screen.DeathScreen
import net.minecraft.text.CharacterVisitor
import org.lwjgl.glfw.GLFW

/**
 * BetterChat Module
 *
 * Quality of life improvements to the in-game chat.
 */
object ModuleBetterChat : ClientModule("BetterChat", Category.RENDER, aliases = listOf("AntiSpam")) {
    private val features by multiEnumChoice("Features",
        Features.INFINITE,
        Features.ANTI_CLEAR,
        Features.KEEP_AFTER_DEATH
    )

    val infiniteLength get() = Features.INFINITE in features
    val antiClear get() = Features.ANTI_CLEAR in features

    /**
     * Allows you to transform your message text to unicode.
     */
    private val forceUnicodeChat get() = Features.FORCE_UNICODE_CHAT in features

    /**
     * Allows you to use the chat on the death screen.
     */
    private val keepAfterDeath get() = Features.KEEP_AFTER_DEATH in features

    private object AppendPrefix : MessageModifier("AppendPrefix", false) {
        val prefix by text("Prefix", "> ")

        override fun getMessage(content: String) = prefix + content
    }

    private object AppendSuffix : MessageModifier("AppendSuffix", false) {
        val suffix by text("Suffix", " | \uD835\uDE7B\uD835\uDE92\uD835\uDE9A\uD835\uDE9E" +
            "\uD835\uDE92\uD835\uDE8D\uD835\uDE71\uD835\uDE98\uD835\uDE9E\uD835\uDE97\uD835\uDE8C\uD835\uDE8E")

        override fun getMessage(content: String) = content + suffix
    }

    private val autoTranslate by multiEnumChoice<ChatReceiveEvent.ChatType>("AutoTranslate")

    object Copy : ToggleableConfigurable(this, "Copy", true) {
        private val notification by boolean("Notificate", true)
        val highlight by boolean("Highlight", true)

        @JvmStatic
        fun copyMessage(parts: List<ChatHudLine.Visible>, button: Int) {
            val content = Pools.buildStringPooled {
                val visitor = CharacterVisitor { _, _, codePoint ->
                    appendCodePoint(codePoint)
                    true
                }

                for (line in parts) {
                    line.content().accept(visitor)
                }
            }

            if (isAnyPressed(
                    GLFW.GLFW_KEY_LEFT_SHIFT,
                    GLFW.GLFW_KEY_RIGHT_SHIFT
                ) && button == GLFW.GLFW_MOUSE_BUTTON_1
            ) {
                mc.keyboard.clipboard = content

                if (notification) {
                    notification(
                        "ChatCopy",
                        "The line is copied",
                        NotificationEvent.Severity.SUCCESS
                    )
                }
            } else if (button == GLFW.GLFW_MOUSE_BUTTON_2) {
                mc.openChat(content)
            }
        }

        private fun isAnyPressed(vararg keys: Int): Boolean =
            keys.any {
                GLFW.glfwGetKey(mc.window.handle, it) == GLFW.GLFW_PRESS
            }
    }

    init {
        tree(AppendPrefix)
        tree(AppendSuffix)
        tree(AntiSpam)
        tree(Copy)
    }

    var antiChatClearPaused = false

    @Suppress("unused")
    private val keyboardKeyHandler = handler<KeyboardKeyEvent> {
        if (keepAfterDeath && mc.currentScreen !is DeathScreen) {
            return@handler
        }

        val options = mc.options
        val prefix = CommandManager.Options.prefix[0]
        when (it.keyCode) {
            options.chatKey.boundKey.code -> mc.openChat("")
            options.commandKey.boundKey.code -> mc.openChat("/")
            prefix.code -> mc.openChat(prefix.toString())
        }
    }

    @Suppress("unused")
    private val chatReceiveHandler = suspendHandler<ChatReceiveEvent> { event ->
        if (event.type !in autoTranslate) {
            return@suspendHandler
        }

        val plainMessage = event.message.stripMinecraftColorCodes()
        if (plainMessage.isBlank()) {
            return@suspendHandler
        }

        val result = ModuleTranslation.translate(text = plainMessage)
        if (result.isValid) {
            chat(
                result.toResultText(),
                metadata = MessageMetadata(prefix = false)
            )
        }
    }

    fun modifyMessage(content: String): String {
        if (!running) {
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

    private sealed class MessageModifier(
        name: String,
        enabled: Boolean
    ) : ToggleableConfigurable(this, name, enabled) {
        fun modifyMessage(content: String) =
            if (!this.enabled) {
                content
            } else {
                getMessage(content)
            }

        abstract fun getMessage(content: String): String
    }

    @Suppress("unused")
    private enum class Features(override val choiceName: String) : NamedChoice {
        INFINITE("Infinite"),
        ANTI_CLEAR("AntiClear"),
        KEEP_AFTER_DEATH("KeepAfterDeath"),
        FORCE_UNICODE_CHAT("ForceUnicodeChat")
    }
}
