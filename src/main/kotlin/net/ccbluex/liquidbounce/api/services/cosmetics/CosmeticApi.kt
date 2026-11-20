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
package net.ccbluex.liquidbounce.api.services.cosmetics

import com.mojang.blaze3d.platform.NativeImage
import net.ccbluex.liquidbounce.api.core.ApiConfig.Companion.config
import net.ccbluex.liquidbounce.api.models.cosmetics.Carrier
import net.ccbluex.liquidbounce.api.models.cosmetics.CarrierHashes
import retrofit2.create
import retrofit2.http.GET
import retrofit2.http.Path
import java.util.UUID

object CosmeticApi {

    private val service = config.retrofit.create<Service>()

    suspend fun getCarriers() = service.carriers().hashes

    suspend fun getCarrierCosmetics(uuid: UUID) = service.carrier(uuid.toString()).cosmetics

    /**
     * The texture of a cape or an elytra.
     */
    suspend fun getCape(style: String) = service.cape(style)

    suspend fun getEars(style: String) = service.ears(style)

    internal interface Service {
        @GET("cosmetics/carriers")
        suspend fun carriers(): CarrierHashes

        @GET("cosmetics/carriers/{uuid}")
        suspend fun carrier(@Path("uuid") uuid: String): Carrier

        @GET("cosmetics/capes/{style}")
        suspend fun cape(@Path("style") style: String): NativeImage

        @GET("cosmetics/ears/{style}")
        suspend fun ears(@Path("style") style: String): NativeImage
    }

}
