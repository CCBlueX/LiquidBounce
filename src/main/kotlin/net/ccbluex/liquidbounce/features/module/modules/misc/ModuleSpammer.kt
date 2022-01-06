/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2016 - 2022 CCBlueX
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

    private val delay by intRange("Delay", 12..14, 0..20)
    private val messages by textArray(
        "Messages",
        mutableListOf(
            "LiquidBounce Nextgen | CCBlueX on [youtube] | liquidbounce{.net}",
            "LiquidBounce: FREE and OPEN-SOURCE & 100% CUSTOMIZABLE"
        )
    )
    private val customFormatter by boolean("CustomFormatter", false)

    val repeatable = repeatable {
        val text = if (customFormatter) {
            format(messages.random())
        } else {
            "[${RandomStringUtils.randomAlphabetic(Random.nextInt(4) + 1)}] " + messages.random().toCharArray()
                .map { if (Random.nextBoolean()) it.toUpperCase() else it.toLowerCase() }.joinToString("")
        }

        player.sendChatMessage(text)
        wait(delay.random())
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
