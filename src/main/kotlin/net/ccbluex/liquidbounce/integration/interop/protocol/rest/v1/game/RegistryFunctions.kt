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
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
import net.ccbluex.liquidbounce.features.module.ModuleManager
import net.ccbluex.liquidbounce.integration.interop.ClientInteropServer
import net.ccbluex.liquidbounce.integration.interop.forbidden
import net.ccbluex.liquidbounce.integration.interop.serviceUnavailable
import net.ccbluex.liquidbounce.utils.client.logger
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.client.toName
import net.ccbluex.liquidbounce.utils.item.getOrNull
import net.ccbluex.liquidbounce.utils.network.packetRegistry
import net.minecraft.core.BlockPos
import net.minecraft.core.DefaultedRegistry
import net.minecraft.core.Registry
import net.minecraft.core.component.DataComponents
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.Registries
import net.minecraft.network.protocol.PacketFlow
import net.minecraft.resources.Identifier
import net.minecraft.tags.TagKey
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.Item
import net.minecraft.world.item.Items
import net.minecraft.world.level.block.Block
import java.util.Locale
import kotlin.jvm.optionals.getOrNull

private fun itemTag(name: String): TagKey<Item> =
    TagKey.create(Registries.ITEM, Identifier.withDefaultNamespace(name))

private fun blockTag(name: String): TagKey<Block> =
    TagKey.create(Registries.BLOCK, Identifier.withDefaultNamespace(name))

private val ACCEPTED_ITEM_TAGS =
    arrayOf(
        itemTag("wool"),
        itemTag("planks"),
        itemTag("stone_bricks"),
        itemTag("buttons"),
        itemTag("wool_carpets"),
        itemTag("fence_gates"),
        itemTag("wooden_pressure_plates"),
        itemTag("doors"),
        itemTag("logs"),
        itemTag("banners"),
        itemTag("sand"),
        itemTag("stairs"),
        itemTag("slabs"),
        itemTag("walls"),
        itemTag("anvil"),
        itemTag("rails"),
        itemTag("small_flowers"),
        itemTag("saplings"),
        itemTag("leaves"),
        itemTag("trapdoors"),
        itemTag("beds"),
        itemTag("fences"),
        itemTag("gold_ores"),
        itemTag("iron_ores"),
        itemTag("diamond_ores"),
        itemTag("redstone_ores"),
        itemTag("lapis_ores"),
        itemTag("coal_ores"),
        itemTag("emerald_ores"),
        itemTag("copper_ores"),
        itemTag("candles"),
        itemTag("dirt"),
        itemTag("terracotta"),
        itemTag("boats"),
        itemTag("fishes"),
        itemTag("signs"),
        itemTag("creeper_drop_music_discs"),
        itemTag("coals"),
        itemTag("arrows"),
        itemTag("compasses"),
        itemTag("trim_materials"),
        itemTag("swords"),
        itemTag("axes"),
        itemTag("hoes"),
        itemTag("pickaxes"),
        itemTag("shovels"),
    )

private val ACCEPTED_BLOCK_TAGS =
    arrayOf(
        blockTag("wool"),
        blockTag("planks"),
        blockTag("stone_bricks"),
        blockTag("buttons"),
        blockTag("wool_carpets"),
        blockTag("pressure_plates"),
        blockTag("doors"),
        blockTag("flowers"),
        blockTag("saplings"),
        blockTag("logs"),
        blockTag("banners"),
        blockTag("sand"),
        blockTag("stairs"),
        blockTag("slabs"),
        blockTag("walls"),
        blockTag("anvil"),
        blockTag("rails"),
        blockTag("leaves"),
        blockTag("trapdoors"),
        blockTag("beds"),
        blockTag("fences"),
        blockTag("gold_ores"),
        blockTag("iron_ores"),
        blockTag("diamond_ores"),
        blockTag("redstone_ores"),
        blockTag("lapis_ores"),
        blockTag("coal_ores"),
        blockTag("emerald_ores"),
        blockTag("copper_ores"),
        blockTag("candles"),
        blockTag("dirt"),
        blockTag("terracotta"),
        blockTag("flower_pots"),
        blockTag("ice"),
        blockTag("corals"),
        blockTag("all_signs"),
        blockTag("beehives"),
        blockTag("crops"),
        blockTag("portals"),
        blockTag("fire"),
        blockTag("nylium"),
        blockTag("shulker_boxes"),
        blockTag("campfires"),
        blockTag("fence_gates"),
        blockTag("cauldrons"),
        blockTag("snow"),
    )

private fun <T : Any> constructMap(
    registry: DefaultedRegistry<T>,
    tagKeys: Array<TagKey<T>>,
): Map<Identifier, Identifier> {
    val map = Object2ObjectOpenHashMap<Identifier, Identifier>()

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
    val obj = Object2ObjectOpenHashMap<String, RegistryItemOutput>(this.size())
    for (item in this) {
        val id = this.getKey(item) ?: continue
        obj[id.toString()] = RegistryItemOutput(name(id, item), iconUrl(id))
    }
    return obj
}

@JvmRecord
private data class RegistryItemOutput(val name: String, val icon: String?)

// GET /api/v1/client/registry/{name}
private fun Route.getRegistry() = get {
    fun itemIconUrl(id: Identifier) =
        "${ClientInteropServer.url}/api/v1/client/resource/itemTexture?id=$id"
    fun effectTextureUrl(id: Identifier) =
        "${ClientInteropServer.url}/api/v1/client/resource/effectTexture?id=$id"

    val registryName = call.parameters["name"]
        ?: call.forbidden("Missing registry name parameter")

    val result = when (registryName.lowercase(Locale.ENGLISH)) {
        "blocks", "block" -> {
            BuiltInRegistries.BLOCK.buildOutput(
                name = { _, id -> id.name.string },
                iconUrl = ::itemIconUrl,
            )
        }

        "items", "item" -> {
            BuiltInRegistries.ITEM.buildOutput(
                name = { id, item -> item.components()[DataComponents.ITEM_NAME]?.string ?: id.toString() },
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
                ?: call.serviceUnavailable("Registry not loaded")
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

        else -> call.forbidden("Invalid registry name: $registryName")
    }

    call.respond(result)
}


// GET /api/v1/client/registry/{name}/groups
@Suppress("CognitiveComplexMethod")
private fun Route.getRegistryGroups() = get("/groups") {
    call.respond(JsonObject().apply {
        val registryName = call.parameters["name"]
            ?: call.forbidden("Missing registry name parameter")
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
                val world = mc.level ?: call.forbidden("No world")

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

            else -> call.forbidden("Invalid registry name: $registryName")
        }
    })
}

internal fun Route.registryRoutes() = route("/registry/{name}") {
    getRegistry()
    getRegistryGroups()
}
