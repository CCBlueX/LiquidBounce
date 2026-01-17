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
import net.ccbluex.liquidbounce.utils.item.isMiningTool
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.world.entity.EntityType
import net.minecraft.world.item.Item
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
            EntityType.VILLAGER, EntityType.ARMOR_STAND
        )
    }

    private fun defaultHoldingItems(): SequencedSet<Item> {
        val set = itemSortedSetOf(
            Items.AIR, Items.SHEARS, Items.TNT, Items.WATER_BUCKET, Items.LAVA_BUCKET, Items.COBWEB
        )
        BuiltInRegistries.ITEM.filterTo(set) { it.defaultInstance.isMiningTool }
        return set
    }

    private val entityTypeFilter by enumChoice("EntityTypeFilter", Filter.BLACKLIST)
    private val entityTypes by entityTypes("EntityTypes", defaultEntityTypes())

    private val holdingItemFilter by enumChoice("HoldingItemFilter", Filter.WHITELIST)
    private val holdingItems by items("HoldingItems", defaultHoldingItems())

    fun test(entity: EntityHitResult): Boolean {
        return !running ||
            entityTypeFilter(entity.entity, entityTypes) &&
            holdingItemFilter(player.mainHandItem.item, holdingItems)
    }

}
