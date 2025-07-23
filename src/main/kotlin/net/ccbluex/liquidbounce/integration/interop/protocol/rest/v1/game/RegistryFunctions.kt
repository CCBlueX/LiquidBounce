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
 *
 *
 */

@file:Suppress("LongMethod")

package net.ccbluex.liquidbounce.integration.interop.protocol.rest.v1.game

import com.google.gson.JsonObject
import net.ccbluex.liquidbounce.integration.interop.ClientInteropServer
import net.ccbluex.liquidbounce.utils.client.convertToString
import net.ccbluex.liquidbounce.utils.client.logger
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.client.toName
import net.ccbluex.liquidbounce.utils.item.isNothing
import net.ccbluex.liquidbounce.utils.network.packetRegistry
import net.ccbluex.netty.http.model.RequestObject
import net.ccbluex.netty.http.util.httpForbidden
import net.ccbluex.netty.http.util.httpOk
import net.minecraft.item.BlockItem
import net.minecraft.item.Items
import net.minecraft.network.NetworkSide
import net.minecraft.registry.DefaultedRegistry
import net.minecraft.registry.Registries
import net.minecraft.registry.tag.BlockTags
import net.minecraft.registry.tag.ItemTags
import net.minecraft.registry.tag.TagKey
import net.minecraft.util.Identifier
import net.minecraft.util.math.BlockPos
import java.util.*
import kotlin.jvm.optionals.getOrNull

private val ACCEPTED_ITEM_TAGS =
    arrayOf(
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
        ItemTags.SMALL_FLOWERS,
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
        ItemTags.TRIM_MATERIALS,
        ItemTags.SWORDS,
        ItemTags.AXES,
        ItemTags.HOES,
        ItemTags.PICKAXES,
        ItemTags.SHOVELS,
    )

private val ACCEPTED_BLOCK_TAGS =
    arrayOf(
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

private fun <T> constructMap(registry: DefaultedRegistry<T>, tagKeys: Array<TagKey<T>>): Map<Identifier, Identifier> {
    val map = hashMapOf<Identifier, Identifier>()

    for (acceptedTag in tagKeys) {
        val get = registry.getOptional(acceptedTag).getOrNull() ?: continue

        get.forEach {
            val itemId = registry.getId(it.value())

            val prev = map.putIfAbsent(itemId, acceptedTag.id)
            if (prev != null) {
                logger.warn("Duplicate $itemId in ${acceptedTag.id} in $prev")

                return@forEach
            }
        }
    }

    return map
}

// GET /api/v1/client/registry/:name
@Suppress("UNUSED_PARAMETER")
fun getRegistry(requestObject: RequestObject) = httpOk(JsonObject().apply {
    fun iconUrl(id: Identifier) =
        "${ClientInteropServer.url}/api/v1/client/resource/itemTexture?id=$id"

    val registryName = requestObject.params["name"]
        ?: return httpForbidden("Missing registry name parameter")
    when (registryName.lowercase(Locale.ENGLISH)) {
        "blocks" -> {
            Registries.BLOCK.forEach { block ->
                val id = Registries.BLOCK.getId(block)
                add(id.toString(), JsonObject().apply {
                    addProperty("name", block.name.convertToString())
                    addProperty("icon", iconUrl(id))
                })
            }
        }

        "items" -> {
            Registries.ITEM.forEach { item ->
                val id = Registries.ITEM.getId(item)
                add(id.toString(), JsonObject().apply {
                    addProperty("name", item.name.convertToString())
                    addProperty("icon", iconUrl(id))
                })
            }
        }

        "sounds" -> {
            val soundDiscId = Registries.ITEM.getId(Items.MUSIC_DISC_13)

            Registries.SOUND_EVENT.forEach { soundEvent ->
                val id = Registries.SOUND_EVENT.getId(soundEvent)
                add(id.toString(), JsonObject().apply {
                    addProperty("name", soundEvent.id.toName())
                    addProperty("icon", iconUrl(soundDiscId))
                })
            }
        }

        "statuseffects" -> {
            val potionId = Registries.ITEM.getId(Items.POTION)

            Registries.STATUS_EFFECT.forEach { effect ->
                val id = Registries.STATUS_EFFECT.getId(effect)
                add(id.toString(), JsonObject().apply {
                    addProperty("name", effect.name.convertToString())
                    addProperty("icon", iconUrl(potionId))
                })
            }
        }

        "clientpackets" -> {
            val iconId = Registries.ITEM.getId(Items.PAPER)

            packetRegistry[NetworkSide.SERVERBOUND]?.forEach { packetId ->
                add(packetId.toString(), JsonObject().apply {
                    addProperty("name", packetId.toName())
                    addProperty("icon", iconUrl(iconId))
                })
            }
        }

        "serverpackets" -> {
            val iconId = Registries.ITEM.getId(Items.PAPER)

            packetRegistry[NetworkSide.CLIENTBOUND]?.forEach { packetId ->
                add(packetId.toString(), JsonObject().apply {
                    addProperty("name", packetId.toName())
                    addProperty("icon", iconUrl(iconId))
                })
            }
        }

        else -> return httpForbidden("Invalid registry name: $registryName")
    }
})


// GET /api/v1/client/registry/:name/groups
@Suppress("UNUSED_PARAMETER", "CognitiveComplexMethod")
fun getRegistryGroups(requestObject: RequestObject) = httpOk(JsonObject().apply {
    val registryName = requestObject.params["name"]
        ?: return httpForbidden("Missing registry name parameter")
    when (registryName.lowercase(Locale.ENGLISH)) {
        "items" -> {
            for ((k, v) in constructMap(Registries.ITEM, ACCEPTED_ITEM_TAGS)) {
                add(
                    k.toString(),
                    JsonObject().apply {
                        addProperty("relation", "group")
                        addProperty("relative", v.toString())
                    }
                )
            }
        }

        "blocks" -> {
            val parentMap = hashMapOf<Identifier, Identifier>()
            val world = mc.world ?: return httpForbidden("No world")

            Registries.BLOCK.forEach { block ->
                val pickStack = block.getPickStack(world, BlockPos.ORIGIN, block.defaultState, false)
                val id = Registries.BLOCK.getId(block)

                when (val item = pickStack.item) {
                    is BlockItem -> {
                        if (item.block != block) {
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

            val constructedMap = constructMap(Registries.BLOCK, ACCEPTED_BLOCK_TAGS)

            Registries.BLOCK.forEach { block ->
                val id = Registries.BLOCK.getId(block)

                val obj = when (id) {
                    in parentMap -> JsonObject().apply {
                        addProperty("relation", "parent")
                        addProperty("relative", parentMap[id]!!.toString())
                    }

                    in constructedMap -> JsonObject().apply {
                        addProperty("relation", "group")
                        addProperty("relative", constructedMap[id]!!.toString())
                    }

                    else -> return@forEach
                }

                add(id.toString(), obj)
            }
        }

        else -> return httpForbidden("Invalid registry name: $registryName")
    }
})
