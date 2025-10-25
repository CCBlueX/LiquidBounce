/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2025 CCBlueX
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
package net.ccbluex.liquidbounce.features.itemgroup.groups

import net.ccbluex.liquidbounce.api.core.HttpClient
import net.ccbluex.liquidbounce.api.core.HttpMethod
import net.ccbluex.liquidbounce.api.core.ioScope
import net.ccbluex.liquidbounce.api.core.parse
import net.ccbluex.liquidbounce.api.core.retrying
import net.ccbluex.liquidbounce.features.itemgroup.ClientItemGroup
import net.ccbluex.liquidbounce.utils.client.logger
import net.ccbluex.liquidbounce.utils.item.createItem
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import java.util.*
import kotlin.time.Duration.Companion.seconds

data class Head(val name: String, val uuid: UUID, val value: String) {

    private fun asNbt() =
        "[minecraft:custom_name='{" +
            "\"text\":\"$name\"," +
            "\"color\":\"gold\"," +
            "\"underlined\":false," +
            "\"bold\":true," +
            "\"italic\":false" +
        "}',minecraft:lore=['{" +
            "\"text\":\"UUID: $uuid\"," +
            "\"color\":\"gray\"," +
            "\"italic\":false" +
        "}','{" +
            "\"text\":\"liquidbounce.net\"," +
            "\"color\":\"blue\"," +
            "\"italic\":false" +
        "}'],profile={id:[I;0,0,0,0],properties:[{" +
            "name:\"textures\"," +
            "value:\"$value\"" +
        "}]}]"

    fun asItemStack() =
        createItem("minecraft:player_head${asNbt()}")

}

class HeadsItemGroup : ClientItemGroup(
    "Heads",
    icon = { ItemStack(Items.SKELETON_SKULL) },
    items = { items ->
        heads.getNow()?.let { heads ->
            items.addAll(heads.distinctBy { it.name }.map(Head::asItemStack))
        }
    }
) {
    companion object {
        /**
         * The API endpoint to fetch heads from which is owned by CCBlueX
         * and therefore can reliably depend on.
         */
        const val HEAD_DB_API = "https://headdb.org/api/category/all"

        val heads = ioScope.retrying(
            interval = 1.seconds,
            name = "player-heads",
            maxRetries = 3,
        ) {
            val heads: HashMap<String, Head> = HttpClient.request(HEAD_DB_API, HttpMethod.GET).parse()

            heads.values.also {
                logger.info("Successfully loaded ${it.size} heads from HeadDB")
            } as Collection<Head>
        }
    }
}
