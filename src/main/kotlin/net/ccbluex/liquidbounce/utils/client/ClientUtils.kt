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
package net.ccbluex.liquidbounce.utils.client

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.event.EventManager
import net.ccbluex.liquidbounce.event.events.NotificationEvent
import net.ccbluex.liquidbounce.features.command.Command
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.interfaces.ClientTextColorAdditions
import net.minecraft.client.MinecraftClient
import net.minecraft.client.util.InputUtil
import net.minecraft.text.MutableText
import net.minecraft.text.Text
import net.minecraft.text.TextColor
import net.minecraft.util.Formatting
import net.minecraft.util.Util
import org.apache.commons.lang3.StringUtils
import org.apache.logging.log4j.Logger
import org.lwjgl.glfw.GLFW

val logger: Logger
    get() = LiquidBounce.logger

val inGame: Boolean
    get() = MinecraftClient.getInstance()?.let { mc -> mc.player != null && mc.world != null } ?: false

// Chat formatting
private val clientPrefix = "§f§lLiquid§9§lBounce §8▸ §7".asText()

fun dot() = regular(".")

fun regular(text: MutableText) = text.styled { it.withColor(Formatting.GRAY) }

fun regular(text: String) = text.asText().styled { it.withColor(Formatting.GRAY) }

fun variable(text: MutableText) = text.styled { it.withColor(Formatting.GOLD) }

fun variable(text: String) = text.asText().styled { it.withColor(Formatting.GOLD) }

fun warning(text: MutableText) = text.styled { it.withColor(Formatting.YELLOW) }

fun markAsError(text: String) = text.asText().styled { it.withColor(Formatting.RED) }

fun markAsError(text: MutableText) = text.styled { it.withColor(Formatting.RED) }

fun withColor(text: MutableText, color: TextColor) = text.styled { it.withColor(color) }
fun withColor(text: MutableText, color: Formatting) = text.styled { it.withColor(color) }
fun withColor(text: String, color: Formatting) = text.asText().styled { it.withColor(color) }

fun bypassNameProtection(text: MutableText) = text.styled {
    val color = it.color ?: TextColor.fromFormatting(Formatting.RESET)

    @Suppress("KotlinConstantConditions")
    val newColor = (color as ClientTextColorAdditions).`liquid_bounce$withNameProtectionBypass`()

    it.withColor(newColor)
}

private val defaultMessageMetadata = MessageMetadata()

/**
 * Stores some data used to construct messages.
 * The [id], when the message is sent from a client object,
 * should follow the pattern `ObjectName#UniqueString`
 * to avoid duplicates.
 *
 * This would mean, for example, that a not-in-game exception should
 * from a command named `SomeCommand` with should have the
 * id `SomeCommand#notIngame`.
 */
@JvmRecord
data class MessageMetadata(
    val prefix: Boolean = true,
    val id: String? = null,
    val remove: Boolean = true,
    val count: Int = 1
)

@Deprecated(
    "Replaced by MessageMetadata. Use chat(vararg texts: Text, metadata: MessageMetadata) instead.",
    replaceWith = ReplaceWith("chat(*texts, metadata = MessageMetadata(prefix = prefix))")
)
fun chat(vararg texts: Text, prefix: Boolean) {
    chat(*texts, metadata =  MessageMetadata(prefix = prefix))
}

/**
 * Adds a new chat message.
 */
fun chat(vararg texts: Text, metadata: MessageMetadata = defaultMessageMetadata) {
    val literalText = if (metadata.prefix) clientPrefix.copy() else Text.literal("")
    texts.forEach { literalText.append(it) }

    if (mc.player == null) {
        logger.info("(Chat) ${literalText.convertToString()}")
        return
    }

    val chatHud = mc.inGameHud.chatHud

    if (metadata.remove && StringUtils.isNotEmpty(metadata.id)) {
        chatHud.removeMessage(metadata.id)
    }

    chatHud.addMessage(literalText, metadata.id, metadata.count)
}

fun chat(text: Text, module: Module) = chat(text, metadata = MessageMetadata(id = "${module.name}#info"))

fun chat(text: Text, command: Command) = chat(text, metadata = MessageMetadata(id = "${command.name}#info"))

fun chat(text: String, module: Module) = chat(text.asText(), module)

fun chat(text: String, command: Command) = chat(text.asText(), command)

fun chat(text: String) = chat(text.asText())

fun notification(title: Text, message: String, severity: NotificationEvent.Severity) =
    EventManager.callEvent(NotificationEvent(title.string, message, severity))

fun notification(title: String, message: Text, severity: NotificationEvent.Severity) =
    EventManager.callEvent(NotificationEvent(title, message.string, severity))

fun notification(title: String, message: String, severity: NotificationEvent.Severity) =
    EventManager.callEvent(NotificationEvent(title, message, severity))

/**
 * Translated key code to key name using GLFW and translates unknown key to NONE
 */
fun key(name: String) = when (name.lowercase()) {
    "rshift" -> GLFW.GLFW_KEY_RIGHT_SHIFT
    "lshift" -> GLFW.GLFW_KEY_LEFT_SHIFT
    else -> runCatching {
        InputUtil.fromTranslationKey("key.keyboard.${name.lowercase()}").code
    }.getOrElse { GLFW.GLFW_KEY_UNKNOWN }
}

/**
 * Translated key code to key name using GLFW and translates unknown key to NONE
 */
fun keyName(keyCode: Int) = when (keyCode) {
    GLFW.GLFW_KEY_UNKNOWN -> "NONE"
    else -> InputUtil.fromKeyCode(keyCode, -1).translationKey
        .split(".")
        .drop(2)
        .joinToString(separator = "_")
        .uppercase()
}

/**
 * Open uri in browser
 */
fun browseUrl(url: String) = Util.getOperatingSystem().open(url)

val TextColor.bypassesNameProtection: Boolean
    get() = (this as ClientTextColorAdditions).`liquid_bounce$doesBypassingNameProtect`()
