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

import com.google.gson.JsonObject
import net.ccbluex.liquidbounce.api.core.ApiConfig.Companion.API_BRANCH
import net.ccbluex.liquidbounce.api.core.ApiConfig.Companion.config
import net.ccbluex.liquidbounce.api.core.BaseApi
import net.ccbluex.liquidbounce.api.core.HttpClient
import net.ccbluex.liquidbounce.api.models.auth.OAuthSession
import net.ccbluex.liquidbounce.api.models.auth.addAuth
import net.ccbluex.liquidbounce.api.models.marketplace.MarketplaceConfigReport
import net.ccbluex.liquidbounce.api.models.marketplace.MarketplaceConfigReportSummary
import net.ccbluex.liquidbounce.api.models.marketplace.MarketplaceItem
import net.ccbluex.liquidbounce.api.models.marketplace.MarketplaceItemRevision
import net.ccbluex.liquidbounce.api.models.marketplace.MarketplaceItemType
import net.ccbluex.liquidbounce.api.models.marketplace.MarketplaceItemVisibility
import net.ccbluex.liquidbounce.api.models.marketplace.MarketplaceLinkedItem
import net.ccbluex.liquidbounce.api.models.marketplace.MarketplaceReview
import net.ccbluex.liquidbounce.api.models.marketplace.MarketplaceRevisionDependency
import net.ccbluex.liquidbounce.api.models.marketplace.MarketplaceTag
import net.ccbluex.liquidbounce.api.models.pagination.PaginatedResponse
import net.ccbluex.liquidbounce.api.core.toRequestBody
import net.ccbluex.liquidbounce.config.gson.publicGson
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.net.URLEncoder

@Suppress("TooManyFunctions")
object MarketplaceApi : BaseApi(config.apiEndpointV3) {

    private data class MarketplaceParams(
        val page: Int = 1,
        val limit: Int = 10,
        val query: String? = null,
        val type: MarketplaceItemType? = null,
        val featured: Boolean = false,
        val uid: String? = null,
        val branch: String? = API_BRANCH,
        val filter: Filter = Filter(),
    ) {
        fun buildQueryString() = buildString {
            append("?page=$page&limit=$limit")
            query?.let { append("&q=${it.urlEncoded()}") }
            type?.let { type -> append("&type=${publicGson.toJsonTree(type).asString}") }
            uid?.let { append("&uid=$it") }
            branch?.let { append("&branch=$branch") }
            append("&featured=$featured")
            filter.name?.let { append("&name=${it.urlEncoded()}") }
            filter.tags.takeIf { it.isNotEmpty() }?.let { append("&tags=${it.joinToString(",")}") }
            filter.targetServer?.let { append("&target_server=${it.urlEncoded()}") }
            filter.forkedFrom?.let { append("&forked_from=$it") }
            filter.sort?.let { append("&sort=${it.tag}") }
        }
    }

    enum class Sort(val tag: String) {
        CREATED("created"),
        SCORE("score")
    }

    data class Filter(
        val name: String? = null,
        val tags: List<Int> = emptyList(),
        val targetServer: String? = null,
        val forkedFrom: Int? = null,
        val sort: Sort? = null,
    )

    /**
     * Listing and config fields of an item. On update, `null` leaves a field unchanged.
     */
    data class ItemDetails(
        val branch: String? = null,
        val tags: List<Int>? = null,
        val targetServers: List<String>? = null,
        val visibility: MarketplaceItemVisibility? = null,
        val forkedFromItemId: Int? = null,
        val forkedFromRevisionId: Int? = null,
    ) {
        fun writeTo(json: JsonObject) = with(json) {
            branch?.let { addProperty("branch", it) }
            tags?.let { add("tags", publicGson.toJsonTree(it)) }
            targetServers?.let { add("target_servers", publicGson.toJsonTree(it)) }
            visibility?.let { add("visibility", publicGson.toJsonTree(it)) }
            forkedFromItemId?.let { addProperty("forked_from_item_id", it) }
            forkedFromRevisionId?.let { addProperty("forked_from_revision_id", it) }
        }
    }

    private fun String.urlEncoded(): String = URLEncoder.encode(this, Charsets.UTF_8)

    // Marketplace Items
    @Suppress("LongParameterList")
    suspend fun getMarketplaceItems(
        page: Int = 1,
        limit: Int = 10,
        query: String? = null,
        type: MarketplaceItemType? = null,
        featured: Boolean = false,
        uid: String? = null,
        branch: String? = null,
        filter: Filter = Filter(),
        session: OAuthSession? = null
    ): PaginatedResponse<MarketplaceItem> {
        val params = MarketplaceParams(page, limit, query, type, featured, uid, branch, filter)
        return get("/marketplace${params.buildQueryString()}", headers = { session?.let { addAuth(it) } })
    }

    suspend fun getTags() = get<List<MarketplaceTag>>("/marketplace/tags")

    suspend fun createMarketplaceItem(
        session: OAuthSession,
        name: String,
        type: MarketplaceItemType,
        description: String,
        details: ItemDetails = ItemDetails()
    ) = post<MarketplaceItem>(
        "/marketplace",
        JsonObject().apply {
            addProperty("name", name)
            add("type", publicGson.toJsonTree(type))
            addProperty("description", description)
            details.writeTo(this)
        }.toRequestBody(),
        headers = { addAuth(session) }
    )

    @Suppress("LongParameterList")
    suspend fun updateMarketplaceItem(
        session: OAuthSession,
        id: Int,
        name: String,
        type: MarketplaceItemType,
        description: String,
        details: ItemDetails = ItemDetails()
    ) = patch<MarketplaceItem>(
        "/marketplace/$id",
        JsonObject().apply {
            addProperty("name", name)
            add("type", publicGson.toJsonTree(type))
            addProperty("description", description)
            details.writeTo(this)
        }.toRequestBody(),
        headers = { addAuth(session) }
    )

    suspend fun deleteMarketplaceItem(session: OAuthSession, id: Int) =
        delete<Unit>("/marketplace/$id", headers = { addAuth(session) })

    suspend fun getMarketplaceItem(id: Int, session: OAuthSession? = null) =
        get<MarketplaceItem>("/marketplace/$id", headers = { session?.let { addAuth(it) } })

    suspend fun getMarketplaceItemByCode(shareCode: String) =
        get<MarketplaceItem>("/marketplace/code/${shareCode.urlEncoded()}")

    // Revisions
    suspend fun getMarketplaceItemRevisions(id: Int, page: Int = 1, limit: Int = 10) =
        get<PaginatedResponse<MarketplaceItemRevision>>("/marketplace/$id/revisions?page=$page&limit=$limit")

    suspend fun getMarketplaceItemRevision(id: Int, revisionId: Int) =
        get<MarketplaceItemRevision>("/marketplace/$id/revisions/$revisionId")

    @Suppress("LongParameterList")
    suspend fun createMarketplaceItemRevision(
        session: OAuthSession,
        id: Int,
        file: File,
        version: String,
        changelog: String? = null,
        dependencies: String? = null,
        includesBinds: Boolean? = null
    ): MarketplaceItemRevision {
        val multipartBuilder = MultipartBody.Builder().setType(MultipartBody.FORM)
            .addFormDataPart("file", file.name, file.asRequestBody(HttpClient.MediaTypes.OCTET_STREAM))
            .addFormDataPart("version", version)

        changelog?.let { multipartBuilder.addFormDataPart("changelog", it) }
        dependencies?.let { multipartBuilder.addFormDataPart("dependencies", it) }
        includesBinds?.let { multipartBuilder.addFormDataPart("includes_binds", it.toString()) }

        return post(
            "/marketplace/$id/revisions",
            multipartBuilder.build(),
            headers = { addAuth(session) }
        )
    }

    suspend fun deleteMarketplaceItemRevision(session: OAuthSession, id: Int, revisionId: Int) =
        delete<Unit>("/marketplace/$id/revisions/$revisionId", headers = { addAuth(session) })

    fun downloadRevision(id: Int, revisionId: Int) = "$baseUrl/marketplace/$id/revisions/$revisionId/download"

    // Dependencies
    suspend fun getItemDependencies(id: Int) =
        get<List<MarketplaceLinkedItem>>("/marketplace/$id/dependencies")

    suspend fun addItemDependency(session: OAuthSession, id: Int, dependencyItemId: Int) = post<Unit>(
        "/marketplace/$id/dependencies",
        JsonObject().apply {
            addProperty("dependency_item_id", dependencyItemId)
        }.toRequestBody(),
        headers = { addAuth(session) }
    )

    suspend fun removeItemDependency(session: OAuthSession, id: Int, dependencyItemId: Int) =
        delete<Unit>("/marketplace/$id/dependencies/$dependencyItemId", headers = { addAuth(session) })

    suspend fun getRevisionDependencies(id: Int, revisionId: Int) =
        get<List<MarketplaceRevisionDependency>>("/marketplace/$id/revisions/$revisionId/dependencies")

    suspend fun addRevisionDependency(
        session: OAuthSession,
        id: Int,
        revisionId: Int,
        dependencyRevisionId: Int
    ) = post<Unit>(
        "/marketplace/$id/revisions/$revisionId/dependencies",
        JsonObject().apply {
            addProperty("dependency_revision_id", dependencyRevisionId)
        }.toRequestBody(),
        headers = { addAuth(session) }
    )

    suspend fun removeRevisionDependency(
        session: OAuthSession,
        id: Int,
        revisionId: Int,
        dependencyRevisionId: Int
    ) = delete<Unit>(
        "/marketplace/$id/revisions/$revisionId/dependencies",
        JsonObject().apply {
            addProperty("dependency_revision_id", dependencyRevisionId)
        }.toRequestBody(),
        headers = { addAuth(session) }
    )

    // Reviews
    suspend fun getReviews(id: Int, page: Int = 1, limit: Int = 10) =
        get<PaginatedResponse<MarketplaceReview>>("/marketplace/$id/reviews?page=$page&limit=$limit")

    suspend fun createReview(
        session: OAuthSession,
        id: Int,
        rating: Int,
        review: String? = null
    ) = post<MarketplaceReview>(
        "/marketplace/$id/reviews",
        JsonObject().apply {
            addProperty("rating", rating)
            review?.let { addProperty("review", it) }
        }.toRequestBody(),
        headers = { addAuth(session) }
    )

    suspend fun deleteReview(session: OAuthSession, id: Int, reviewId: Int) =
        delete<Unit>("/marketplace/$id/reviews/$reviewId", headers = { addAuth(session) })

    // Config reports
    @Suppress("LongParameterList")
    suspend fun putConfigReport(
        session: OAuthSession,
        id: Int,
        revisionId: Int,
        works: Boolean,
        clientVersion: String? = null,
        serverAddress: String? = null
    ) = put<MarketplaceConfigReport>(
        "/marketplace/$id/revisions/$revisionId/report",
        JsonObject().apply {
            addProperty("works", works)
            clientVersion?.let { addProperty("client_version", it) }
            serverAddress?.let { addProperty("server_address", it) }
        }.toRequestBody(),
        headers = { addAuth(session) }
    )

    suspend fun getOwnConfigReport(session: OAuthSession, id: Int, revisionId: Int) =
        get<MarketplaceConfigReport>("/marketplace/$id/revisions/$revisionId/report", headers = { addAuth(session) })

    suspend fun deleteConfigReport(session: OAuthSession, id: Int, revisionId: Int) =
        delete<Unit>("/marketplace/$id/revisions/$revisionId/report", headers = { addAuth(session) })

    suspend fun getConfigReports(id: Int) =
        get<List<MarketplaceConfigReportSummary>>("/marketplace/$id/reports")

    // Thumbnails
    suspend fun uploadThumbnail(session: OAuthSession, id: Int, thumbnailFile: File) {
        val requestBody = MultipartBody.Builder().setType(MultipartBody.FORM)
            .addFormDataPart(
                "thumbnail",
                thumbnailFile.name,
                thumbnailFile.asRequestBody(HttpClient.MediaTypes.IMAGE_PNG)
            )
            .build()

        post<MarketplaceItem>(
            "/marketplace/$id/thumbnail",
            requestBody,
            headers = { addAuth(session) }
        )
    }

    suspend fun deleteThumbnail(session: OAuthSession, id: Int) =
        delete<Unit>("/marketplace/$id/thumbnail", headers = { addAuth(session) })
}
