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

import net.ccbluex.liquidbounce.api.models.marketplace.MarketplaceItem
import net.ccbluex.liquidbounce.api.models.marketplace.MarketplaceItemStatus
import net.ccbluex.liquidbounce.api.models.marketplace.MarketplaceItemType
import net.ccbluex.liquidbounce.api.models.marketplace.MarketplaceItemVisibility
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ConfigEditorTest {

    private val config = MarketplaceItem(
        id = 12,
        uid = "owner",
        type = MarketplaceItemType.CONFIG,
        name = "Config",
        branch = "nextgen",
        description = "",
        thumbnailPid = null,
        featured = false,
        createdAt = "2026-10-01T12:00:00",
        status = MarketplaceItemStatus.ACTIVE,
        author = "Owner",
        visibility = MarketplaceItemVisibility.PUBLIC
    )

    @Test
    fun `community editing opens public configs to signed-in users`() {
        assertTrue(config.isEditableBy("owner"))
        assertFalse(config.isEditableBy("other"))
        assertTrue(config.copy(communityEditable = true).isEditableBy("other"))
        assertFalse(config.copy(communityEditable = true).isEditableBy(null))
    }

    @Test
    fun `community editing does not open unlisted configs`() {
        val unlisted = config.copy(communityEditable = true, visibility = MarketplaceItemVisibility.UNLISTED)
        assertFalse(unlisted.isEditableBy("other"))
        assertTrue(unlisted.copy(canEdit = true).isEditableBy("other"))
    }
}
