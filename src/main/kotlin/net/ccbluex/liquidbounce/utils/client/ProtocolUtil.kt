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

import de.florianmichael.viafabricplus.protocolhack.ProtocolHack
import net.raphimc.vialoader.util.VersionEnum

// Only runs once
val usesViaFabricPlus = runCatching {
    Class.forName("de.florianmichael.viafabricplus.protocolhack.ProtocolHack")
    true
}.getOrDefault(false)

val isOldCombat: Boolean
    get() = runCatching {
        if (!usesViaFabricPlus) {
            return@runCatching false
        }

        // Throws an exception if the protocol hack is not loaded - but this should never happen
        ProtocolHack.getTargetVersion().isOlderThanOrEqualTo(VersionEnum.r1_8)
    }.getOrDefault(false)
