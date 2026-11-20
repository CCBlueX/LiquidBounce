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
package net.ccbluex.liquidbounce.api.services.marketplace

import net.ccbluex.liquidbounce.api.models.marketplace.MarketplaceConfigReport
import net.ccbluex.liquidbounce.api.models.marketplace.MarketplaceItem
import net.ccbluex.liquidbounce.api.models.marketplace.MarketplaceItemRevision
import net.ccbluex.liquidbounce.api.models.marketplace.MarketplaceReview
import net.ccbluex.liquidbounce.api.models.marketplace.MarketplaceTag
import net.ccbluex.liquidbounce.api.models.pagination.PaginatedResponse
import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.QueryMap

@Suppress("TooManyFunctions")
internal interface MarketplaceService {

    @GET("marketplace/items")
    suspend fun items(@QueryMap query: Map<String, String>): PaginatedResponse<MarketplaceItem>

    @GET("marketplace/tags")
    suspend fun tags(): List<MarketplaceTag>

    @POST("marketplace/items")
    suspend fun createItem(
        @Body body: RequestBody,
        @Header("Authorization") authorization: String,
    ): MarketplaceItem

    @PATCH("marketplace/items/{id}")
    suspend fun updateItem(
        @Path("id") id: Int,
        @Body body: RequestBody,
        @Header("Authorization") authorization: String,
    ): MarketplaceItem

    @DELETE("marketplace/items/{id}")
    suspend fun deleteItem(
        @Path("id") id: Int,
        @Header("Authorization") authorization: String,
    )

    @GET("marketplace/items/{id}")
    suspend fun item(
        @Path("id") id: Int,
        @QueryMap game: Map<String, String>,
    ): MarketplaceItem

    @GET("marketplace/share-codes/{code}")
    suspend fun itemByCode(
        @Path("code") code: String,
        @QueryMap game: Map<String, String>,
    ): MarketplaceItem

    @GET("me/marketplace/items/{id}")
    suspend fun ownItem(
        @Path("id") id: Int,
        @Header("Authorization") authorization: String,
    ): MarketplaceItem

    @GET("marketplace/items/{id}/revisions")
    suspend fun revisions(
        @Path("id") id: Int,
        @Query("limit") limit: Int,
        @Query("offset") offset: Int,
        @Query("minecraft") minecraft: String? = null,
        @Query("liquidbounce") liquidbounce: String? = null,
    ): PaginatedResponse<MarketplaceItemRevision>

    @GET("marketplace/items/{id}/revisions/{revisionId}")
    suspend fun revision(
        @Path("id") id: Int,
        @Path("revisionId") revisionId: Int,
    ): MarketplaceItemRevision

    @POST("marketplace/items/{id}/revisions")
    suspend fun createRevision(
        @Path("id") id: Int,
        @Body body: RequestBody,
        @Header("Authorization") authorization: String,
    ): MarketplaceItemRevision

    @DELETE("marketplace/items/{id}/revisions/{revisionId}")
    suspend fun deleteRevision(
        @Path("id") id: Int,
        @Path("revisionId") revisionId: Int,
        @Header("Authorization") authorization: String,
    )

    @PUT("marketplace/items/{id}/dependencies/{dependencyId}")
    suspend fun addDependency(
        @Path("id") id: Int,
        @Path("dependencyId") dependencyId: Int,
        @Header("Authorization") authorization: String,
    )

    @DELETE("marketplace/items/{id}/dependencies/{dependencyId}")
    suspend fun removeDependency(
        @Path("id") id: Int,
        @Path("dependencyId") dependencyId: Int,
        @Header("Authorization") authorization: String,
    )

    @GET("marketplace/items/{id}/reviews")
    suspend fun reviews(
        @Path("id") id: Int,
        @Query("limit") limit: Int,
        @Query("offset") offset: Int,
    ): PaginatedResponse<MarketplaceReview>

    @POST("marketplace/items/{id}/reviews")
    suspend fun createReview(
        @Path("id") id: Int,
        @Body body: RequestBody,
        @Header("Authorization") authorization: String,
    ): MarketplaceReview

    @DELETE("marketplace/items/{id}/reviews/{reviewId}")
    suspend fun deleteReview(
        @Path("id") id: Int,
        @Path("reviewId") reviewId: Int,
        @Header("Authorization") authorization: String,
    )

    @PUT("me/marketplace/reports/{revisionId}")
    suspend fun putConfigReport(
        @Path("revisionId") revisionId: Int,
        @Body body: RequestBody,
        @Header("Authorization") authorization: String,
    ): MarketplaceConfigReport

    @GET("me/marketplace/reports/{revisionId}")
    suspend fun configReport(
        @Path("revisionId") revisionId: Int,
        @Header("Authorization") authorization: String,
    ): MarketplaceConfigReport

    @DELETE("me/marketplace/reports/{revisionId}")
    suspend fun deleteConfigReport(
        @Path("revisionId") revisionId: Int,
        @Header("Authorization") authorization: String,
    )

    @PUT("marketplace/items/{id}/thumbnail")
    suspend fun setThumbnail(
        @Path("id") id: Int,
        @Body body: RequestBody,
        @Header("Authorization") authorization: String,
    ): MarketplaceItem

    @DELETE("marketplace/items/{id}/thumbnail")
    suspend fun deleteThumbnail(
        @Path("id") id: Int,
        @Header("Authorization") authorization: String,
    )
}
