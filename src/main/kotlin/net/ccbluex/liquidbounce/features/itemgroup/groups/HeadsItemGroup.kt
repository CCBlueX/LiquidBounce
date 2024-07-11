/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2024 CCBlueX
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

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.config.util.decode
import net.ccbluex.liquidbounce.features.itemgroup.ClientItemGroup
import net.ccbluex.liquidbounce.utils.client.logger
import net.ccbluex.liquidbounce.utils.io.HttpClient
import net.ccbluex.liquidbounce.utils.item.createItem
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import java.util.*

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

val headsCollection by lazy {
    runCatching {
        class HeadsService(val enabled: Boolean, val url: String)

        logger.info("Loading heads...")
        // Load head service from cloud
        // Makes it possible to disable service or change domain in case of an emergency
        val headService: HeadsService = decode(HttpClient.get("${LiquidBounce.CLIENT_CLOUD}/heads.json"))

        if (headService.enabled) {
            // Load heads from service
            //  Syntax based on HeadDB (headdb.org)
            val heads: HashMap<String, Head> = decode(HttpClient.get(headService.url))

            heads.map { it.value }
                .toTypedArray()
                .also {
                    logger.info("Successfully loaded ${it.size} heads from the database")
                }
        } else {
            error("Head service has been disabled")
        }
    }.onFailure {
        logger.error("Unable to load heads database", it)
    }.getOrElse { emptyArray() }
}

class HeadsItemGroup : ClientItemGroup(
    "Heads",
    icon = { ItemStack(Items.SKELETON_SKULL) },
    items = { items ->
        items.addAll(
            headsCollection
                .distinctBy { it.name }
                .map(Head::asItemStack)
        )
    }
)
