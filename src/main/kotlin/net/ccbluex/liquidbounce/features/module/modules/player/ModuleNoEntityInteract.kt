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

package net.ccbluex.liquidbounce.features.module.modules.player

import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.utils.collection.Filter
import net.ccbluex.liquidbounce.utils.item.isMiningTool
import net.minecraft.entity.EntityType
import net.minecraft.item.Item
import net.minecraft.item.Items
import net.minecraft.registry.Registries
import net.minecraft.util.hit.EntityHitResult

/**
 * Skip crosshair entity targets.
 */
object ModuleNoEntityInteract : ClientModule("NoEntityInteract", Category.PLAYER) {

    private fun defaultEntityTypes(): MutableSet<EntityType<*>> {
        return hashSetOf(EntityType.VILLAGER, EntityType.ARMOR_STAND)
    }

    private fun defaultHoldingItems(): MutableSet<Item> {
        val set = hashSetOf(Items.AIR, Items.SHEARS, Items.TNT, Items.WATER_BUCKET, Items.LAVA_BUCKET, Items.COBWEB)
        Registries.ITEM.filterTo(set) { it.defaultStack.isMiningTool }
        return set
    }

    private val entityTypeFilter by enumChoice("EntityTypeFilter", Filter.BLACKLIST)
    private val entityTypes by entityTypes("EntityTypes", defaultEntityTypes())

    private val holdingItemFilter by enumChoice("HoldingItemFilter", Filter.WHITELIST)
    private val holdingItems by items("HoldingItems", defaultHoldingItems())

    fun test(entity: EntityHitResult): Boolean {
        return !running ||
            entityTypeFilter(entity.entity, entityTypes) &&
            holdingItemFilter(player.mainHandStack.item, holdingItems)
    }

}
