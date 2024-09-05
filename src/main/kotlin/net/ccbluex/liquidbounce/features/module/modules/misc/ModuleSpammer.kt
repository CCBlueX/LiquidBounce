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

import net.ccbluex.liquidbounce.config.NamedChoice
import net.ccbluex.liquidbounce.event.repeatable
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.register.IncludeModule
import net.ccbluex.liquidbounce.utils.client.chat
import org.apache.commons.lang3.RandomStringUtils
import kotlin.random.Random

/**
 * Spammer module
 *
 * Spams the chat with a given message.
 */
@IncludeModule
object ModuleSpammer : Module("Spammer", Category.MISC, disableOnQuit = true) {

    private val delay by intRange("Delay", 2..4, 0..300, "secs")
    private val mps by intRange("MPS", 1..1, 1..500, "messages")
    private val message by textArray("Message", mutableListOf(
        "LiquidBounce Nextgen | CCBlueX on [youtube] | liquidbounce{.net}",
        "I'm using LiquidBounce Nextgen and you should too!",
        "Check out LiquidBounce Nextgen - the best Minecraft client!",
        "Tired of losing? Try LiquidBounce Nextgen!",
    )).doNotIncludeAlways()
    private val pattern by enumChoice("Pattern", SpammerPattern.RANDOM)
        .doNotIncludeAlways()
    private val messageConverterMode by enumChoice("MessageConverter", MessageConverterMode.LEET_CONVERTER)
        .doNotIncludeAlways()
    private val customFormatter by boolean("CustomFormatter", false)
        .doNotIncludeAlways()

    private var linear = 0

    val repeatable = repeatable {
        repeat(mps.random()) {
            val chosenMessage = when (pattern) {
                SpammerPattern.RANDOM -> message.random()
                SpammerPattern.LINEAR -> message[linear++ % message.size]
            }

            val text = messageConverterMode.convert(if (customFormatter) {
                format(chosenMessage)
            } else {
                "[${RandomStringUtils.randomAlphabetic(Random.nextInt(4) + 1)}] " +
                    chosenMessage.toCharArray().joinToString("") {
                        if (Random.nextBoolean()) it.uppercase() else it.lowercase()
                    }
            })

            if (text.length > 256) {
                chat("Spammer message is too long! (Max 256 characters)")
                return@repeatable
            }

            // Check if message text is command
            if (text.startsWith("/")) {
                network.sendCommand(text.substring(1))
            } else {
                network.sendChatMessage(text)
            }
        }

        waitSeconds(delay.random()) // Delay in seconds (20 ticks per second)
    }

    private fun format(text: String): String {
        var formattedText = text

        while (formattedText.contains("%f"))
            formattedText = formattedText.insert("%f", Random.nextFloat())
        while (formattedText.contains("%i"))
            formattedText = formattedText.insert("%i", Random.nextInt(10000))
        while (formattedText.contains("%s"))
            formattedText = formattedText.insert("%s", RandomStringUtils.randomAlphabetic((4..6).random()))

        if (formattedText.contains("@a")) {
            val playerList = mc.networkHandler?.playerList?.filter {
                it?.profile?.name != player.gameProfile?.name
            }

            if (!playerList.isNullOrEmpty()) {
                while (formattedText.contains("@a")) {
                    formattedText = formattedText.insert("@a",
                        playerList.randomOrNull()?.profile?.name ?: break)
                }
            }
        }

        return formattedText
    }

    private fun String.insert(prefix: String, insert: Any): String {
        return substring(0, indexOf(prefix)) +
            insert.toString() + substring(indexOf(prefix) + prefix.length)
    }

    enum class MessageConverterMode(override val choiceName: String, val convert: (String) -> String) : NamedChoice {
        NO_CONVERTER("None", { text ->
            text
        }),
        LEET_CONVERTER("Leet", { text ->
            text.map { char ->
                when (char) {
                    'o' -> '0'
                    'l' -> '1'
                    'e' -> '3'
                    'a' -> '4'
                    't' -> '7'
                    's' -> 'Z'
                    else -> char
                }
            }.joinToString("")
        }),
        RANDOM_CASE_CONVERTER("Random Case", { text ->
            // Random case the whole string
            text.map { char ->
                if (Random.nextBoolean()) char.uppercase() else char.lowercase()
            }.joinToString("")
        }),
        RANDOM_SPACE_CONVERTER("Random Space", { text ->
            text.map { char ->
                if (Random.nextBoolean()) "$char " else char.toString()
            }.joinToString("")
        }),
    }

    enum class SpammerPattern(override val choiceName: String) : NamedChoice {
        RANDOM("Random"),
        LINEAR("Linear"),
    }

}
