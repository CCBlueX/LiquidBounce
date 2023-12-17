/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2023 CCBlueX
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
package net.ccbluex.liquidbounce.features.tabs

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.config.util.decode
import net.ccbluex.liquidbounce.utils.client.asText
import net.ccbluex.liquidbounce.utils.client.logger
import net.ccbluex.liquidbounce.utils.io.HttpClient
import net.ccbluex.liquidbounce.utils.item.createItem
import net.minecraft.block.Blocks
import net.minecraft.enchantment.Enchantments
import net.minecraft.entity.effect.StatusEffectInstance
import net.minecraft.entity.effect.StatusEffects
import net.minecraft.item.ItemGroup
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.potion.PotionUtil
import net.minecraft.registry.Registries
import net.minecraft.registry.Registry
import net.minecraft.util.Formatting
import net.minecraft.util.Identifier
import java.util.*

/**
 * LiquidBounce Creative Tabs with useful items and blocks
 *
 * @author kawaiinekololis (@team CCBlueX)
 * @depends FabricAPI (for page buttons)
 */
object Tabs {
    private var setup = false

    /**
     * Since 1.20 we need to set this up at a more precise timing than just when the client starts.
     */
    fun setup() {
        if (!setup) {
//            setupSpecial()
//            setupExploits()
//            setupHeads()

            setup = true
        }
    }

    /**
     * Special item group is useful to get blocks or items which you are not able to get without give command
     */
    private fun setupSpecial() =  LiquidsItemGroup(
        "Special",
        icon = {
            ItemStack(Blocks.COMMAND_BLOCK).apply {
                addEnchantment(Enchantments.SOUL_SPEED, 1337)
            }
        },
        items = {
            it.add(ItemStack(Blocks.COMMAND_BLOCK))
            it.add(ItemStack(Blocks.CHAIN_COMMAND_BLOCK))
            it.add(ItemStack(Blocks.REPEATING_COMMAND_BLOCK))
            it.add(ItemStack(Items.COMMAND_BLOCK_MINECART))
            it.add(ItemStack(Blocks.END_PORTAL_FRAME))
            it.add(ItemStack(Blocks.DRAGON_EGG))
            it.add(ItemStack(Blocks.BARRIER))
            it.add(ItemStack(Blocks.JIGSAW))
            it.add(ItemStack(Blocks.STRUCTURE_BLOCK))
            it.add(ItemStack(Blocks.STRUCTURE_VOID))
            it.add(ItemStack(Blocks.SPAWNER))
            it.add(ItemStack(Items.DEBUG_STICK))
        }
    ).create()

    /**
     * Exploits item group allows you to get items which are able to exploit bugs (like crash exploits or render issues)
     */
    private fun setupExploits() = LiquidsItemGroup(
        "Exploits",
        icon = { ItemStack(Items.LINGERING_POTION) },
        items = {
            // TODO: Add exploits
            // it.add(createItem("spawner{BlockEntityTag:{EntityId:\"Painting\"}}", 1).setCustomName("§8Test §7| §cmc1.8-mc1.16.4".asText()))

            it.add(
                PotionUtil.setCustomPotionEffects(
                    ItemStack(Items.SPLASH_POTION)
                        .setCustomName(
                            "".asText()
                                .styled { s -> s.withBold(true) }
                                .append(
                                    "Troll".asText()
                                        .styled { s -> s.withColor(Formatting.RED) }
                                )
                                .append(
                                    "Potion".asText()
                                        .styled { s -> s.withColor(Formatting.GOLD) }
                                )
                        ),
                    Registries.STATUS_EFFECT.map { e ->
                        StatusEffectInstance(e, Int.MAX_VALUE, 127)
                    }
                )
            )

            it.add(
                PotionUtil.setCustomPotionEffects(
                    ItemStack(Items.SPLASH_POTION)
                        .setCustomName(
                            "".asText()
                                .styled { s -> s.withBold(true) }
                                .append(
                                    "Kill".asText()
                                        .styled { s -> s.withColor(Formatting.RED) }
                                )
                                .append(
                                    "Potion".asText()
                                        .styled { s -> s.withColor(Formatting.GOLD) }
                                )
                        ),
                    listOf(
                        StatusEffectInstance(StatusEffects.INSTANT_HEALTH, 0, 125),
                        StatusEffectInstance(StatusEffects.INSTANT_DAMAGE, 0, 125)
                    )
                )
            )
        }
    ).create()

    /**
     * Heads item group allows you to decorate your world with different heads
     */
    private class Head(val name: String, val uuid: UUID, val value: String)
    private class HeadsService(val enabled: Boolean, val url: String)

    private var headsDb = runCatching {
        logger.info("Loading heads...")
        // Load head service from cloud
        //  Makes it possible to disable service or change domain in case of an emergency
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

    private fun setupHeads() = LiquidsItemGroup(
        "Heads",
        icon = { ItemStack(Items.SKELETON_SKULL) },
        items = {
            it.addAll(headsDb
                .distinctBy { it.name }
                .map { head ->
                    createItem("minecraft:player_head{display:{Name:\"{\\\"text\\\":\\\"${head.name}\\\"}\"},SkullOwner:{Id:[I;0,0,0,0],Properties:{textures:[{Value:\"${head.value}\"}]}}}")
                })
        }
    ).create()

}

/**
 * A item group from the client
 */
open class LiquidsItemGroup(
    val plainName: String,
    val icon: () -> ItemStack,
    val items: (items: ItemGroup.Entries) -> Unit
) {

    // Create item group and assign to minecraft groups
    fun create(): ItemGroup {
        // Expand array
        val itemGroup = ItemGroup.create(ItemGroup.Row.TOP, -1)
            .displayName(plainName.asText())
            .icon(icon)
            .entries { displayContext, entries ->
                runCatching {
                    items(entries)
                }.onFailure {
                    logger.error("Unable to create item group $plainName", it)
                }
            }
            .build()

        // Add tab to creative inventory
        Registry.register(Registries.ITEM_GROUP, Identifier("liquidbounce", plainName.lowercase()), itemGroup)

        return itemGroup
    }

}
