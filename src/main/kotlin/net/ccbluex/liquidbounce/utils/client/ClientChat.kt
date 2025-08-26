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
@file:Suppress("TooManyFunctions", "NOTHING_TO_INLINE")
@file:JvmName("ClientChat")

package net.ccbluex.liquidbounce.utils.client

import net.ccbluex.liquidbounce.event.EventManager
import net.ccbluex.liquidbounce.event.events.NotificationEvent
import net.ccbluex.liquidbounce.features.command.Command
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.injection.mixins.minecraft.gui.MixinChatScreenAccessor
import net.ccbluex.liquidbounce.injection.mixins.minecraft.text.MixinMutableTextAccessor
import net.ccbluex.liquidbounce.interfaces.ClientTextColorAdditions
import net.ccbluex.liquidbounce.lang.translation
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.screen.ChatScreen
import net.minecraft.text.*
import net.minecraft.util.Formatting
import net.minecraft.util.Util
import java.io.File

// Chat formatting
private val clientPrefix: Text = Text.empty()
    .formatted(Formatting.RESET, Formatting.GRAY)
    .append(gradientText("LiquidBounce", Color4b.fromHex("#4677ff"), Color4b.fromHex("#24AA7F")))
    .append(Text.literal(" ▸ ").formatted(Formatting.RESET, Formatting.GRAY))

fun regular(text: MutableText): MutableText = text.formatted(Formatting.GRAY)

fun regular(text: String): MutableText = text.asText().formatted(Formatting.GRAY)

fun variable(text: MutableText): MutableText = text.formatted(Formatting.GOLD)

fun variable(text: String): MutableText = text.asText().formatted(Formatting.GOLD)

fun clickablePath(file: File): MutableText =
    variable(file.absolutePath)
        .onClick { Util.getOperatingSystem().open(file) }
        .onHover(HoverEvent(HoverEvent.Action.SHOW_TEXT, "Open".asText()))

fun highlight(text: MutableText): MutableText = text.formatted(Formatting.DARK_PURPLE)

fun highlight(text: String): MutableText = text.asText().formatted(Formatting.DARK_PURPLE)

fun warning(text: MutableText): MutableText = text.formatted(Formatting.YELLOW)

fun warning(text: String): MutableText = text.asText().formatted(Formatting.YELLOW)

fun markAsError(text: String): MutableText = text.asText().formatted(Formatting.RED)

fun markAsError(text: MutableText): MutableText = text.formatted(Formatting.RED)

inline fun MutableText.withColor(value: Formatting?): MutableText =
    setStyle(style.withColor(value))

inline fun MutableText.withColor(value: TextColor?): MutableText =
    setStyle(style.withColor(value))

inline fun MutableText.bold(value: Boolean?): MutableText =
    setStyle(style.withBold(value))

inline fun MutableText.obfuscated(value: Boolean?): MutableText =
    setStyle(style.withObfuscated(value))

inline fun MutableText.strikethrough(value: Boolean?): MutableText =
    setStyle(style.withStrikethrough(value))

inline fun MutableText.underline(value: Boolean?): MutableText =
    setStyle(style.withUnderline(value))

inline fun MutableText.italic(value: Boolean?): MutableText =
    setStyle(style.withItalic(value))

inline fun MutableText.onHover(event: HoverEvent?): MutableText =
    setStyle(style.withHoverEvent(event))

inline fun MutableText.onClick(event: ClickEvent?): MutableText =
    setStyle(style.withClickEvent(event))

inline fun MutableText.onClick(callback: Runnable): MutableText =
    setStyle(style.withClickEvent(RunnableClickEvent(callback)))

inline operator fun MutableText.plusAssign(other: String) {
    this.append(other)
}

inline operator fun MutableText.plusAssign(other: Text) {
    this.append(other)
}

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
            Text.literal(char.toString()).withColor(color.toARGB())
        )
    }
}

/**
 * Creates text with a copy-to-clipboard click event
 *
 * @param this@copyable The text to make copyable
 * @param copyContent The content to copy when clicked (defaults to text's string representation)
 * @param hover The hover event to apply (defaults to "Click to copy" tooltip)
 * @return Styled text with copy functionality
 */
fun MutableText.copyable(
    copyContent: String = convertToString(),
    hover: HoverEvent? = HoverEvent(
        HoverEvent.Action.SHOW_TEXT,
        translation("liquidbounce.tooltip.clickToCopy")
    )
): MutableText = apply {
    hover?.let(::onHover)
    onClick(ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, copyContent))
}

fun MutableText.bypassNameProtection(): MutableText = styled {
    val color = it.color ?: TextColor.fromFormatting(Formatting.RESET)

    @Suppress("CAST_NEVER_SUCCEEDS")
    val newColor = (color as ClientTextColorAdditions).`liquid_bounce$withNameProtectionBypass`()

    it.withColor(newColor)
}

/**
 * Open a [ChatScreen] with given text,
 * or set the text of current [ChatScreen]
 */
fun MinecraftClient.openChat(text: String) = send {
    (currentScreen as? MixinChatScreenAccessor)?.chatField?.setText(text) ?: setScreen(ChatScreen(text))
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

fun chat(text: Text, metadata: MessageMetadata = defaultMessageMetadata) {
    val realText = if (metadata.prefix) clientPrefix.copy().append(text) else text

    if (mc.player == null) {
        logger.info("(Chat) ${realText.convertToString()}")
        return
    }

    val chatHud = mc.inGameHud.chatHud

    if (metadata.remove && !metadata.id.isNullOrEmpty()) {
        chatHud.removeMessage(metadata.id)
    }

    chatHud.addMessage(realText, metadata.id, metadata.count)
}

/**
 * Adds a new chat message.
 */
fun chat(vararg texts: Text, metadata: MessageMetadata = defaultMessageMetadata) {
    val text: Text = MixinMutableTextAccessor.create(
        PlainTextContent.EMPTY, texts.asList(), Style.EMPTY
    )
    chat(text, metadata)
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
 * Joins a list of [Text] into a single [Text] with the given [separator].
 */
fun List<Text>.joinToText(separator: Text): MutableText {
    val result = Text.empty()
    if (isEmpty()) {
        return result
    }

    with(iterator()) {
        result += next()
        while (hasNext()) {
            result += separator
            result += next()
        }
    }
    return result
}

val TextColor.bypassesNameProtection: Boolean
    @Suppress("CAST_NEVER_SUCCEEDS")
    get() = (this as ClientTextColorAdditions).`liquid_bounce$doesBypassingNameProtect`()
