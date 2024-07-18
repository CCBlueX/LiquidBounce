/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2024 CCBlueX
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
 *
 *
 */

package net.ccbluex.liquidbounce.web.socket.protocol.rest.game

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import net.ccbluex.liquidbounce.utils.client.convertToString
import net.ccbluex.liquidbounce.utils.client.logger
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.item.isNothing
import net.ccbluex.liquidbounce.web.socket.netty.httpForbidden
import net.ccbluex.liquidbounce.web.socket.netty.httpOk
import net.ccbluex.liquidbounce.web.socket.netty.rest.RestNode
import net.minecraft.item.BlockItem
import net.minecraft.registry.DefaultedRegistry
import net.minecraft.registry.Registries
import net.minecraft.registry.tag.BlockTags
import net.minecraft.registry.tag.ItemTags
import net.minecraft.registry.tag.TagKey
import net.minecraft.util.Identifier
import net.minecraft.util.math.BlockPos
import kotlin.jvm.optionals.getOrNull

val ACCEPTED_ITEM_TAGS
    get() = arrayOf(
        ItemTags.WOOL,
        ItemTags.PLANKS,
        ItemTags.STONE_BRICKS,
        ItemTags.BUTTONS,
        ItemTags.WOOL_CARPETS,
        ItemTags.FENCE_GATES,
        ItemTags.WOODEN_PRESSURE_PLATES,
        ItemTags.DOORS,
        ItemTags.LOGS,
        ItemTags.BANNERS,
        ItemTags.SAND,
        ItemTags.STAIRS,
        ItemTags.SLABS,
        ItemTags.WALLS,
        ItemTags.ANVIL,
        ItemTags.RAILS,
        ItemTags.FLOWERS,
        ItemTags.SAPLINGS,
        ItemTags.LEAVES,
        ItemTags.TRAPDOORS,
        ItemTags.BEDS,
        ItemTags.FENCES,
        ItemTags.GOLD_ORES,
        ItemTags.IRON_ORES,
        ItemTags.DIAMOND_ORES,
        ItemTags.REDSTONE_ORES,
        ItemTags.LAPIS_ORES,
        ItemTags.COAL_ORES,
        ItemTags.EMERALD_ORES,
        ItemTags.COPPER_ORES,
        ItemTags.CANDLES,
        ItemTags.DIRT,
        ItemTags.TERRACOTTA,
        ItemTags.BOATS,
        ItemTags.FISHES,
        ItemTags.SIGNS,
        ItemTags.CREEPER_DROP_MUSIC_DISCS,
        ItemTags.COALS,
        ItemTags.ARROWS,
        ItemTags.COMPASSES,
        ItemTags.TRIM_TEMPLATES,
        ItemTags.SWORDS,
        ItemTags.AXES,
        ItemTags.HOES,
        ItemTags.PICKAXES,
        ItemTags.SHOVELS,
    )

val ACCEPTED_BLOCK_TAGS
    get() = arrayOf(
        BlockTags.WOOL,
        BlockTags.PLANKS,
        BlockTags.STONE_BRICKS,
        BlockTags.BUTTONS,
        BlockTags.WOOL_CARPETS,
        BlockTags.PRESSURE_PLATES,
        BlockTags.DOORS,
        BlockTags.FLOWERS,
        BlockTags.SAPLINGS,
        BlockTags.LOGS,
        BlockTags.BANNERS,
        BlockTags.SAND,
        BlockTags.STAIRS,
        BlockTags.SLABS,
        BlockTags.WALLS,
        BlockTags.ANVIL,
        BlockTags.RAILS,
        BlockTags.LEAVES,
        BlockTags.TRAPDOORS,
        BlockTags.BEDS,
        BlockTags.FENCES,
        BlockTags.GOLD_ORES,
        BlockTags.IRON_ORES,
        BlockTags.DIAMOND_ORES,
        BlockTags.REDSTONE_ORES,
        BlockTags.LAPIS_ORES,
        BlockTags.COAL_ORES,
        BlockTags.EMERALD_ORES,
        BlockTags.COPPER_ORES,
        BlockTags.CANDLES,
        BlockTags.DIRT,
        BlockTags.TERRACOTTA,
        BlockTags.FLOWER_POTS,
        BlockTags.ICE,
        BlockTags.CORALS,
        BlockTags.ALL_SIGNS,
        BlockTags.BEEHIVES,
        BlockTags.CROPS,
        BlockTags.PORTALS,
        BlockTags.FIRE,
        BlockTags.NYLIUM,
        BlockTags.SHULKER_BOXES,
        BlockTags.CAMPFIRES,
        BlockTags.FENCE_GATES,
        BlockTags.CAULDRONS,
        BlockTags.SNOW,
    )

fun <T> constructMap(registry: DefaultedRegistry<T>, tagKeys: Array<TagKey<T>>): Map<Identifier, Identifier> {
    val map = hashMapOf<Identifier, Identifier>()

    for (acceptedTag in tagKeys) {
        val get = registry.getEntryList(acceptedTag).getOrNull() ?: continue

        get.forEach {
            val itemId = registry.getId(it.value())

            if (map.containsKey(itemId)) {
                println("Duplicate $itemId in ${acceptedTag.id} in ${map[itemId]}")

                return@forEach
            }

            map[itemId] = acceptedTag.id
        }
    }

    return map
}

fun RestNode.registriesRest() {
    get("/registries") {
        val parentMap = hashMapOf<Identifier, Identifier>()
        val world = mc.world ?: return@get httpForbidden("No world")

        Registries.BLOCK.forEach {
            val pickStack = it.getPickStack(world, BlockPos.ORIGIN , it.defaultState)

            val id = Registries.BLOCK.getId(it)

            when (val item = pickStack.item) {
                is BlockItem -> {
                    if (item.block != it) {
                        parentMap[id] = Registries.BLOCK.getId(item.block)
                    }
                }
                else -> {
                    if (!pickStack.isNothing()) {
                        logger.warn("Invalid pick stack for $id: $pickStack")
                    }
                }
            }
        }

        httpOk(JsonObject().apply {
            add("blocks", JsonArray().apply {
                Registries.BLOCK.forEach { block ->
                    val jsonObject = JsonObject()
                    jsonObject.addProperty("identifier", Registries.BLOCK.getId(block).toString())
                    jsonObject.addProperty("name", block.name.convertToString())
                    add(jsonObject)
                }
            })
            add("items", JsonArray().apply {
                Registries.ITEM.forEach { item ->
                    val jsonObject = JsonObject()
                    jsonObject.addProperty("identifier", Registries.ITEM.getId(item).toString())
                    jsonObject.addProperty("name", item.name.convertToString())
                    add(jsonObject)
                }
            })
            add("itemGroups", JsonObject().apply {
                for ((k, v) in constructMap(Registries.ITEM, ACCEPTED_ITEM_TAGS).entries) {
                    add(
                        k.toString(),
                        JsonObject().apply {
                            addProperty("relation", "group")
                            addProperty("relative", v.toString())
                        }
                    )
                }
            })
            add("blockGroups", JsonObject().apply {
                val constructedMap = constructMap(Registries.BLOCK, ACCEPTED_BLOCK_TAGS)

                Registries.BLOCK.forEach { block ->
                    val id = Registries.BLOCK.getId(block)

                    val obj = when (id) {
                        in parentMap -> JsonObject().apply {
                            addProperty("relation", "parent")
                            addProperty("relative", parentMap[id].toString())
                        }
                        in constructedMap -> JsonObject().apply {
                            addProperty("relation", "group")
                            addProperty("relative", constructedMap[id].toString())
                        }
                        else -> return@forEach
                    }

                    add(id.toString(), obj)
                }
            })
        })
    }
}

