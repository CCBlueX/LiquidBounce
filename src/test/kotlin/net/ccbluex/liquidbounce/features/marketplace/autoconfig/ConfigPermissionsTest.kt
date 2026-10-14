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
package net.ccbluex.liquidbounce.features.marketplace.autoconfig

import net.ccbluex.liquidbounce.api.models.marketplace.MarketplaceItem
import net.ccbluex.liquidbounce.api.models.marketplace.MarketplaceItemStatus
import net.ccbluex.liquidbounce.api.models.marketplace.MarketplaceItemType
import net.ccbluex.liquidbounce.api.models.marketplace.MarketplaceItemVisibility
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ConfigPermissionsTest {

    private val item = MarketplaceItem(
        id = 12,
        uid = "owner",
        type = MarketplaceItemType.CONFIG,
        name = "Original Config",
        branch = "nextgen",
        description = "Settings",
        thumbnailPid = null,
        featured = false,
        createdAt = "2026-10-01T12:00:00",
        status = MarketplaceItemStatus.ACTIVE,
        author = "Original Author",
        visibility = MarketplaceItemVisibility.PUBLIC
    )

    @Test
    fun `owners can edit configs without viewer flags`() {
        assertTrue(item.canEditConfig("owner"))
        assertTrue(item.copy(visibility = MarketplaceItemVisibility.UNLISTED).canEditConfig("owner"))
        assertTrue(item.copy(visibility = null).canEditConfig("owner"))
    }

    @Test
    fun `nonowners cannot edit ordinary configs`() {
        assertFalse(item.canEditConfig("editor"))
        assertFalse(item.copy(canManage = true).canEditConfig("editor"))
    }

    @Test
    fun `community editing requires a signed in viewer`() {
        assertTrue(item.copy(communityEditable = true).canEditConfig("editor"))
        assertFalse(item.copy(communityEditable = true).canEditConfig(null))
        assertFalse(item.copy(canEdit = true).canEditConfig(null))
    }

    @Test
    fun `community flag alone does not grant unlisted or unknown visibility editing`() {
        val unlisted = item.copy(communityEditable = true, visibility = MarketplaceItemVisibility.UNLISTED)
        assertFalse(unlisted.canEditConfig("editor"))
        assertFalse(item.copy(communityEditable = true, visibility = null).canEditConfig("editor"))
    }

    @Test
    fun `viewer permission grants config editing independently of community visibility`() {
        assertTrue(item.copy(canEdit = true).canEditConfig("editor"))
        assertTrue(item.copy(canEdit = true, visibility = MarketplaceItemVisibility.UNLISTED).canEditConfig("editor"))
    }

    @Test
    fun `config editing never grants permissions for other item types`() {
        for (type in MarketplaceItemType.entries.filter { it != MarketplaceItemType.CONFIG }) {
            val other = item.copy(type = type, communityEditable = true, canEdit = true)
            assertFalse(other.canEditConfig("editor"), type.name)
            assertFalse(other.canEditConfig("owner"), type.name)
        }
    }
}
