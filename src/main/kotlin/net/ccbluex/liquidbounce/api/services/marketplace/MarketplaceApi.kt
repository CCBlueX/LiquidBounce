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
import net.ccbluex.liquidbounce.features.addon.AddonInstaller
import net.ccbluex.liquidbounce.utils.client.OkHttpUtils.asRequestBody
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.create
import java.io.File

@Suppress("TooManyFunctions")
object MarketplaceApi {

    private val service: MarketplaceService = config.retrofit.create()

    /**
     * Sent with every item read, so an add-on comes with the revision that loads in this game.
     */
    private val game by lazy {
        mapOf("minecraft" to AddonInstaller.minecraft, "liquidbounce" to AddonInstaller.liquidbounce)
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
     * Listing and config fields of an item. On update, `null` leaves a field unchanged; a fork is only
     * told when the item is created.
     */
    data class ItemDetails(
        val tags: List<Int>? = null,
        val targetServers: List<String>? = null,
        val visibility: MarketplaceItemVisibility? = null,
        val forkedFromItemId: Int? = null,
        val forkedFromRevisionId: Int? = null,
    ) {
        fun writeTo(json: JsonObject) = with(json) {
            tags?.let { add("tagIds", apiGson.toJsonTree(it)) }
            targetServers?.let { add("targetServers", apiGson.toJsonTree(it)) }
            visibility?.let { add("visibility", apiGson.toJsonTree(it)) }
            if (forkedFromItemId != null && forkedFromRevisionId != null) {
                add("forkedFrom", JsonObject().apply {
                    addProperty("itemId", forkedFromItemId)
                    addProperty("revisionId", forkedFromRevisionId)
                })
            }
        }
    }

    @Suppress("LongParameterList")
    suspend fun getMarketplaceItems(
        page: Int = 1,
        limit: Int = 10,
        query: String? = null,
        type: MarketplaceItemType? = null,
        edition: String? = null,
        filter: Filter = Filter(),
    ): PaginatedResponse<MarketplaceItem> = service.items(
        buildMap {
            putAll(game)
            put("limit", limit.toString())
            put("offset", ((page - 1) * limit).toString())
            query?.let { put("q", it) }
            type?.let { put("type", it.tag.lowercase()) }
            edition?.let { put("edition", it) }
            filter.name?.let { put("name", it) }
            filter.author?.let { put("author", it) }
            filter.featured?.let { put("featured", it.toString()) }
            filter.tags.takeIf { it.isNotEmpty() }?.let { put("tags", it.joinToString(",")) }
            filter.targetServer?.let { put("targetServer", it) }
            filter.forkedFrom?.let { put("forkedFrom", it.toString()) }
            filter.sort?.let { put("sort", it.tag) }
        }
    )

    suspend fun getTags() = service.tags()

    @Suppress("LongParameterList")
    suspend fun createMarketplaceItem(
        session: OAuthSession,
        name: String,
        type: MarketplaceItemType,
        edition: String,
        description: String,
        details: ItemDetails = ItemDetails()
    ) = service.createItem(
        JsonObject().apply {
            addProperty("name", name)
            addProperty("type", type.tag.lowercase())
            addProperty("edition", edition)
            addProperty("description", description)
            details.writeTo(this)
        }.toRequestBody(),
        session.authorization,
    )

    /**
     * `null` leaves the name or the description unchanged. The answer comes without revisions.
     */
    suspend fun updateMarketplaceItem(
        session: OAuthSession,
        id: Int,
        name: String? = null,
        description: String? = null,
        details: ItemDetails = ItemDetails()
    ) = service.updateItem(
        id,
        JsonObject().apply {
            name?.let { addProperty("name", it) }
            description?.let { addProperty("description", it) }
            details.writeTo(this)
        }.toRequestBody(),
        session.authorization,
    )

    suspend fun deleteMarketplaceItem(session: OAuthSession, id: Int) =
        service.deleteItem(id, session.authorization)

    /**
     * A published item, unlisted ones included, with everything it requires.
     */
    suspend fun getMarketplaceItem(id: Int) = service.item(id, game)

    suspend fun getMarketplaceItemByCode(shareCode: String) = service.itemByCode(shareCode, game)

    /**
     * An item of the account as only its owner reads it: published or not, with its share code.
     */
    suspend fun getOwnMarketplaceItem(session: OAuthSession, id: Int) = service.ownItem(id, session.authorization)

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
        limit = limit,
        offset = (page - 1) * limit,
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
        includesBinds: Boolean? = null
    ): MarketplaceItemRevision = service.createRevision(
        id = id,
        body = MultipartBody.Builder().setType(MultipartBody.FORM)
            .addFormDataPart("file", fileName, content)
            .addFormDataPart("version", version)
            .apply {
                changelog?.let { addFormDataPart("changelog", it) }
                includesBinds?.let { addFormDataPart("includesBinds", it.toString()) }
            }
            .build(),
        authorization = session.authorization,
    )

    suspend fun deleteMarketplaceItemRevision(session: OAuthSession, id: Int, revisionId: Int) =
        service.deleteRevision(id, revisionId, session.authorization)

    fun downloadRevision(id: Int, revisionId: Int) =
        config.endpoint("marketplace/items/$id/revisions/$revisionId/file")

    fun fileUrl(pid: String) = config.endpoint("files/$pid")

    suspend fun addItemDependency(session: OAuthSession, id: Int, dependencyItemId: Int) =
        service.addDependency(id, dependencyItemId, session.authorization)

    suspend fun removeItemDependency(session: OAuthSession, id: Int, dependencyItemId: Int) =
        service.removeDependency(id, dependencyItemId, session.authorization)

    suspend fun getReviews(id: Int, page: Int = 1, limit: Int = 10) =
        service.reviews(id, limit, (page - 1) * limit)

    suspend fun createReview(
        session: OAuthSession,
        id: Int,
        rating: Int,
        comment: String? = null
    ) = service.createReview(
        id,
        JsonObject().apply {
            addProperty("rating", rating)
            comment?.let { addProperty("comment", it) }
        }.toRequestBody(),
        session.authorization,
    )

    suspend fun deleteReview(session: OAuthSession, id: Int, reviewId: Int) =
        service.deleteReview(id, reviewId, session.authorization)

    suspend fun putConfigReport(
        session: OAuthSession,
        revisionId: Int,
        works: Boolean,
        clientVersion: String? = null,
        serverAddress: String? = null
    ) = service.putConfigReport(
        revisionId,
        JsonObject().apply {
            addProperty("works", works)
            clientVersion?.let { addProperty("clientVersion", it) }
            serverAddress?.let { addProperty("serverAddress", it) }
        }.toRequestBody(),
        session.authorization,
    )

    suspend fun getOwnConfigReport(session: OAuthSession, revisionId: Int) =
        service.configReport(revisionId, session.authorization)

    suspend fun deleteConfigReport(session: OAuthSession, revisionId: Int) =
        service.deleteConfigReport(revisionId, session.authorization)

    suspend fun uploadThumbnail(session: OAuthSession, id: Int, thumbnailFile: File) {
        service.setThumbnail(
            id,
            MultipartBody.Builder().setType(MultipartBody.FORM)
                .addFormDataPart(
                    "file",
                    thumbnailFile.name,
                    asRequestBody(thumbnailFile, HttpClient.MediaTypes.IMAGE_PNG),
                )
                .build(),
            session.authorization,
        )
    }

    suspend fun deleteThumbnail(session: OAuthSession, id: Int) =
        service.deleteThumbnail(id, session.authorization)

}
