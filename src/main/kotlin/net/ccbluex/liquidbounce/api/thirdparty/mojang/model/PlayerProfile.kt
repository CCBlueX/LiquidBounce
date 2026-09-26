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

import com.google.gson.annotations.SerializedName

@JvmRecord
data class PlayerProfile(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("skins") val skins: List<SkinInfo> = emptyList(),
    @SerializedName("capes") val capes: List<CapeInfo> = emptyList(),
)

@JvmRecord
data class SkinInfo(
    @SerializedName("id") val id: String,
    @SerializedName("state") val state: String,
    @SerializedName("url") val url: String,
    @SerializedName("variant") val variant: String,
)

@JvmRecord
data class CapeInfo(
    @SerializedName("id") val id: String,
    @SerializedName("state") val state: String,
    @SerializedName("url") val url: String,
    @SerializedName("alias") val alias: String,
)
