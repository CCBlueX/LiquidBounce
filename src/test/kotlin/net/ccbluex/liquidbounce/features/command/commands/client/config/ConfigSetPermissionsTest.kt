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
package net.ccbluex.liquidbounce.features.command.commands.client.config

import net.ccbluex.liquidbounce.api.models.marketplace.MarketplaceItemVisibility
import net.ccbluex.liquidbounce.api.services.marketplace.MarketplaceApi
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ConfigSetPermissionsTest {

    @Test
    fun `name description tags and servers use editor permissions`() {
        val content = listOf(
            MarketplaceApi.ItemDetails(),
            MarketplaceApi.ItemDetails(tags = listOf(1)),
            MarketplaceApi.ItemDetails(tags = emptyList()),
            MarketplaceApi.ItemDetails(targetServers = listOf("example.com")),
            MarketplaceApi.ItemDetails(targetServers = emptyList())
        )
        content.forEach { assertFalse(it.requiresConfigOwner) }
    }

    @Test
    fun `visibility stays owner only even when changing content`() {
        for (visibility in MarketplaceItemVisibility.entries) {
            assertTrue(MarketplaceApi.ItemDetails(visibility = visibility).requiresConfigOwner)
            assertTrue(MarketplaceApi.ItemDetails(tags = listOf(1), visibility = visibility).requiresConfigOwner)
        }
    }
}
