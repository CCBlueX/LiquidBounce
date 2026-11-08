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

package net.ccbluex.liquidbounce.features.module.modules.player

import net.ccbluex.fastutil.objectRBTreeSetOf
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.utils.collection.Filter
import net.ccbluex.liquidbounce.utils.collection.asComparator
import net.ccbluex.liquidbounce.utils.collection.itemSortedSetOf
import net.ccbluex.liquidbounce.utils.combat.shouldBeAttacked
import net.ccbluex.liquidbounce.utils.item.isMiningTool
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.EntityTypes
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.ItemStackTemplate
import net.minecraft.world.item.Items
import net.minecraft.world.phys.EntityHitResult
import java.util.SequencedSet

/**
 * Skip crosshair entity targets.
 */
object ModuleNoEntityInteract : ClientModule("NoEntityInteract", ModuleCategories.PLAYER) {

    private fun defaultEntityTypes(): SequencedSet<EntityType<*>> {
        return objectRBTreeSetOf(
            BuiltInRegistries.ENTITY_TYPE.asComparator(),
            EntityTypes.VILLAGER, EntityTypes.ARMOR_STAND
        )
    }

    private fun defaultHoldingItems(): SequencedSet<Item> {
        val set = itemSortedSetOf(
            Items.AIR, Items.SHEARS, Items.TNT, Items.WATER_BUCKET, Items.LAVA_BUCKET, Items.COBWEB
        )
        BuiltInRegistries.ITEM.filterTo(set) {
            it !in set && ItemStackTemplate(it).isMiningTool
        }
        return set
    }

    private val entityTypeFilter by enumChoice("EntityTypeFilter", Filter.BLACKLIST)
    private val entityTypes by entityTypes("EntityTypes", defaultEntityTypes())

    private val holdingItemFilter by enumChoice("HoldingItemFilter", Filter.WHITELIST)
    private val holdingItems by items("HoldingItems", defaultHoldingItems())

    /**
     * Leaves the crosshair and attacks alone and only skips the use interaction.
     */
    private val onlyUse by boolean("OnlyUse", false)
    private val offhand by boolean("Offhand", false)
    private val onlyEnemies by boolean("OnlyEnemies", false)

    /**
     * Whether the crosshair may target the entity.
     */
    fun test(entity: EntityHitResult): Boolean {
        if (!running || onlyUse) {
            return true
        }

        val target = entity.entity
        return allows(target, player.mainHandItem) && (!offhand || allows(target, player.offhandItem))
    }

    /**
     * Whether using [hand] on [entity] reaches the entity, see [onlyUse].
     */
    fun allowsUse(entity: Entity, hand: InteractionHand): Boolean {
        if (!running || !onlyUse || hand == InteractionHand.OFF_HAND && !offhand) {
            return true
        }

        return allows(entity, player.getItemInHand(hand))
    }

    private fun allows(entity: Entity, heldItem: ItemStack): Boolean {
        if (onlyEnemies && !entity.shouldBeAttacked()) {
            return true
        }

        return entityTypeFilter(entity, entityTypes) && holdingItemFilter(heldItem.item, holdingItems)
    }

}
