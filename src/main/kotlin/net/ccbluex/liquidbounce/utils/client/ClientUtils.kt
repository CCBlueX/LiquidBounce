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
package net.ccbluex.liquidbounce.utils.client

import net.ccbluex.liquidbounce.LiquidBounce
import net.minecraft.client.MinecraftClient
import net.minecraft.util.Util
import org.apache.logging.log4j.Logger

val logger: Logger
    get() = LiquidBounce.logger

val inGame: Boolean
    get() = MinecraftClient.getInstance()?.let { mc -> mc.player != null && mc.world != null } == true

/**
 * Open uri in browser
 */
fun browseUrl(url: String) = Util.getOperatingSystem().open(url)
