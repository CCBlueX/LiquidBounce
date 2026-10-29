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
import net.ccbluex.liquidbounce.api.core.HttpClient
import net.ccbluex.liquidbounce.api.core.toRequestBody
import net.ccbluex.liquidbounce.api.models.auth.OAuthSession
import net.ccbluex.liquidbounce.api.models.auth.authorization
import net.ccbluex.liquidbounce.api.models.marketplace.MarketplaceItem
import net.ccbluex.liquidbounce.api.models.marketplace.MarketplaceItemRevision
import net.ccbluex.liquidbounce.api.models.marketplace.MarketplaceItemType
import net.ccbluex.liquidbounce.api.models.marketplace.MarketplaceItemVisibility
import net.ccbluex.liquidbounce.api.models.pagination.PaginatedResponse
import net.ccbluex.liquidbounce.config.gson.apiGson
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.create
import java.io.File

@Suppress("TooManyFunctions")
object MarketplaceApi {

    private val baseUrl = config.apiEndpointV3

    private val service: MarketplaceService = config.retrofitApiV3.create()

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
        fun toQuery() = buildMap {
            put("page", page.toString())
            put("limit", limit.toString())
            put("featured", featured.toString())
            query?.let { put("q", it) }
            type?.let { put("type", it.tag) }
            uid?.let { put("uid", it) }
            branch?.let { put("branch", it) }
            filter.name?.let { put("name", it) }
            filter.author?.let { put("author", it) }
            filter.featured?.let { put("is_featured", it.toString()) }
            filter.tags.takeIf { it.isNotEmpty() }?.let { put("tags", it.joinToString(",")) }
            filter.targetServer?.let { put("target_server", it) }
            filter.forkedFrom?.let { put("forked_from", it.toString()) }
            filter.sort?.let { put("sort", it.tag) }
        }
    }

    enum class Sort(val tag: String) {
        CREATED("created"),
        SCORE("score")
    }

    data class Filter(
        val name: String? = null,
        val author: String? = null,
        val featured: Boolean? = null,
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
            tags?.let { add("tags", apiGson.toJsonTree(it)) }
            targetServers?.let { add("target_servers", apiGson.toJsonTree(it)) }
            visibility?.let { add("visibility", apiGson.toJsonTree(it)) }
            forkedFromItemId?.let { addProperty("forked_from_item_id", it) }
            forkedFromRevisionId?.let { addProperty("forked_from_revision_id", it) }
        }
    }

    private fun itemBody(
        name: String,
        type: MarketplaceItemType,
        description: String,
        details: ItemDetails,
    ): RequestBody = JsonObject().apply {
        addProperty("name", name)
        add("type", apiGson.toJsonTree(type))
        addProperty("description", description)
        details.writeTo(this)
    }.toRequestBody()

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
    ): PaginatedResponse<MarketplaceItem> = service.items(
        MarketplaceParams(page, limit, query, type, featured, uid, branch, filter).toQuery(),
        session?.authorization,
    )

    suspend fun getTags() = service.tags()

    suspend fun createMarketplaceItem(
        session: OAuthSession,
        name: String,
        type: MarketplaceItemType,
        description: String,
        details: ItemDetails = ItemDetails()
    ) = service.createItem(itemBody(name, type, description, details), session.authorization)

    @Suppress("LongParameterList")
    suspend fun updateMarketplaceItem(
        session: OAuthSession,
        id: Int,
        name: String,
        type: MarketplaceItemType,
        description: String,
        details: ItemDetails = ItemDetails()
    ) = service.updateItem(id, itemBody(name, type, description, details), session.authorization)

    suspend fun deleteMarketplaceItem(session: OAuthSession, id: Int) =
        service.deleteItem(id, session.authorization)

    suspend fun getMarketplaceItem(id: Int, session: OAuthSession? = null) =
        service.item(id, session?.authorization)

    suspend fun getMarketplaceItemByCode(shareCode: String) = service.itemByCode(shareCode)

    // Revisions
    /**
     * With [minecraft], an add-on only lists the revisions that work with that version.
     */
    suspend fun getMarketplaceItemRevisions(
        id: Int,
        page: Int = 1,
        limit: Int = 10,
        minecraft: String? = null,
        liquidbounce: String? = null
    ) = service.revisions(
        id = id,
        page = page,
        limit = limit,
        minecraft = minecraft,
        liquidbounce = liquidbounce,
    )

    suspend fun getMarketplaceItemRevision(id: Int, revisionId: Int) = service.revision(id, revisionId)

    @Suppress("LongParameterList")
    suspend fun createMarketplaceItemRevision(
        session: OAuthSession,
        id: Int,
        fileName: String,
        content: RequestBody,
        version: String,
        changelog: String? = null,
        dependencies: String? = null,
        includesBinds: Boolean? = null
    ): MarketplaceItemRevision = service.createRevision(
        id = id,
        body = MultipartBody.Builder().setType(MultipartBody.FORM)
            .addFormDataPart("file", fileName, content)
            .addFormDataPart("version", version)
            .apply {
                changelog?.let { addFormDataPart("changelog", it) }
                dependencies?.let { addFormDataPart("dependencies", it) }
                includesBinds?.let { addFormDataPart("includes_binds", it.toString()) }
            }
            .build(),
        authorization = session.authorization,
    )

    suspend fun deleteMarketplaceItemRevision(session: OAuthSession, id: Int, revisionId: Int) =
        service.deleteRevision(id, revisionId, session.authorization)

    fun downloadRevision(id: Int, revisionId: Int) =
        "$baseUrl/marketplace/$id/revisions/$revisionId/download"

    // Dependencies
    suspend fun getItemDependencies(id: Int) = service.dependencies(id)

    suspend fun addItemDependency(session: OAuthSession, id: Int, dependencyItemId: Int) =
        service.addDependency(
            id,
            JsonObject().apply { addProperty("dependency_item_id", dependencyItemId) }.toRequestBody(),
            session.authorization,
        )

    suspend fun removeItemDependency(session: OAuthSession, id: Int, dependencyItemId: Int) =
        service.removeDependency(id, dependencyItemId, session.authorization)

    suspend fun getRevisionDependencies(id: Int, revisionId: Int) = service.revisionDependencies(id, revisionId)

    suspend fun addRevisionDependency(
        session: OAuthSession,
        id: Int,
        revisionId: Int,
        dependencyRevisionId: Int
    ) = service.addRevisionDependency(
        id, revisionId,
        JsonObject().apply { addProperty("dependency_revision_id", dependencyRevisionId) }.toRequestBody(),
        session.authorization,
    )

    suspend fun removeRevisionDependency(
        session: OAuthSession,
        id: Int,
        revisionId: Int,
        dependencyRevisionId: Int
    ) = service.removeRevisionDependency(
        id, revisionId,
        JsonObject().apply { addProperty("dependency_revision_id", dependencyRevisionId) }.toRequestBody(),
        session.authorization,
    )

    // Reviews
    suspend fun getReviews(id: Int, page: Int = 1, limit: Int = 10) = service.reviews(id, page, limit)

    suspend fun createReview(
        session: OAuthSession,
        id: Int,
        rating: Int,
        review: String? = null
    ) = service.createReview(
        id,
        JsonObject().apply {
            addProperty("rating", rating)
            review?.let { addProperty("review", it) }
        }.toRequestBody(),
        session.authorization,
    )

    suspend fun deleteReview(session: OAuthSession, id: Int, reviewId: Int) =
        service.deleteReview(id, reviewId, session.authorization)

    // Config reports
    @Suppress("LongParameterList")
    suspend fun putConfigReport(
        session: OAuthSession,
        id: Int,
        revisionId: Int,
        works: Boolean,
        clientVersion: String? = null,
        serverAddress: String? = null
    ) = service.putConfigReport(
        id,
        revisionId,
        JsonObject().apply {
            addProperty("works", works)
            clientVersion?.let { addProperty("client_version", it) }
            serverAddress?.let { addProperty("server_address", it) }
        }.toRequestBody(),
        session.authorization,
    )

    suspend fun getOwnConfigReport(session: OAuthSession, id: Int, revisionId: Int) =
        service.configReport(id, revisionId, session.authorization)

    suspend fun deleteConfigReport(session: OAuthSession, id: Int, revisionId: Int) =
        service.deleteConfigReport(id, revisionId, session.authorization)

    suspend fun getConfigReports(id: Int) = service.configReports(id)

    // Thumbnails
    suspend fun uploadThumbnail(session: OAuthSession, id: Int, thumbnailFile: File) {
        service.uploadThumbnail(
            id,
            MultipartBody.Builder().setType(MultipartBody.FORM)
                .addFormDataPart(
                    "thumbnail",
                    thumbnailFile.name,
                    thumbnailFile.asRequestBody(HttpClient.MediaTypes.IMAGE_PNG),
                )
                .build(),
            session.authorization,
        )
    }

    suspend fun deleteThumbnail(session: OAuthSession, id: Int) =
        service.deleteThumbnail(id, session.authorization)

}
