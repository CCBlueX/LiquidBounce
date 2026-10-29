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
package net.ccbluex.liquidbounce.api.models.marketplace

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import net.ccbluex.liquidbounce.config.gson.publicGson
import net.ccbluex.liquidbounce.features.marketplace.autoconfig.MarketplaceConfigs.address
import net.ccbluex.liquidbounce.test.MinecraftBootstrap
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MarketplaceMetadataTest {

    companion object {
        init {
            MinecraftBootstrap.ensureInitialized()
        }
    }

    private val itemJson = """
        {
          "id": 12,
          "uid": "owner",
          "type": "Config",
          "name": "Original Config",
          "branch": "nextgen",
          "description": "Settings",
          "featured": false,
          "created_at": "2026-10-01T12:00:00",
          "status": "Active",
          "author": "Original Author",
          "visibility": "public"
        }
    """.trimIndent()

    private val revisionJson = """
        {
          "id": 34,
          "item_id": 12,
          "version": "0.41.1",
          "file_pid": "file",
          "created_at": "2026-10-01T12:00:00",
          "status": "Active"
        }
    """.trimIndent()

    @Test
    fun `older items do not require source or grant community permissions`() {
        val item = publicGson.fromJson(itemJson, MarketplaceItem::class.java)

        assertNull(item.sourceUrl)
        assertFalse(item.sourceRequired)
        assertFalse(item.communityEditable)
        assertFalse(item.canEdit)
        assertFalse(item.canManage)
        assertNull(item.contributors)
        assertEquals("Original Author/Original Config", item.address)
    }

    @Test
    fun `community metadata keeps the original author and name`() {
        val json = publicGson.fromJson(itemJson, JsonObject::class.java).apply {
            addProperty("source_url", "https://example.com/source")
            addProperty("source_required", true)
            addProperty("community_editable", true)
            addProperty("can_edit", true)
            addProperty("can_manage", true)
            add("contributors", publicGson.fromJson("""
                [
                  {
                    "uid": "editor",
                    "nickname": "Contributor",
                    "first_contributed_at": "2026-10-02T12:00:00",
                    "last_contributed_at": "2026-10-03T12:00:00"
                  },
                  {
                    "uid": "another-editor",
                    "first_contributed_at": "2026-10-04T12:00:00",
                    "last_contributed_at": "2026-10-04T12:00:00"
                  }
                ]
            """.trimIndent(), JsonArray::class.java))
        }
        val item = publicGson.fromJson(json, MarketplaceItem::class.java)

        assertEquals("https://example.com/source", item.sourceUrl)
        assertTrue(item.sourceRequired)
        assertTrue(item.communityEditable)
        assertTrue(item.canEdit)
        assertTrue(item.canManage)
        assertEquals("owner", item.uid)
        assertEquals("Original Author", item.author)
        assertEquals("Original Config", item.name)
        assertEquals("Original Author/Original Config", item.address)
        val contributors = item.contributors!!
        assertEquals("editor", contributors[0].uid)
        assertEquals("Contributor", contributors[0].nickname)
        assertEquals("2026-10-02T12:00:00", contributors[0].firstContributedAt)
        assertEquals("2026-10-03T12:00:00", contributors[0].lastContributedAt)
        assertNull(contributors[1].nickname)
        assertEquals(item, publicGson.fromJson(publicGson.toJson(item), MarketplaceItem::class.java))
    }

    @Test
    fun `community config without an author keeps its bare name`() {
        val json = publicGson.fromJson(itemJson, JsonObject::class.java).apply {
            remove("author")
            addProperty("community_editable", true)
        }
        val item = publicGson.fromJson(json, MarketplaceItem::class.java)

        assertNull(item.author)
        assertEquals("Original Config", item.address)
    }

    @Test
    fun `older revisions accept absent source and uploader fields`() {
        val revision = publicGson.fromJson(revisionJson, MarketplaceItemRevision::class.java)

        assertEquals(34, revision.id)
        assertEquals(12, revision.itemId)
        assertNull(revision.sourceRepository)
        assertNull(revision.sourceRef)
        assertNull(revision.sourceCommit)
        assertNull(revision.sourceReleaseUrl)
        assertNull(revision.sourceReleaseTag)
        assertNull(revision.sourceBuildUrl)
        assertNull(revision.uploaderUid)
    }

    @Test
    fun `revision provenance reads flat fields and round trips`() {
        val fields = mapOf(
            "source_repository" to "https://example.com/source",
            "source_ref" to "main",
            "source_commit" to "abc123",
            "source_release_url" to "https://example.com/source/releases/v1",
            "source_release_tag" to "v1",
            "source_build_url" to "https://example.com/source/builds/1",
            "uploader_uid" to "editor"
        )
        val json = publicGson.fromJson(revisionJson, JsonObject::class.java).apply {
            fields.forEach { (name, value) -> addProperty(name, value) }
        }
        val revision = publicGson.fromJson(json, MarketplaceItemRevision::class.java)

        assertEquals(fields["source_repository"], revision.sourceRepository)
        assertEquals(fields["source_ref"], revision.sourceRef)
        assertEquals(fields["source_commit"], revision.sourceCommit)
        assertEquals(fields["source_release_url"], revision.sourceReleaseUrl)
        assertEquals(fields["source_release_tag"], revision.sourceReleaseTag)
        assertEquals(fields["source_build_url"], revision.sourceBuildUrl)
        assertEquals(fields["uploader_uid"], revision.uploaderUid)
        val serialized = publicGson.toJsonTree(revision).asJsonObject
        fields.forEach { (name, value) -> assertEquals(value, serialized[name].asString) }
        assertEquals(revision, publicGson.fromJson(serialized, MarketplaceItemRevision::class.java))
    }
}
