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
import net.ccbluex.liquidbounce.api.models.marketplace.MarketplaceConfigReportSummary
import net.ccbluex.liquidbounce.api.models.marketplace.MarketplaceItem
import net.ccbluex.liquidbounce.api.models.marketplace.MarketplaceItemRevision
import net.ccbluex.liquidbounce.api.models.marketplace.MarketplaceLinkedItem
import net.ccbluex.liquidbounce.api.models.marketplace.MarketplaceReview
import net.ccbluex.liquidbounce.api.models.marketplace.MarketplaceRevisionDependency
import net.ccbluex.liquidbounce.api.models.marketplace.MarketplaceTag
import net.ccbluex.liquidbounce.api.models.pagination.PaginatedResponse
import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.HTTP
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.QueryMap

/**
 * Endpoints of the marketplace API.
 */
@Suppress("TooManyFunctions")
internal interface MarketplaceService {

    // Items
    @GET("marketplace")
    suspend fun items(
        @QueryMap query: Map<String, String>,
        @Header("Authorization") authorization: String? = null,
    ): PaginatedResponse<MarketplaceItem>

    @GET("marketplace/tags")
    suspend fun tags(): List<MarketplaceTag>

    @POST("marketplace")
    suspend fun createItem(
        @Body body: RequestBody,
        @Header("Authorization") authorization: String,
    ): MarketplaceItem

    @PATCH("marketplace/{id}")
    suspend fun updateItem(
        @Path("id") id: Int,
        @Body body: RequestBody,
        @Header("Authorization") authorization: String,
    ): MarketplaceItem

    @DELETE("marketplace/{id}")
    suspend fun deleteItem(
        @Path("id") id: Int,
        @Header("Authorization") authorization: String,
    )

    @GET("marketplace/{id}")
    suspend fun item(
        @Path("id") id: Int,
        @Header("Authorization") authorization: String? = null,
    ): MarketplaceItem

    @GET("marketplace/code/{code}")
    suspend fun itemByCode(@Path("code") code: String): MarketplaceItem

    // Revisions
    @GET("marketplace/{id}/revisions")
    suspend fun revisions(
        @Path("id") id: Int,
        @Query("page") page: Int,
        @Query("limit") limit: Int,
        @Query("minecraft") minecraft: String? = null,
        @Query("liquidbounce") liquidbounce: String? = null,
    ): PaginatedResponse<MarketplaceItemRevision>

    @GET("marketplace/{id}/revisions/{revisionId}")
    suspend fun revision(
        @Path("id") id: Int,
        @Path("revisionId") revisionId: Int,
    ): MarketplaceItemRevision

    @POST("marketplace/{id}/revisions")
    suspend fun createRevision(
        @Path("id") id: Int,
        @Body body: RequestBody,
        @Header("Authorization") authorization: String,
    ): MarketplaceItemRevision

    @DELETE("marketplace/{id}/revisions/{revisionId}")
    suspend fun deleteRevision(
        @Path("id") id: Int,
        @Path("revisionId") revisionId: Int,
        @Header("Authorization") authorization: String,
    )

    // Dependencies
    @GET("marketplace/{id}/dependencies")
    suspend fun dependencies(@Path("id") id: Int): List<MarketplaceLinkedItem>

    @POST("marketplace/{id}/dependencies")
    suspend fun addDependency(
        @Path("id") id: Int,
        @Body body: RequestBody,
        @Header("Authorization") authorization: String,
    )

    @DELETE("marketplace/{id}/dependencies/{dependencyItemId}")
    suspend fun removeDependency(
        @Path("id") id: Int,
        @Path("dependencyItemId") dependencyItemId: Int,
        @Header("Authorization") authorization: String,
    )

    @GET("marketplace/{id}/revisions/{revisionId}/dependencies")
    suspend fun revisionDependencies(
        @Path("id") id: Int,
        @Path("revisionId") revisionId: Int,
    ): List<MarketplaceRevisionDependency>

    @POST("marketplace/{id}/revisions/{revisionId}/dependencies")
    suspend fun addRevisionDependency(
        @Path("id") id: Int,
        @Path("revisionId") revisionId: Int,
        @Body body: RequestBody,
        @Header("Authorization") authorization: String,
    )

    @HTTP(method = "DELETE", path = "marketplace/{id}/revisions/{revisionId}/dependencies", hasBody = true)
    suspend fun removeRevisionDependency(
        @Path("id") id: Int,
        @Path("revisionId") revisionId: Int,
        @Body body: RequestBody,
        @Header("Authorization") authorization: String,
    )

    // Reviews
    @GET("marketplace/{id}/reviews")
    suspend fun reviews(
        @Path("id") id: Int,
        @Query("page") page: Int,
        @Query("limit") limit: Int,
    ): PaginatedResponse<MarketplaceReview>

    @POST("marketplace/{id}/reviews")
    suspend fun createReview(
        @Path("id") id: Int,
        @Body body: RequestBody,
        @Header("Authorization") authorization: String,
    ): MarketplaceReview

    @DELETE("marketplace/{id}/reviews/{reviewId}")
    suspend fun deleteReview(
        @Path("id") id: Int,
        @Path("reviewId") reviewId: Int,
        @Header("Authorization") authorization: String,
    )

    // Config reports
    @PUT("marketplace/{id}/revisions/{revisionId}/report")
    suspend fun putConfigReport(
        @Path("id") id: Int,
        @Path("revisionId") revisionId: Int,
        @Body body: RequestBody,
        @Header("Authorization") authorization: String,
    ): MarketplaceConfigReport

    @GET("marketplace/{id}/revisions/{revisionId}/report")
    suspend fun configReport(
        @Path("id") id: Int,
        @Path("revisionId") revisionId: Int,
        @Header("Authorization") authorization: String,
    ): MarketplaceConfigReport

    @DELETE("marketplace/{id}/revisions/{revisionId}/report")
    suspend fun deleteConfigReport(
        @Path("id") id: Int,
        @Path("revisionId") revisionId: Int,
        @Header("Authorization") authorization: String,
    )

    @GET("marketplace/{id}/reports")
    suspend fun configReports(@Path("id") id: Int): List<MarketplaceConfigReportSummary>

    // Thumbnails
    @POST("marketplace/{id}/thumbnail")
    suspend fun uploadThumbnail(
        @Path("id") id: Int,
        @Body body: RequestBody,
        @Header("Authorization") authorization: String,
    ): MarketplaceItem

    @DELETE("marketplace/{id}/thumbnail")
    suspend fun deleteThumbnail(
        @Path("id") id: Int,
        @Header("Authorization") authorization: String,
    )
}
