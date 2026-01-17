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

@file:Suppress("LongMethod")

package net.ccbluex.liquidbounce.integration.interop.protocol.rest.v1.game

import com.google.common.base.CaseFormat
import com.google.gson.JsonObject
import io.netty.handler.codec.http.FullHttpResponse
import net.ccbluex.fastutil.objectObjectHashMapOf
import net.ccbluex.liquidbounce.config.gson.interopGson
import net.ccbluex.liquidbounce.features.module.ModuleManager
import net.ccbluex.liquidbounce.integration.interop.ClientInteropServer
import net.ccbluex.liquidbounce.utils.client.logger
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.client.toName
import net.ccbluex.liquidbounce.utils.item.getOrNull
import net.ccbluex.liquidbounce.utils.network.packetRegistry
import net.ccbluex.netty.http.model.RequestObject
import net.ccbluex.netty.http.util.httpForbidden
import net.ccbluex.netty.http.util.httpOk
import net.ccbluex.netty.http.util.httpServiceUnavailable
import net.minecraft.core.BlockPos
import net.minecraft.core.DefaultedRegistry
import net.minecraft.core.Registry
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.Registries
import net.minecraft.network.protocol.PacketFlow
import net.minecraft.resources.Identifier
import net.minecraft.tags.BlockTags
import net.minecraft.tags.ItemTags
import net.minecraft.tags.TagKey
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.Items
import java.util.Locale
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

private fun <T : Any> constructMap(
    registry: DefaultedRegistry<T>,
    tagKeys: Array<TagKey<T>>,
): Map<Identifier, Identifier> {
    val map = hashMapOf<Identifier, Identifier>()

    for (acceptedTag in tagKeys) {
        val get = registry.get(acceptedTag).getOrNull() ?: continue

        get.forEach {
            val itemId = registry.getKey(it.value())

            val prev = map.putIfAbsent(itemId, acceptedTag.location)
            if (prev != null) {
                logger.warn("Duplicate $itemId in ${acceptedTag.location} in $prev")

                return@forEach
            }
        }
    }

    return map
}

private inline fun <T : Any> Registry<T>.buildOutput(
    name: (Identifier, T) -> String,
    iconUrl: (Identifier) -> String? = { null },
): Map<String, RegistryItemOutput> {
    val obj = objectObjectHashMapOf<String, RegistryItemOutput>()
    for (item in this) {
        val id = this.getKey(item) ?: continue
        obj[id.toString()] = RegistryItemOutput(name(id, item), iconUrl(id))
    }
    return obj
}

@JvmRecord
private data class RegistryItemOutput(val name: String, val icon: String?)

// GET /api/v1/client/registry/:name
@Suppress("UNUSED_PARAMETER")
fun getRegistry(requestObject: RequestObject): FullHttpResponse {
    fun itemIconUrl(id: Identifier) =
        "${ClientInteropServer.url}/api/v1/client/resource/itemTexture?id=$id"
    fun effectTextureUrl(id: Identifier) =
        "${ClientInteropServer.url}/api/v1/client/resource/effectTexture?id=$id"

    val registryName = requestObject.params["name"]
        ?: return httpForbidden("Missing registry name parameter")

    return when (registryName.lowercase(Locale.ENGLISH)) {
        "blocks", "block" -> {
            BuiltInRegistries.BLOCK.buildOutput(
                name = { _, id -> id.name.string },
                iconUrl = ::itemIconUrl,
            )
        }

        "items", "item" -> {
            BuiltInRegistries.ITEM.buildOutput(
                name = { _, id -> id.name.string },
                iconUrl = ::itemIconUrl,
            )
        }

        "sounds", "sound_event" -> {
            val soundDiscId = BuiltInRegistries.ITEM.getKey(Items.MUSIC_DISC_13)
            val icon = itemIconUrl(soundDiscId)

            BuiltInRegistries.SOUND_EVENT.buildOutput(
                name =  { _, id -> id.location.toName() },
            ) { icon }
        }

        "mob_effect" -> {
            BuiltInRegistries.MOB_EFFECT.buildOutput(
                name =  { _, id -> id.displayName.string },
                iconUrl = ::effectTextureUrl,
            )
        }

        "enchantment" -> {
            val registry = Registries.ENCHANTMENT.getOrNull()
                ?: return httpServiceUnavailable("Registry not loaded")
            registry.buildOutput(name = { _, id -> id.description.string })
        }

        "c2s_packet" -> {
            packetRegistry[PacketFlow.SERVERBOUND]!!.associate {
                it.toString() to RegistryItemOutput(it.toName(), null)
            }
        }

        "s2c_packet" -> {
            packetRegistry[PacketFlow.CLIENTBOUND]!!.associate {
                it.toString() to RegistryItemOutput(it.toName(), null)
            }
        }

        "entity_type" -> {
            BuiltInRegistries.ENTITY_TYPE.buildOutput(name = { _, id -> id.description.string })
        }

        "screen_handler", "menu" -> {
            val converter = CaseFormat.LOWER_UNDERSCORE.converterTo(CaseFormat.UPPER_CAMEL)
            BuiltInRegistries.MENU.buildOutput(name = { id, _ -> converter.convert(id.toName())!! })
        }

        "client_module" -> {
            ModuleManager.associate {
                it.name to RegistryItemOutput(it.name, null)
            }
        }

        else -> return httpForbidden("Invalid registry name: $registryName")
    }.let { httpOk(it, interopGson) }
}


// GET /api/v1/client/registry/:name/groups
@Suppress("UNUSED_PARAMETER", "CognitiveComplexMethod")
fun getRegistryGroups(requestObject: RequestObject) = httpOk(JsonObject().apply {
    val registryName = requestObject.params["name"]
        ?: return httpForbidden("Missing registry name parameter")
    when (registryName.lowercase(Locale.ENGLISH)) {
        "items" -> {
            for ((k, v) in constructMap(BuiltInRegistries.ITEM, ACCEPTED_ITEM_TAGS)) {
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
            val world = mc.level ?: return httpForbidden("No world")

            BuiltInRegistries.BLOCK.forEach { block ->
                val pickStack = block.getCloneItemStack(world, BlockPos.ZERO, block.defaultBlockState(), false)
                val id = BuiltInRegistries.BLOCK.getKey(block)

                when (val item = pickStack.item) {
                    is BlockItem -> {
                        if (item.block != block) {
                            parentMap[id] = BuiltInRegistries.BLOCK.getKey(item.block)
                        }
                    }

                    else -> {
                        if (!pickStack.isEmpty) {
                            logger.warn("Invalid pick stack for $id: $pickStack")
                        }
                    }
                }
            }

            val constructedMap = constructMap(BuiltInRegistries.BLOCK, ACCEPTED_BLOCK_TAGS)

            BuiltInRegistries.BLOCK.forEach { block ->
                val id = BuiltInRegistries.BLOCK.getKey(block)

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
