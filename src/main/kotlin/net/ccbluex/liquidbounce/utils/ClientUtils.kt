/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2016 - 2021 CCBlueX
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
package net.ccbluex.liquidbounce.utils

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.utils.extensions.asText
import net.ccbluex.liquidbounce.utils.extensions.outputString
import net.minecraft.client.MinecraftClient
import net.minecraft.text.LiteralText
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import org.apache.logging.log4j.Logger

val mc = MinecraftClient.getInstance()!!

val logger: Logger
    get() = LiquidBounce.logger

// Chat formatting
private val clientPrefix = "§8[§9§l${LiquidBounce.CLIENT_NAME}§8] ".asText()

fun dot() = regular(".")

fun regular(text: String) = text.asText().styled { it.withColor(Formatting.GRAY) }

fun variable(text: String) = text.asText().styled { it.withColor(Formatting.DARK_GRAY) }

fun chat(vararg texts: Text, prefix: Boolean = true) {
    val literalText = if (prefix) clientPrefix.copy() else LiteralText("")
    texts.forEach { literalText.append(it) }

    if (mc.player == null) {
        logger.info("(Chat) ${literalText.outputString()}")
        return
    }

    mc.inGameHud.chatHud.addMessage(literalText)
}

fun chat(text: String) = chat(text.asText())

/**
 * Converts a resource to string
 *
 * @param path The *absolute* resource path
 * @throws IllegalArgumentException If the path is invalid
 */
fun resourceToString(path: String): String {
    class Empty

    val resourceAsStream =
        Empty::class.java.getResourceAsStream(path) ?: throw IllegalArgumentException("Resource $path not found")

    return resourceAsStream.use { it.reader().readText() }
}
