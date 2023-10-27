/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2023 CCBlueX
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

import net.ccbluex.liquidbounce.event.repeatable
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import org.apache.commons.lang3.RandomStringUtils
import kotlin.random.Random

/**
 * Spammer module
 *
 * Spams the chat with a given message.
 */
object ModuleSpammer : Module("Spammer", Category.MISC) {

    private val delay by intRange("Delay", 12..14, 0..300)
    private val message by text("Message",
        "LiquidBounce Nextgen | CCBlueX on [youtube] | liquidbounce{.net}")
        .doNotInclude()
    // todo: add back when textArray is supported
    // private val messages by textArray(
    //    "Messages",
    //    mutableListOf(
    //        "LiquidBounce Nextgen | CCBlueX on [youtube] | liquidbounce{.net}",
    //        "LiquidBounce: FREE and OPEN-SOURCE & 100% CUSTOMIZABLE"
    //    )
    //)
    private val customFormatter by boolean("CustomFormatter", false)
        .doNotInclude()

    val repeatable = repeatable {
        val text = if (customFormatter) {
            format(message)
        } else {
            "[${RandomStringUtils.randomAlphabetic(Random.nextInt(4) + 1)}] " +
                message.toCharArray().joinToString("") {
                    if (Random.nextBoolean()) it.uppercase() else it.lowercase()
                }
        }

        // Check if message text is command
        if (text.startsWith("/")) {
            network.sendCommand(text.substring(1))
        } else {
            network.sendChatMessage(text)
        }
        waitSeconds(delay.random()) // Delay in seconds (20 ticks per second)
    }

    private fun format(text: String): String {
        var formattedText = text

        while (formattedText.contains("%f"))
            formattedText = formattedText.substring(0, formattedText.indexOf("%f")) + Random.nextFloat() + formattedText.substring(formattedText.indexOf("%f") + "%f".length)
        while (formattedText.contains("%i"))
            formattedText = formattedText.substring(0, formattedText.indexOf("%i")) + Random.nextInt(10000) + formattedText.substring(formattedText.indexOf("%i") + "%i".length)
        while (formattedText.contains("%s"))
            formattedText = formattedText.substring(0, formattedText.indexOf("%s")) + RandomStringUtils.randomAlphabetic(Random.nextInt(8) + 1).toString() + formattedText.substring(formattedText.indexOf("%s") + "%s".length)
        while (formattedText.contains("%ss"))
            formattedText = formattedText.substring(0, formattedText.indexOf("%ss")) + RandomStringUtils.randomAlphabetic(Random.nextInt(8) + 1).toString() + formattedText.substring(formattedText.indexOf("%ss") + "%ss".length)
        while (formattedText.contains("%ls"))
            formattedText = formattedText.substring(0, formattedText.indexOf("%ls")) + RandomStringUtils.randomAlphabetic(Random.nextInt(8) + 1).toString() + formattedText.substring(formattedText.indexOf("%ls") + "%ls".length)

        return formattedText
    }
}
