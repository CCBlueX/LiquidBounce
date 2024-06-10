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
package net.ccbluex.liquidbounce.features.module.modules.misc

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.ccbluex.liquidbounce.config.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.events.ChatReceiveEvent
import net.ccbluex.liquidbounce.event.events.NotificationEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.interfaces.ChatHudLineAddition
import net.ccbluex.liquidbounce.interfaces.ChatMessageAddition
import net.ccbluex.liquidbounce.lang.translation
import net.ccbluex.liquidbounce.utils.client.*
import net.ccbluex.liquidbounce.utils.io.FileTransferable
import net.minecraft.client.texture.NativeImage
import net.minecraft.text.ClickEvent
import net.minecraft.text.HoverEvent
import net.minecraft.text.Text
import net.minecraft.text.TextVisitFactory
import net.minecraft.util.Formatting
import org.apache.commons.lang3.StringUtils
import java.awt.HeadlessException
import java.awt.Toolkit
import java.io.File
import java.util.function.Consumer

/**
 * BetterChat Module
 *
 * Quality of life improvements to the in-game chat.
 */
object ModuleBetterChat : Module("BetterChat", Category.MISC, aliases = arrayOf("AntiSpam")) {

    val infiniteLength = boolean("Infinite", true)
    val antiClear = boolean("AntiClear", true)
    val betterScreenshotMessages = boolean("BetterScreenshotMessages", true)

    var antiChatClearPaused = false

    private object AntiSpam : ToggleableConfigurable(this, "AntiSpam", true) {

        val regexFilters = mutableListOf<Regex>()

        val stack = boolean("StackMessages", true)
        val filters = textArray("Filters", mutableListOf()).onChanged {
            compileFilters()
        }

        fun compileFilters() {
            regexFilters.clear()
            filters.get().forEach {
                regexFilters.add(Regex(it))
            }
        }

    }

    init {
        tree(AntiSpam)
    }

    @Suppress("unused")
    val chatHandler = handler<ChatReceiveEvent> { event ->
        if (!AntiSpam.enabled) {
            return@handler
        }

        val string = TextVisitFactory.removeFormattingCodes(event.textData)
        var content = StringUtils.substringAfter(string, ">") ?: string
        content = content.trim()

        if (AntiSpam.regexFilters.isNotEmpty()) {
            val shouldBeRemoved = AntiSpam.regexFilters.any {
                it.matches(content)
            }

            if (shouldBeRemoved) {
                event.cancelEvent()
                return@handler
            }
        }

        // stacks messages so that e.g., when a message is sent twice
        // it gets replaces by a new messages that has `[2]` appended
        if (AntiSpam.stack.get()) {
            // always cancel so each message gets an ID
            event.cancelEvent()

            // appends "external" to every message id
            // so servers can't troll users with messages that
            // imitate client messages
            val id = "$string-external"

            val literalText = Text.literal("")
            val text = event.applyChatDecoration.invoke(event.textData)
            literalText.append(text)

            @Suppress("CAST_NEVER_SUCCEEDS") // succeeds with mixins
            val other = mc.inGameHud.chatHud.messages.find {
                (it as ChatMessageAddition).`liquid_bounce$getId`() == id
            }

            var count = 1
            other?.let {
                @Suppress("CAST_NEVER_SUCCEEDS", "KotlinRedundantDiagnosticSuppress") // succeeds with mixins
                count += (other as ChatHudLineAddition).`liquid_bounce$getCount`()
                literalText.append(" ${Formatting.GRAY}[$count]")
            }

            val data = MessageMetadata(prefix = false, id = id, remove = true, count = count)
            chat(texts = arrayOf(literalText), data)
        }
    }

    /**
     * Saves a [nativeImage] to a [file] and notifies the user over the [messageReceiver].
     */
    fun saveScreenshot(messageReceiver: Consumer<Text>, nativeImage: NativeImage, file: File) {
        val scope = CoroutineScope(Dispatchers.Default)
        scope.launch {
            try {
                nativeImage.use {
                    nativeImage.writeTo(file)
                    sendScreenshotMessage(messageReceiver, file)
                }
            } catch (e: Exception) {
                logger.warn("Couldn't save screenshot", e)
                notification(
                    "BetterChat",
                    translation("liquidbounce.module.betterChat.screenshot.fail"),
                    NotificationEvent.Severity.ERROR
                )
            }
        }
    }

    /**
     * Constructs and adds a message that allows copying and opening a [file]
     * representing a screenshot.
     */
    private fun sendScreenshotMessage(messageReceiver: Consumer<Text>, file: File) {
        val text = Text.literal(Formatting.WHITE.toString())
        text.append(translation("liquidbounce.module.betterChat.screenshot.saved").styled {
            it.withHoverEvent(
                HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal(file.getName()))
            )
        })
        text.append(
            Text.literal(" [").withColor(Formatting.GRAY.colorValue!!)
        )
        text.append(translation("liquidbounce.module.betterChat.screenshot.open").styled {
            it.withClickEvent(
                ClickEvent(ClickEvent.Action.OPEN_FILE, file.absolutePath)
            )
        }.withColor(Formatting.AQUA.colorValue!!).formatted(Formatting.UNDERLINE))
        text.append(
            Text.literal("] [").withColor(Formatting.GRAY.colorValue!!)
        )
        text.append(translation("liquidbounce.module.betterChat.screenshot.copy").styled {
            it.withClickEvent(RunnableClickEvent {
                val scope = CoroutineScope(Dispatchers.Default)
                scope.launch {
                    try {
                        val clipboard = Toolkit.getDefaultToolkit().systemClipboard
                        clipboard.setContents(FileTransferable(file), null)
                    } catch (e: HeadlessException) {
                        logger.warn("Copying failed", e)
                        notification(
                            "BetterChat",
                            translation("liquidbounce.module.betterChat.screenshot.copyFail"),
                            NotificationEvent.Severity.ERROR
                        )
                    }
                }
            })
        }.withColor(Formatting.GOLD.colorValue!!).formatted(Formatting.UNDERLINE))
        text.append(
            Text.literal("]").withColor(Formatting.GRAY.colorValue!!)
        )

        mc.run { messageReceiver.accept(text) }
    }

}
