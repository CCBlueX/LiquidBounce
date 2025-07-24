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
import net.ccbluex.liquidbounce.event.events.MovementInputEvent
import net.ccbluex.liquidbounce.event.events.NotificationEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.aiming.data.Rotation
import net.ccbluex.liquidbounce.utils.client.notification
import net.ccbluex.liquidbounce.utils.inventory.Slots
import net.ccbluex.liquidbounce.utils.inventory.hasInventorySpace
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.ccbluex.liquidbounce.utils.math.sq
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.entity.ItemEntity
import net.minecraft.util.math.Vec3d
import java.util.EnumSet

object AutoFarmAutoWalk : ToggleableConfigurable(ModuleAutoFarm, "AutoWalk", false) {

    // Makes the player move to farmland blocks where there is a need for crop replacement
    private val toPlace by boolean("ToPlace", true)

    private val toItems = object : ToggleableConfigurable(this, "ToItems", true) {
        private val range by float("Range", 20f, 8f..64f).onChanged {
            rangeSquared = it.sq()
        }

        var rangeSquared: Float = range.sq()
    }

    private val autoJump by boolean("AutoJump", false)

    init {
        tree(toItems)
    }

    private var invHadSpace = true

    var walkTarget: Vec3d? = null

    private fun findWalkToItem() = world.entities.filter {
        it is ItemEntity && it.squaredDistanceTo(player) < toItems.rangeSquared
    }.minByOrNull { it.squaredDistanceTo(player) }?.pos

    fun updateWalkTarget(): Boolean {
        if (!enabled) return false

        val invHasSpace = hasInventorySpace()
        if (!invHasSpace && invHadSpace && toItems.enabled) {
            notification("Inventory is Full", "autoFarm wont walk to items", NotificationEvent.Severity.ERROR)
        }
        invHadSpace = invHasSpace

        walkTarget = if (toItems.enabled && invHasSpace) {
            arrayOf(findWalkToBlock(), findWalkToItem()).minByOrNull {
                it?.squaredDistanceTo(player.pos) ?: Double.MAX_VALUE
            }
        } else {
            findWalkToBlock()
        }

        val target = walkTarget ?: return false

        RotationManager.setRotationTarget(
            Rotation.lookingAt(point = target, from = player.eyePos),
            configurable = ModuleAutoFarm.rotations,
            priority = Priority.IMPORTANT_FOR_USAGE_1,
            provider = ModuleAutoFarm
        )
        return true
    }

    private fun findWalkToBlock(): Vec3d? {
        if (AutoFarmBlockTracker.isEmpty()) return null

        val allowedItems = EnumSet.of(AutoFarmTrackedStates.Destroy)
        // 1. true: we should always walk to blocks we want to destroy because we can do so even without any items
        // 2. false: we should only walk to farmland blocks if we got the needed items
        // 3. false: same as 2. only go if we got the needed items for soulsand (netherwarts)
        if (toPlace) {
            for (item in Slots.OffhandWithHotbar.items) {
                when (item) {
                    in ModuleAutoFarm.itemsForFarmland -> allowedItems.add(AutoFarmTrackedStates.Farmland)
                    in ModuleAutoFarm.itemsForSoulsand -> allowedItems.add(AutoFarmTrackedStates.Soulsand)
                }
            }
        }

        val closestBlock = AutoFarmBlockTracker.iterate().mapNotNull { (pos, state) ->
            if (state in allowedItems) pos.toCenterPos() else null
        }.minByOrNull(player::squaredDistanceTo)

        return closestBlock
    }

    fun stopWalk() {
        walkTarget = null
    }

    private fun shouldWalk() = (walkTarget != null && mc.currentScreen !is HandledScreen<*>)

    @Suppress("unused")
    private val horizontalMovementHandling = handler<MovementInputEvent> { event ->
        if (!shouldWalk()) {
            return@handler
        }

        event.directionalInput = event.directionalInput.copy(forwards = true)
        player.isSprinting = true
    }

    @Suppress("unused")
    private val verticalMovementHandling = handler<MovementInputEvent> { event ->
        if (!shouldWalk()) return@handler

        // We want to swim up in water, so we don't drown and can move onwards
        if (player.isTouchingWater) {
            event.jump = true
        }

        // Auto jump
        if (autoJump && player.horizontalCollision && walkTarget!!.y > player.y) {
            event.jump = true
        }
    }
}
