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

package net.ccbluex.liquidbounce.features.account

import net.ccbluex.liquidbounce.authlib.account.*
import net.ccbluex.liquidbounce.config.types.NamedChoice

enum class AccountService(override val choiceName: String, val canJoinOnline: Boolean) : NamedChoice {
    MICROSOFT("Microsoft", true),
    SESSION("Session", true),
    THEALTENING("TheAltening", true),
    CRACKED("Cracked", false);

    companion object {
        fun getService(account: MinecraftAccount) = when (account) {
            is MicrosoftAccount -> MICROSOFT
            is SessionAccount -> SESSION
            is AlteningAccount -> THEALTENING
            is CrackedAccount -> CRACKED
            else -> throw IllegalArgumentException("Unknown account type: ${account::class.java.name}")
        }
    }

}
