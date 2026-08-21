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

package net.ccbluex.liquidbounce.api.thirdparty.mojang.service

import net.ccbluex.liquidbounce.api.thirdparty.mojang.model.*
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*

/**
 * Endpoints on [api.minecraftservices.com](https://api.minecraftservices.com).
 * Most endpoints require `Authorization: Bearer <token>` header.
 *
 * The authentication endpoints of this host (`login_with_xbox`, `entitlements/license`,
 * `player/certificates`) are deliberately absent - MinecraftAuth already implements them as
 * `MinecraftLauncherLoginRequest`, `MinecraftEntitlementsRequest` and
 * `MinecraftPlayerCertificatesRequest`, driven by `JavaAuthManager`.
 */
@Suppress("TooManyFunctions")
interface MinecraftServicesApi {

    // ── Profile lookup (no auth) ──

    @GET("minecraft/profile/lookup/name/{username}")
    suspend fun lookupUuidByName(@Path("username") username: String): ProfileIdName

    @GET("minecraft/profile/lookup/{uuid}")
    suspend fun lookupNameByUuid(@Path("uuid") uuid: String): ProfileIdName

    @POST("minecraft/profile/lookup/bulk/byname")
    suspend fun lookupProfilesBulk(@Body names: List<String>): List<ProfileIdName>

    // ── Player profile & attributes ──

    @GET("minecraft/profile")
    suspend fun fetchProfile(): PlayerProfile

    @GET("player/attributes")
    suspend fun fetchAttributes(): PlayerAttributes

    @POST("player/attributes")
    suspend fun updateAttributes(@Body update: PlayerAttributesUpdate): PlayerAttributes

    // ── Privacy ──

    @GET("privacy/blocklist")
    suspend fun getBlockList(): BlockList

    // ── Name management ──

    @GET("minecraft/profile/namechange")
    suspend fun getNameChangeInfo(): NameChangeInfo

    @GET("minecraft/profile/name/{name}/available")
    suspend fun checkNameAvailability(@Path("name") name: String): NameAvailability

    @PUT("minecraft/profile/name/{name}")
    suspend fun changeName(@Path("name") newName: String): PlayerProfile

    // ── Skin management ──

    @POST("minecraft/profile/skins")
    suspend fun changeSkin(@Body request: ChangeSkinRequest): PlayerProfile

    @Multipart
    @POST("minecraft/profile/skins")
    suspend fun uploadSkin(
        @Part("variant") variant: RequestBody,
        @Part file: MultipartBody.Part,
    ): PlayerProfile

    @DELETE("minecraft/profile/skins/active")
    suspend fun resetSkin(): PlayerProfile

    // ── Cape management ──

    @DELETE("minecraft/profile/capes/active")
    suspend fun hideCape(): PlayerProfile

    @PUT("minecraft/profile/capes/active")
    suspend fun showCape(@Body request: ActivateCapeRequest): PlayerProfile

    // ── Gift code ──

    @GET("productvoucher/giftcode")
    suspend fun checkGiftCode(): Response<Unit>

    // ── Friends ──

    @GET("friends")
    suspend fun getFriends(
        @Header("If-None-Match") etag: String? = null,
    ): Response<FriendsList>

    @PUT("friends")
    suspend fun updateFriend(@Body request: FriendsUpdateRequest): Response<FriendsList>

    // ── Presence ──

    @POST("presence")
    suspend fun reportPresence(@Body request: PresenceRequest): PresenceResponse

    // ── Public keys ──

    @GET("publickeys")
    suspend fun getPublicKeys(): PublicKeys
}
