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

package net.ccbluex.liquidbounce.utils.client

import de.florianmichael.vialoadingbase.ViaLoadingBase
import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.event.EventManager
import net.ccbluex.liquidbounce.event.NotificationEvent
import net.minecraft.client.MinecraftClient
import net.minecraft.client.util.InputUtil
import net.minecraft.text.MutableText
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import net.minecraft.util.Util
import org.apache.logging.log4j.Logger
import org.lwjgl.glfw.GLFW

/**
 * Get minecraft instance
 */
val mc = MinecraftClient.getInstance()!!

val logger: Logger
    get() = LiquidBounce.logger

/**
 * Get current protocol version
 *
 * @return protocol version
 */
val protocolVersion: Int
    get() = runCatching {
        ViaLoadingBase.getInstance().targetVersion.index
    }.getOrElse { MC_1_19_4 }

const val MC_1_19_4: Int = 762
const val MC_1_8: Int = 47

// Chat formatting
private val clientPrefix = "§f§lLiquid§9§lBounce §8▸ §7".asText()

fun dot() = regular(".")

fun regular(text: MutableText) = text.styled { it.withColor(Formatting.GRAY) }

fun regular(text: String) = text.asText().styled { it.withColor(Formatting.GRAY) }

fun variable(text: MutableText) = text.styled { it.withColor(Formatting.DARK_GRAY) }

fun variable(text: String) = text.asText().styled { it.withColor(Formatting.DARK_GRAY) }

fun chat(vararg texts: Text, prefix: Boolean = true) {
    val literalText = if (prefix) clientPrefix.copy() else Text.literal("")
    texts.forEach { literalText.append(it) }

    if (mc.player == null) {
        logger.info("(Chat) ${literalText.outputString()}")
        return
    }

    mc.inGameHud.chatHud.addMessage(literalText)
}

fun chat(text: String) = chat(text.asText())

fun notification(title: Text, message: String, severity: NotificationEvent.Severity) =
    EventManager.callEvent(NotificationEvent(title.string, message, severity))

fun notification(title: String, message: Text, severity: NotificationEvent.Severity) =
    EventManager.callEvent(NotificationEvent(title, message.string, severity))

fun notification(title: String, message: String, severity: NotificationEvent.Severity) =
    EventManager.callEvent(NotificationEvent(title, message, severity))

/**
 * Translated key code to key name using GLFW and translates unknown key to NONE
 */
fun key(name: String) = when (name.toLowerCase()) {
    "rshift" -> GLFW.GLFW_KEY_RIGHT_SHIFT
    "lshift" -> GLFW.GLFW_KEY_LEFT_SHIFT
    else -> runCatching {
        InputUtil.fromTranslationKey("key.keyboard.${name.toLowerCase()}").code
    }.getOrElse { GLFW.GLFW_KEY_UNKNOWN }
}

/**
 * Translated key code to key name using GLFW and translates unknown key to NONE
 */
fun keyName(keyCode: Int) = when (keyCode) {
    GLFW.GLFW_KEY_UNKNOWN -> "NONE"
    else -> InputUtil.fromKeyCode(keyCode, -1).translationKey
        .split(".")
        .drop(2)
        .joinToString(separator = "_")
        .uppercase()
}

/**
 * Open uri in browser
 */
fun browseUrl(url: String) = Util.getOperatingSystem().open(url)
