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
package net.ccbluex.liquidbounce.features.module.modules.misc

import it.unimi.dsi.fastutil.longs.LongArrayList
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.ccbluex.fastutil.longListOf
import net.ccbluex.liquidbounce.api.core.ioScope
import net.ccbluex.liquidbounce.config.types.list.Tagged
import net.ccbluex.liquidbounce.config.types.group.Mode
import net.ccbluex.liquidbounce.config.types.group.ModeValueGroup
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.markAsError
import net.ccbluex.liquidbounce.utils.io.skipLine
import net.ccbluex.liquidbounce.utils.kotlin.mapString
import net.ccbluex.liquidbounce.utils.kotlin.random
import org.apache.commons.lang3.RandomStringUtils
import java.io.RandomAccessFile
import kotlin.random.Random
import kotlin.time.Duration.Companion.seconds

/**
 * Spammer module
 *
 * Spams the chat with a given message.
 */
object ModuleSpammer : ClientModule("Spammer", ModuleCategories.MISC, disableOnQuit = true) {

    init {
        doNotIncludeAlways()
    }

    private const val MAX_CHARS = 256

    private val delay by floatRange("Delay", 2f..4f, 0f..300f, "secs")
    private val mps by intRange("MPS", 1..1, 1..500, "messages")
    private val message = choices("MessageSource", 0) {
        arrayOf(MessageProvider.Setting, MessageProvider.File)
    }

    private sealed class MessageProvider(name: String) : Mode(name) {
        override val parent: ModeValueGroup<*>
            get() = message

        abstract fun nextMessage(): String

        object Setting : MessageProvider("Setting") {
            private val linear = atomic(0)

            private val texts by textList("Message", mutableListOf(
                "LiquidBounce Nextgen | CCBlueX on [youtube] | liquidbounce{.net}",
                "I'm using LiquidBounce Nextgen and you should too!",
                "Check out LiquidBounce Nextgen - the best Minecraft client!",
                "Tired of losing? Try LiquidBounce Nextgen!",
            ))

            override fun nextMessage(): String =
                when (pattern) {
                    SpammerPattern.RANDOM -> texts.random()
                    SpammerPattern.LINEAR -> texts[linear.getAndIncrement() % texts.size]
                }
        }

        object File : MessageProvider("File") {

            private val coroutineName = CoroutineName("SpammerFileSourceReader")

            private val source by file("Source").onChanged {
                if (!it.isFile) return@onChanged

                ioScope.launch(coroutineName) {
                    val newIndices = LongArrayList()
                    newIndices.add(0L)
                    RandomAccessFile(it, "r").use { raf ->
                        while (raf.skipLine() != 0L) {
                            newIndices.add(raf.filePointer)
                        }
                    }
                    lineIndex = newIndices
                }
            }

            private val linear = atomic(0)
            @Volatile
            private var lineIndex = longListOf()

            override fun nextMessage(): String {
                val lineIndex = lineIndex
                require(lineIndex.isNotEmpty()) { "File is empty or not selected" }

                val index = when (pattern) {
                    SpammerPattern.RANDOM -> lineIndex.getLong(Random.nextInt(lineIndex.size))
                    SpammerPattern.LINEAR -> lineIndex.getLong(linear.getAndIncrement() % lineIndex.size)
                }
                return RandomAccessFile(source, "r").use { raf ->
                    raf.seek(index)
                    raf.readLine()
                }
            }
        }
    }
    private val pattern by enumChoice("Pattern", SpammerPattern.RANDOM)
    private val messageConverterMode by enumChoice("MessageConverter", MessageConverterMode.LEET_CONVERTER)
    private val customFormatter by boolean("CustomFormatter", false)

    override suspend fun enabledEffect() = withContext(Dispatchers.IO) {
        while (true) {
            repeat(mps.random()) {
                val chosenMessage = try {
                    message.activeMode.nextMessage()
                } catch (e: Exception) {
                    chat(markAsError("Failed to get spammer message: $e"))
                    return@repeat
                }

                val text = applyConversion(chosenMessage)

                sendMessageOrCommand(text)
            }

            delay(delay.random().toDouble().seconds)
        }
    }

    private fun applyConversion(text: String): String {
        return messageConverterMode.convert(if (customFormatter) {
            format(text)
        } else {
            "[${RandomStringUtils.insecure().nextAlphabetic(1, 5)}] " +
                MessageConverterMode.RANDOM_CASE_CONVERTER.convert(text)
        })
    }

    private fun sendMessageOrCommand(text: String) {
        if (text.length > MAX_CHARS) {
            chat("Spammer message is too long! (Max $MAX_CHARS characters)")
            return
        }

        if (text.startsWith('/')) {
            network.sendCommand(text.substring(1))
        } else {
            network.sendChat(text)
        }
    }

    private fun format(text: String): String {
        var formattedText = text.replace("%f") {
            Random.nextFloat()
        }.replace("%i") {
            Random.nextInt(10000)
        }.replace("%s") {
            RandomStringUtils.insecure().nextAlphabetic(4, 7)
        }

        if (formattedText.contains("@a")) {
            mc.connection?.onlinePlayers?.mapNotNull {
                it?.profile?.name.takeIf { n -> n != player.gameProfile?.name }
            }?.takeIf { it.isNotEmpty() }?.let { playerNameList ->
                formattedText = formattedText.replace("@a") { playerNameList.random() }
            }
        }

        return formattedText
    }

    private inline fun String.replace(oldValue: String, newValueProvider: () -> Any): String {
        var index = 0
        val newString = StringBuilder(this)
        while (true) {
            index = newString.indexOf(oldValue, startIndex = index)
            if (index == -1) {
                break
            }

            val newValue = newValueProvider().toString()
            newString.replace(index, index + oldValue.length, newValue)

            index += newValue.length
        }
        return newString.toString()
    }

    enum class MessageConverterMode(override val tag: String, val convert: (String) -> String) : Tagged {
        NO_CONVERTER("None", { text ->
            text
        }),
        LEET_CONVERTER("Leet", { text ->
            text.mapString { char ->
                when (char) {
                    'o' -> '0'
                    'l' -> '1'
                    'e' -> '3'
                    'a' -> '4'
                    't' -> '7'
                    's' -> 'Z'
                    else -> char
                }
            }
        }),
        RANDOM_CASE_CONVERTER("Random Case", { text ->
            // Random case the whole string
            text.mapString { char ->
                if (Random.nextBoolean()) char.uppercaseChar() else char.lowercaseChar()
            }
        }),
        RANDOM_SPACE_CONVERTER("Random Space", { text ->
            buildString(text.length * 2) {
                for (char in text) {
                    append(char)
                    if (Random.nextBoolean()) {
                        append(' ')
                    }
                }
            }
        }),
    }

    enum class SpammerPattern(override val tag: String) : Tagged {
        RANDOM("Random"),
        LINEAR("Linear"),
    }

}
