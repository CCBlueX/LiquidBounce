/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2026 CCBlueX
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

import net.ccbluex.liquidbounce.config.types.list.Tagged

/**
 * The service a [MinecraftAccount] authenticates against.
 */
enum class AccountService(
    override val tag: String,
    val canJoinOnline: Boolean,
    /**
     * Name this account type is persisted under in the accounts config.
     *
     * Historically the simple class name of the [MinecraftAccount] subclass, and kept that way so
     * that configs written by older versions keep loading.
     */
    val serialName: String,
) : Tagged {
    MICROSOFT("Microsoft", true, "MicrosoftAccount"),
    SESSION("Session", true, "SessionAccount"),
    THEALTENING("TheAltening", true, "AlteningAccount"),
    CRACKED("Cracked", false, "CrackedAccount");

    companion object {
        fun bySerialName(serialName: String) = entries.firstOrNull { it.serialName == serialName }
    }

}
