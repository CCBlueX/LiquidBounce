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
@file:Suppress("TooManyFunctions")

package net.ccbluex.liquidbounce.utils.client

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.event.EventManager
import net.ccbluex.liquidbounce.event.events.NotificationEvent
import net.ccbluex.liquidbounce.features.command.Command
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.interfaces.ClientTextColorAdditions
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.minecraft.client.MinecraftClient
import net.minecraft.text.MutableText
import net.minecraft.text.Style
import net.minecraft.text.Text
import net.minecraft.text.TextColor
import net.minecraft.util.Formatting
import net.minecraft.util.Util
import org.apache.commons.lang3.StringUtils
import org.apache.logging.log4j.Logger

val logger: Logger
    get() = LiquidBounce.logger

val inGame: Boolean
    get() = MinecraftClient.getInstance()?.let { mc -> mc.player != null && mc.world != null } == true

// Chat formatting
private val clientPrefix = Text.empty()
    .formatted(Formatting.RESET, Formatting.GRAY)
    .append(gradientText("LiquidBounce", Color4b.fromHex("#4677ff"), Color4b.fromHex("#24AA7F")))
    .append(Text.literal(" ▸ ").formatted(Formatting.RESET, Formatting.GRAY))

fun regular(text: MutableText) = text.formatted(Formatting.GRAY)

fun regular(text: String) = text.asText().formatted(Formatting.GRAY)

fun variable(text: MutableText) = text.formatted(Formatting.GOLD)

fun variable(text: String) = text.asText().formatted(Formatting.GOLD)

fun highlight(text: MutableText) = text.formatted(Formatting.DARK_PURPLE)

fun highlight(text: String) = text.asText().formatted(Formatting.DARK_PURPLE)

fun warning(text: MutableText) = text.formatted(Formatting.YELLOW)

fun warning(text: String) = text.asText().formatted(Formatting.YELLOW)

fun markAsError(text: String) = text.asText().formatted(Formatting.RED)

fun markAsError(text: MutableText) = text.formatted(Formatting.RED)

fun withColor(text: MutableText, color: TextColor) = text.styled { style -> style.withColor(color) }
fun withColor(text: MutableText, color: Formatting) = text.formatted(color)
fun withColor(text: String, color: Formatting) = text.asText().formatted(color)

/**
 * Creates text with a color gradient between two colors.
 *
 * @param text The string to apply the gradient to
 * @param startColor The first color in the gradient
 * @param endColor The second color in the gradient
 * @return A MutableText with the gradient applied
 */
fun gradientText(text: String, startColor: Color4b, endColor: Color4b): MutableText {
    return text.foldIndexed(Text.empty()) { index, newText, char ->
        val factor = if (text.length > 1) index / (text.length - 1.0) else 0.0
        val color = startColor.interpolateTo(endColor, factor)

        newText.append(
            Text.literal(char.toString())
                .setStyle(Style.EMPTY.withColor(TextColor.fromRgb(color.toARGB())))
        )
    }
}

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
    chat(texts = texts, metadata = MessageMetadata(prefix = prefix))
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

fun chat(text: Text, module: ClientModule) = chat(text, metadata = MessageMetadata(id = "M${module.name}#info"))

fun chat(text: Text, command: Command) = chat(text, metadata = MessageMetadata(id = "C${command.name}#info"))

fun chat(text: String, module: ClientModule) = chat(text.asText(), module)

fun chat(text: String, command: Command) = chat(text.asText(), command)

fun chat(text: String) = chat(text.asText())

fun notification(title: Text, message: String, severity: NotificationEvent.Severity) =
    EventManager.callEvent(NotificationEvent(title.string, message, severity))

fun notification(title: String, message: Text, severity: NotificationEvent.Severity) =
    EventManager.callEvent(NotificationEvent(title, message.string, severity))

fun notification(title: String, message: String, severity: NotificationEvent.Severity) =
    EventManager.callEvent(NotificationEvent(title, message, severity))

/**
 * Open uri in browser
 */
fun browseUrl(url: String) = Util.getOperatingSystem().open(url)

@Suppress("CAST_NEVER_SUCCEEDS")
val TextColor.bypassesNameProtection: Boolean
    get() = (this as ClientTextColorAdditions).`liquid_bounce$doesBypassingNameProtect`()
