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
package net.ccbluex.liquidbounce.features.chat.packet

import com.google.gson.annotations.SerializedName
import java.util.UUID

sealed interface AxochatPacket {
    sealed interface C2S : AxochatPacket
    sealed interface S2C : AxochatPacket

    annotation class Metadata(val name: String)
}

/**
 * A axochat user
 *
 * @param name of user
 * @param uuid of user
 */
@JvmRecord
data class AxoUser(
    @SerializedName("name")
    val name: String,
    @SerializedName("uuid")
    val uuid: UUID,
)
