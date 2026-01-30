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
package net.ccbluex.liquidbounce.features.module.modules.world.autofarm

import net.ccbluex.fastutil.objectHashSetOf
import net.ccbluex.liquidbounce.config.types.group.ToggleableValueGroup
import net.ccbluex.liquidbounce.event.events.NotificationEvent
import net.ccbluex.liquidbounce.utils.client.notification
import net.ccbluex.liquidbounce.utils.collection.Filter
import net.ccbluex.liquidbounce.utils.collection.itemSortedSetOf
import net.ccbluex.liquidbounce.utils.inventory.Slots
import net.ccbluex.liquidbounce.utils.inventory.hasInventorySpace
import net.ccbluex.liquidbounce.utils.math.sq
import net.ccbluex.liquidbounce.utils.navigation.NavigationBaseValueGroup
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.item.BoneMealItem
import net.minecraft.world.phys.Vec3

object AutoFarmAutoWalk : NavigationBaseValueGroup<Vec3?>(ModuleAutoFarm, "AutoWalk", false) {

    private val minimumDistance by float("MinimumDistance", 2f, 1f..4f)

    // Makes the player move to farmland blocks where there is a need for crop replacement
    private val toPlant by boolean("ToPlant", true, aliases = listOf("ToPlace"))

    private val toItems = object : ToggleableValueGroup(this, "ToItems", true) {
        private val range by float("Range", 20f, 8f..64f).onChanged {
            rangeSquared = it.sq()
        }

        private val items by items("Items", itemSortedSetOf())
        private val filter by enumChoice("Filter", Filter.BLACKLIST)

        fun shouldPickUp(itemEntity: ItemEntity): Boolean {
            return filter(itemEntity.item.item, items)
        }

        var rangeSquared: Float = range.sq()
            private set
    }

    init {
        tree(toItems)
    }

    private var invHadSpace = true

    var walkTarget: Vec3? = null
        private set

    private fun collectAllowedStates(): Set<AutoFarmTrackedState> {
        // we should only walk to farmland/soulsand blocks if we have plantable items
        if (!toPlant) return setOf(AutoFarmTrackedState.ReadyForHarvest)

        // we should always walk to blocks we want to destroy because we can do so even without any items
        val allowedStates = objectHashSetOf<AutoFarmTrackedState>()

        allowedStates.add(AutoFarmTrackedState.ReadyForHarvest)

        for (item in Slots.OffhandWithHotbar.items) {
            AutoFarmTrackedState.Plantable.entries.filterTo(allowedStates) { it.items.contains(item) }

            if (item is BoneMealItem && ModuleAutoFarm.AutoUseBoneMeal.enabled) {
                allowedStates.add(AutoFarmTrackedState.Bonemealable)
            }
        }
        return allowedStates
    }

    private fun findWalkToBlock(): Vec3? {
        if (AutoFarmBlockTracker.isEmpty()) return null

        val allowedStates = collectAllowedStates()

        val closestBlockPos = AutoFarmBlockTracker.iterate().mapNotNull { (pos, state) ->
            if (state in allowedStates) pos.center else null
        }.minByOrNull(player::distanceToSqr)

        return closestBlockPos
    }

    private fun findWalkTarget(invHasSpace: Boolean): Vec3? {
        val blockTarget = findWalkToBlock()

        if (toItems.enabled && invHasSpace) {
            val playerPos = player.position()
            val itemTarget = findWalkToItem() ?: return blockTarget
            blockTarget ?: return itemTarget

            val blockTargetDistSq = blockTarget.distanceToSqr(playerPos)
            val itemTargetDistSq = itemTarget.distanceToSqr(playerPos)
            return if (blockTargetDistSq < itemTargetDistSq) blockTarget else itemTarget
        } else {
            return blockTarget
        }
    }

    private fun findWalkToItem(): Vec3? = world.entitiesForRendering().filter {
        it is ItemEntity && toItems.shouldPickUp(it) && it.distanceToSqr(player) < toItems.rangeSquared
    }.minByOrNull { it.distanceToSqr(player) }?.position()

    @Suppress("EmptyFunctionBlock")
    override fun createNavigationContext(): Vec3? {
        val invHasSpace = hasInventorySpace()
        if (!invHasSpace && invHadSpace && toItems.enabled) {
            notification("Inventory is Full", "AutoFarm will no longer ", NotificationEvent.Severity.ERROR)
            return null
        }
        invHadSpace = invHasSpace

        return findWalkTarget(invHasSpace)
    }

    override fun calculateGoalPosition(context: Vec3?): Vec3? {
        val target = ModuleAutoFarm.currentTarget?.center ?: context
        if (target != null && player.distanceToSqr(target) < minimumDistance.sq()) {
            this.walkTarget = null
            return null
        }

        return target.also { this.walkTarget = it }
    }

}
