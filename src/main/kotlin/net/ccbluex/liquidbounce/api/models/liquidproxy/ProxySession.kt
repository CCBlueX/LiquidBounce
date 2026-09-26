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
package net.ccbluex.liquidbounce.api.models.liquidproxy

import com.google.gson.annotations.SerializedName

/**
 * One connection to a Minecraft server through LiquidProxy.
 */
data class ProxySession(
    @SerializedName("conn_id")
    val connId: String,
    val username: String,
    /** The node, e.g. `fra-1`. Its prefix is the location code. */
    val node: String,
    @SerializedName("server_addr")
    val serverAddr: String,
    @SerializedName("first_seen")
    val firstSeen: String,
    @SerializedName("last_seen")
    val lastSeen: String,
    val connected: Boolean,
    val error: String?,
    @SerializedName("ip_type")
    val ipType: String?,
    /** Country of the exit IP */
    val country: String,
)
