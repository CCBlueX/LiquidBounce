/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2016 - 2020 CCBlueX
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
import net.minecraft.text.Text
import org.apache.logging.log4j.Logger


val logger: Logger
    get() = LiquidBounce.logger
private const val clientPrefix = "§8[§9§l${LiquidBounce.CLIENT_NAME}§8] §3"

fun chat(message: String) {
    displayChatMessage(clientPrefix + message)
}

private fun displayChatMessage(message: String) {
    if (mc.player == null) {
        logger.info("(MCChat)$message")
        return
    }

    mc.inGameHud.chatHud.addMessage(Text.of(message))
}