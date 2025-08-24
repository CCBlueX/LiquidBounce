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

import net.ccbluex.liquidbounce.config.types.NamedChoice
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.kotlin.mapString
import org.apache.commons.lang3.RandomStringUtils
import kotlin.random.Random

/**
 * Spammer module
 *
 * Spams the chat with a given message.
 */
object ModuleSpammer : ClientModule("Spammer", Category.MISC, disableOnQuit = true) {

    init {
        doNotIncludeAlways()
    }

    private val delay by intRange("Delay", 2..4, 0..300, "secs")
    private val mps by intRange("MPS", 1..1, 1..500, "messages")
    private val message by textList("Message", mutableListOf(
        "LiquidBounce Nextgen | CCBlueX on [youtube] | liquidbounce{.net}",
        "I'm using LiquidBounce Nextgen and you should too!",
        "Check out LiquidBounce Nextgen - the best Minecraft client!",
        "Tired of losing? Try LiquidBounce Nextgen!",
    ))
    private val pattern by enumChoice("Pattern", SpammerPattern.RANDOM)
    private val messageConverterMode by enumChoice("MessageConverter", MessageConverterMode.LEET_CONVERTER)
    private val customFormatter by boolean("CustomFormatter", false)

    private var linear = 0

    val repeatable = tickHandler {
        repeat(mps.random()) {
            val chosenMessage = when (pattern) {
                SpammerPattern.RANDOM -> message.random()
                SpammerPattern.LINEAR -> message[linear++ % message.size]
            }

            val text = if (chosenMessage.startsWith('/')) {
                format(chosenMessage)
            }else {
                messageConverterMode.convert(if (customFormatter) {
                    format(chosenMessage)
                } else {
                    "[${RandomStringUtils.randomAlphabetic(1, 5)}] " +
                        MessageConverterMode.RANDOM_CASE_CONVERTER.convert(chosenMessage)
                })
            }

            if (text.length > 256) {
                chat("Spammer message is too long! (Max 256 characters)")
                return@tickHandler
            }

            // Check if message text is command
            if (text.startsWith('/')) {
                network.sendCommand(text.substring(1))
            } else {
                network.sendChatMessage(text)
            }
        }

        waitSeconds(delay.random()) // Delay in seconds (20 ticks per second)
    }

    private fun format(text: String): String {
        var formattedText = text.replace("%f") {
            Random.nextFloat()
        }.replace("%i") {
            Random.nextInt(10000)
        }.replace("%s") {
            RandomStringUtils.randomAlphabetic(4, 7)
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
