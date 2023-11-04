/*
 *
 *  * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *  *
 *  * Copyright (c) 2015 - 2023 CCBlueX
 *  *
 *  * LiquidBounce is free software: you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License as published by
 *  * the Free Software Foundation, either version 3 of the License, or
 *  * (at your option) any later version.
 *  *
 *  * LiquidBounce is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  * GNU General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License
 *  * along with LiquidBounce. If not, see <https://www.gnu.org/licenses/>.
 *
 */

package net.ccbluex.liquidbounce.utils.client

import net.raphimc.vialoader.util.VersionEnum

// Only runs once
val usesViaFabricPlus = runCatching {
    Class.forName("de.florianmichael.viafabricplus.protocolhack.ProtocolHack")
    true
}.getOrDefault(false)

val isOldCombat: Boolean
    get() = runCatching {
        // Check if the ViaFabricPlus mod is loaded - prevents from causing too many exceptions
        if (!usesViaFabricPlus) {
            return@runCatching false
        }

        val clazz = Class.forName("de.florianmichael.viafabricplus.protocolhack.ProtocolHack")
        val method = clazz.getMethod("getTargetVersion")
        val version = method.invoke(null) as VersionEnum

        // Check if the version is older or equal than 1.8
        return version.isOlderThanOrEqualTo(VersionEnum.r1_8)
    }.onFailure {
        logger.error("Failed to check if the server is using old combat", it)
    }.getOrDefault(false)
