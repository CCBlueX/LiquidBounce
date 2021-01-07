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
import net.ccbluex.liquidbounce.utils.extensions.asText
import net.minecraft.client.MinecraftClient
import org.apache.logging.log4j.Logger

val mc = MinecraftClient.getInstance()!!

val logger: Logger
    get() = LiquidBounce.logger

// Chat formatting
const val defaultColor = "§3"
const val variableColor = "§7"
const val statusColor = "§5"
private const val clientPrefix = "§8[§9§l${LiquidBounce.CLIENT_NAME}§8] $defaultColor"

fun chat(message: String) {
    if (mc.player == null) {
        logger.info("(Chat) $message")
        return
    }

    mc.inGameHud.chatHud.addMessage("$clientPrefix$message".asText())
}
