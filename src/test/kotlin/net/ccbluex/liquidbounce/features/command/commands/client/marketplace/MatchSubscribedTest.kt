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
package net.ccbluex.liquidbounce.features.command.commands.client.marketplace

import net.ccbluex.liquidbounce.api.models.marketplace.MarketplaceItemType
import net.ccbluex.liquidbounce.config.gson.fileGson
import net.ccbluex.liquidbounce.features.marketplace.SubscribedItem
import net.ccbluex.liquidbounce.test.MinecraftBootstrap
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MatchSubscribedTest {

    private fun item(name: String, id: Int, author: String? = null) =
        SubscribedItem(name, id, MarketplaceItemType.SCRIPT).also { it.author = author }

    private val glass = item("Liquid Glass", 12, "CCBlueX")
    private val ours = item("AutoGG", 34, "CCBlueX")
    private val theirs = item("autogg", 56, "Someone")
    private val items = listOf(glass, ours, theirs)

    @Test
    fun `an id picks that item`() {
        assertEquals(listOf(glass), matchSubscribed("12", items))
    }

    @Test
    fun `an unknown id picks nothing`() {
        assertEquals(emptyList(), matchSubscribed("99", items))
    }

    @Test
    fun `a name matches ignoring case`() {
        assertEquals(listOf(glass), matchSubscribed("liquid glass", items))
    }

    @Test
    fun `a shared name matches every item that has it`() {
        assertEquals(listOf(ours, theirs), matchSubscribed("AUTOGG", items))
    }

    @Test
    fun `author and name pick the item of that author`() {
        assertEquals(listOf(theirs), matchSubscribed("someone/AutoGG", items))
    }

    @Test
    fun `author and name match by the name while the author is unknown`() {
        val legacy = item("Old", 78)
        assertEquals(listOf(legacy), matchSubscribed("Anyone/Old", listOf(legacy)))
    }

    @Test
    fun `a name that has a slash itself matches as a whole first`() {
        val slashed = item("a/b", 78)
        val b = item("b", 90, "a")
        assertEquals(listOf(slashed), matchSubscribed("a/b", listOf(slashed, b)))
    }

    @Test
    fun `author and name prefer the item of that author over one whose author is unknown`() {
        val legacy = item("AutoGG", 78)
        assertEquals(listOf(theirs), matchSubscribed("Someone/AutoGG", listOf(legacy, theirs)))
    }

    @Test
    fun `suggests author and name`() {
        assertEquals(listOf("\"CCBlueX/Liquid Glass\"", "CCBlueX/AutoGG", "Someone/autogg"), suggestSubscribed(items))
    }

    @Test
    fun `suggests the name while the author is unknown`() {
        assertEquals(listOf("Old", "CCBlueX/AutoGG"), suggestSubscribed(listOf(item("Old", 1), ours)))
    }

    @Test
    fun `suggests the id where author and name or an unknown author's name are shared`() {
        val script = item("AutoGG", 1, "CCBlueX")
        val theme = item("AutoGG", 2, "CCBlueX")
        val legacy = item("AutoGG", 3)
        assertEquals(listOf("1", "2", "3"), suggestSubscribed(listOf(script, theme, legacy)))
    }

    @Test
    fun `the author is saved with the subscription and missing from older ones`() {
        MinecraftBootstrap.ensureInitialized()

        val saved = fileGson.fromJson(fileGson.toJson(ours), SubscribedItem::class.java)
        assertEquals(ours, saved)
        assertEquals("CCBlueX", saved.author)

        val older = fileGson.fromJson("""{"name": "AutoGG", "id": 34, "type": "Script"}""", SubscribedItem::class.java)
        assertNull(older.author)
    }

}
