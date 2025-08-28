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

import it.unimi.dsi.fastutil.longs.LongArrayList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import net.ccbluex.liquidbounce.config.types.NamedChoice
import net.ccbluex.liquidbounce.config.types.nesting.Choice
import net.ccbluex.liquidbounce.config.types.nesting.ChoiceConfigurable
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
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
object ModuleSpammer : ClientModule("Spammer", Category.MISC, disableOnQuit = true) {

    init {
        doNotIncludeAlways()
    }

    private val delay by floatRange("Delay", 2f..4f, 0f..300f, "secs")
    private val mps by intRange("MPS", 1..1, 1..500, "messages")
    private val message = choices("MessageSource", 0) {
        arrayOf(MessageProvider.Setting, MessageProvider.File)
    }

    private sealed class MessageProvider(name: String) : Choice(name) {
        override val parent: ChoiceConfigurable<*>
            get() = message

        abstract fun nextMessage(): String

        object Setting : MessageProvider("Setting") {
            private var linear = 0

            private val texts by textList("Message", mutableListOf(
                "LiquidBounce Nextgen | CCBlueX on [youtube] | liquidbounce{.net}",
                "I'm using LiquidBounce Nextgen and you should too!",
                "Check out LiquidBounce Nextgen - the best Minecraft client!",
                "Tired of losing? Try LiquidBounce Nextgen!",
            ))

            override fun nextMessage(): String =
                when (pattern) {
                    SpammerPattern.RANDOM -> texts.random()
                    SpammerPattern.LINEAR -> texts[linear++ % texts.size]
                }
        }

        object File : MessageProvider("File") {

            private val source by file("Source").onChanged {
                lineIndex.clear()
                if (!it.isFile) return@onChanged

                lineIndex.add(0L)
                RandomAccessFile(it, "r").use { raf ->
                    while (raf.skipLine() != 0L) {
                        lineIndex.add(raf.filePointer)
                    }
                }
            }

            private var linear = 0
            private val lineIndex = LongArrayList()

            override fun nextMessage(): String {
                require(lineIndex.isNotEmpty()) { "File is empty or not selected" }

                val index = when (pattern) {
                    SpammerPattern.RANDOM -> lineIndex.getLong(Random.nextInt(lineIndex.size))
                    SpammerPattern.LINEAR -> lineIndex.getLong(linear++ % lineIndex.size)
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
                    message.activeChoice.nextMessage()
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
        if (text.length > 256) {
            chat("Spammer message is too long! (Max 256 characters)")
            return
        }

        if (text.startsWith('/')) {
            network.sendCommand(text.substring(1))
        } else {
            network.sendChatMessage(text)
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
            mc.networkHandler?.playerList?.mapNotNull {
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

    enum class MessageConverterMode(override val choiceName: String, val convert: (String) -> String) : NamedChoice {
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

    enum class SpammerPattern(override val choiceName: String) : NamedChoice {
        RANDOM("Random"),
        LINEAR("Linear"),
    }

}
