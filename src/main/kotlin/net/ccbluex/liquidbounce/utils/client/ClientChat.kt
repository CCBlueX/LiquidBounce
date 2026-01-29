/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2026 CCBlueX
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
import net.ccbluex.liquidbounce.interfaces.TextColorAddition
import net.ccbluex.liquidbounce.lang.translation
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.utils.text.RunnableClickEvent
import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.ChatScreen
import net.minecraft.network.chat.ClickEvent
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.HoverEvent
import net.minecraft.network.chat.MutableComponent
import net.minecraft.network.chat.Style
import net.minecraft.network.chat.TextColor
import java.io.File

// Chat formatting
private val clientPrefix: Component = "".asText()
    .withStyle(ChatFormatting.RESET, ChatFormatting.GRAY)
    .append(gradientText("LiquidBounce", Color4b.fromHex("#4677ff"), Color4b.fromHex("#24AA7F")))
    .append(" ▸ ".asText().withStyle(ChatFormatting.RESET, ChatFormatting.GRAY))

fun regular(text: MutableComponent): MutableComponent = text.withStyle(ChatFormatting.GRAY)

fun regular(text: String): MutableComponent = text.asText().withStyle(ChatFormatting.GRAY)

fun variable(text: MutableComponent): MutableComponent = text.withStyle(ChatFormatting.GOLD)

fun variable(text: String): MutableComponent = text.asText().withStyle(ChatFormatting.GOLD)

fun clickablePath(file: File): MutableComponent =
    variable(file.absolutePath)
        .onClick(ClickEvent.OpenFile(file))
        .onHover(HoverEvent.ShowText("Open".asPlainText()))

fun highlight(text: MutableComponent): MutableComponent = text
    .withStyle(Style.EMPTY + Color4b.LIQUID_BOUNCE + ChatFormatting.BOLD)

fun highlight(text: String): MutableComponent = text.asText()
    .withStyle(Style.EMPTY + Color4b.LIQUID_BOUNCE + ChatFormatting.BOLD)

fun warning(text: MutableComponent): MutableComponent = text.withStyle(ChatFormatting.YELLOW)

fun warning(text: String): MutableComponent = text.asText().withStyle(ChatFormatting.YELLOW)

fun markAsError(text: String): MutableComponent = text.asText().withStyle(ChatFormatting.RED)

fun markAsError(text: MutableComponent): MutableComponent = text.withStyle(ChatFormatting.RED)

inline fun MutableComponent.withColor(value: ChatFormatting?): MutableComponent =
    setStyle(style.withColor(value))

inline fun MutableComponent.withColor(value: TextColor?): MutableComponent =
    setStyle(style.withColor(value))

inline fun MutableComponent.bold(value: Boolean?): MutableComponent =
    setStyle(style.withBold(value))

inline fun MutableComponent.obfuscated(value: Boolean?): MutableComponent =
    setStyle(style.withObfuscated(value))

inline fun MutableComponent.strikethrough(value: Boolean?): MutableComponent =
    setStyle(style.withStrikethrough(value))

inline fun MutableComponent.underline(value: Boolean?): MutableComponent =
    setStyle(style.withUnderlined(value))

inline fun MutableComponent.italic(value: Boolean?): MutableComponent =
    setStyle(style.withItalic(value))

inline fun MutableComponent.onHover(event: HoverEvent?): MutableComponent =
    setStyle(style.withHoverEvent(event))

inline fun MutableComponent.onClick(event: ClickEvent?): MutableComponent =
    setStyle(style.withClickEvent(event))

inline fun MutableComponent.onClickRun(callback: Runnable): MutableComponent =
    setStyle(style.withClickEvent(RunnableClickEvent(callback)))

inline operator fun MutableComponent.plusAssign(other: String) {
    this.append(other)
}

inline operator fun MutableComponent.plusAssign(other: Component) {
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
fun gradientText(text: String, startColor: Color4b, endColor: Color4b): MutableComponent {
    return text.foldIndexed("".asText()) { index, newText, char ->
        val factor = if (text.length > 1) index / (text.length - 1.0) else 0.0
        val color = startColor.interpolateTo(endColor, factor)

        newText.append(
            char.toString().asPlainText(Style.EMPTY + color)
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
fun MutableComponent.copyable(
    copyContent: String = this.string,
    hover: HoverEvent? = HoverEvent.ShowText(
        translation("liquidbounce.tooltip.clickToCopy")
    )
): MutableComponent = apply {
    hover?.let(::onHover)
    onClick(ClickEvent.CopyToClipboard(copyContent))
}

fun MutableComponent.bypassNameProtection(): MutableComponent = withStyle {
    val color = it.color ?: TextColor.fromLegacyFormat(ChatFormatting.RESET)

    @Suppress("CAST_NEVER_SUCCEEDS")
    val newColor = (color as TextColorAddition).`liquid_bounce$withNameProtectionBypass`()

    it.withColor(newColor)
}

/**
 * Open a [ChatScreen] with given text,
 * or set the text of current [ChatScreen]
 */
fun Minecraft.openChat(text: String, draft: Boolean = false) = schedule {
    (screen as? MixinChatScreenAccessor)?.input?.setValue(text) ?: setScreen(ChatScreen(text, draft))
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
) {
    companion object {
        @JvmStatic
        fun byModule(module: ClientModule) = MessageMetadata(id = "M${module.name}#info")

        @JvmStatic
        fun byCommand(command: Command) = MessageMetadata(id = "C${command.name}#info")
    }
}

fun chat(text: Component, metadata: MessageMetadata = defaultMessageMetadata) {
    val realText = if (metadata.prefix) clientPrefix.copy().append(text) else text

    if (mc.player == null) {
        logger.info("(Chat) ${realText.string}")
        return
    }

    val chatHud = mc.gui.chat

    if (metadata.remove && !metadata.id.isNullOrEmpty()) {
        chatHud.removeMessage(metadata.id)
    }

    chatHud.addMessage(realText, metadata.id, metadata.count)
}

/**
 * Adds a new chat message.
 */
fun chat(vararg texts: Component, metadata: MessageMetadata = defaultMessageMetadata) {
    chat(texts.asText(), metadata)
}

fun chat(text: Component, module: ClientModule) = chat(text, metadata = MessageMetadata.byModule(module))

fun chat(text: Component, command: Command) = chat(text, metadata = MessageMetadata.byCommand(command))

fun chat(text: String, module: ClientModule) = chat(text.asPlainText(), module)

fun chat(text: String, command: Command) = chat(text.asPlainText(), command)

fun chat(text: String) = chat(text.asPlainText())

fun notification(title: Component, message: String, severity: NotificationEvent.Severity) =
    EventManager.callEvent(NotificationEvent(title.string, message, severity))

fun notification(title: String, message: Component, severity: NotificationEvent.Severity) =
    EventManager.callEvent(NotificationEvent(title, message.string, severity))

fun notification(title: String, message: String, severity: NotificationEvent.Severity) =
    EventManager.callEvent(NotificationEvent(title, message, severity))

val TextColor.bypassesNameProtection: Boolean
    @Suppress("CAST_NEVER_SUCCEEDS")
    get() = (this as TextColorAddition).`liquid_bounce$doesBypassingNameProtect`()
