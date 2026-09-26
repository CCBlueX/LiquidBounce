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

package net.ccbluex.liquidbounce.api.thirdparty.mojang.model

import com.google.gson.JsonElement
import com.google.gson.JsonNull
import com.google.gson.annotations.SerializedName

@JvmRecord
data class PresenceRequest(
    @SerializedName("status") val status: String,
    @SerializedName("joinInfo") val joinInfo: JoinInfo? = null,
)

@JvmRecord
data class JoinInfo(
    @SerializedName("value") val value: JsonElement = JsonNull.INSTANCE,
    @SerializedName("invites") val invites: List<String>? = null,
)

@JvmRecord
data class PresenceResponse(
    @SerializedName("presence") val presence: List<FriendPresence> = emptyList(),
)

@JvmRecord
data class FriendPresence(
    @SerializedName("profileId") val profileId: String,
    @SerializedName("pmid") val pmid: String,
    @SerializedName("status") val status: String,
    @SerializedName("joinInfo") val joinInfo: FriendJoinInfo? = null,
    @SerializedName("lastUpdated") val lastUpdated: String,
)

@JvmRecord
data class FriendJoinInfo(
    @SerializedName("value") val value: String? = null,
    @SerializedName("invited") val invited: Boolean,
)
