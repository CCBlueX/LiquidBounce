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
package net.ccbluex.liquidbounce.features.module.modules.world.autofarm

import net.ccbluex.liquidbounce.config.types.nesting.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.events.NotificationEvent
import net.ccbluex.liquidbounce.utils.client.notification
import net.ccbluex.liquidbounce.utils.collection.Filter
import net.ccbluex.liquidbounce.utils.inventory.Slots
import net.ccbluex.liquidbounce.utils.inventory.hasInventorySpace
import net.ccbluex.liquidbounce.utils.math.sq
import net.ccbluex.liquidbounce.utils.navigation.NavigationBaseConfigurable
import net.minecraft.entity.ItemEntity
import net.minecraft.item.Items
import net.minecraft.util.math.Vec3d
import java.util.*

object AutoFarmAutoWalk : NavigationBaseConfigurable<Vec3d?>(ModuleAutoFarm, "AutoWalk", false) {

    private val minimumDistance by float("MinimumDistance", 2f, 1f..4f)

    // Makes the player move to farmland blocks where there is a need for crop replacement
    private val toPlace by boolean("ToPlace", true)

    private val toItems = object : ToggleableConfigurable(this, "ToItems", true) {
        private val range by float("Range", 20f, 8f..64f).onChanged {
            rangeSquared = it.sq()
        }

        private val items by items("Items", hashSetOf())
        private val filter by enumChoice("Filter", Filter.BLACKLIST)

        fun shouldPickUp(itemEntity: ItemEntity): Boolean {
            return filter(itemEntity.stack.item, items)
        }

        var rangeSquared: Float = range.sq()
            private set
    }

    init {
        tree(toItems)
    }

    private var invHadSpace = true

    var walkTarget: Vec3d? = null
        private set

    private fun collectAllowedStates(): Set<AutoFarmTrackedState> {
        // we should always walk to blocks we want to destroy because we can do so even without any items
        val allowedStates = EnumSet.of(AutoFarmTrackedState.SHOULD_BE_DESTROYED)

        // we should only walk to farmland/soulsand blocks if we have plantable items
        if (!toPlace) return allowedStates

        for (item in Slots.OffhandWithHotbar.items) {
            when (item) {
                in itemsForFarmland -> allowedStates.add(AutoFarmTrackedState.FARMLAND)
                in itemsForSoulSand -> allowedStates.add(AutoFarmTrackedState.SOUL_SAND)
                Items.BONE_MEAL -> if (ModuleAutoFarm.AutoUseBoneMeal.enabled) {
                    allowedStates.add(AutoFarmTrackedState.CAN_USE_BONE_MEAL)
                }
            }
        }
        return allowedStates
    }

    private fun findWalkToBlock(): Vec3d? {
        if (AutoFarmBlockTracker.isEmpty()) return null

        val allowedStates = collectAllowedStates()

        val closestBlockPos = AutoFarmBlockTracker.iterate().mapNotNull { (pos, state) ->
            if (state in allowedStates) pos.toCenterPos() else null
        }.minByOrNull(player::squaredDistanceTo)

        return closestBlockPos
    }

    private fun findWalkTarget(invHasSpace: Boolean): Vec3d? {
        val blockTarget = findWalkToBlock()

        if (toItems.enabled && invHasSpace) {
            val playerPos = player.pos
            val itemTarget = findWalkToItem() ?: return blockTarget
            blockTarget ?: return itemTarget

            val blockTargetDistSq = blockTarget.squaredDistanceTo(playerPos)
            val itemTargetDistSq = itemTarget.squaredDistanceTo(playerPos)
            return if (blockTargetDistSq < itemTargetDistSq) blockTarget else itemTarget
        } else {
            return blockTarget
        }
    }

    private fun findWalkToItem(): Vec3d? = world.entities.filter {
        it is ItemEntity && toItems.shouldPickUp(it) && it.squaredDistanceTo(player) < toItems.rangeSquared
    }.minByOrNull { it.squaredDistanceTo(player) }?.pos

    @Suppress("EmptyFunctionBlock")
    override fun createNavigationContext(): Vec3d? {
        val invHasSpace = hasInventorySpace()
        if (!invHasSpace && invHadSpace && toItems.enabled) {
            notification("Inventory is Full", "AutoFarm will no longer ", NotificationEvent.Severity.ERROR)
            return null
        }
        invHadSpace = invHasSpace

        return findWalkTarget(invHasSpace)
    }

    override fun calculateGoalPosition(context: Vec3d?): Vec3d? {
        val target = ModuleAutoFarm.currentTarget?.toCenterPos() ?: context
        if (target != null && player.squaredDistanceTo(target) < minimumDistance.sq()) {
            this.walkTarget = null
            return null
        }

        return target.also { this.walkTarget = it }
    }

}
