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
package net.ccbluex.liquidbounce.features.module.modules.player.antivoid.mode

import net.ccbluex.liquidbounce.config.types.group.ModeValueGroup
import net.ccbluex.liquidbounce.features.module.modules.player.antivoid.ModuleAntiVoid
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleDebug.debugParameter
import net.ccbluex.liquidbounce.utils.collection.itemSortedSetOf
import net.ccbluex.liquidbounce.utils.entity.PlayerSimulationCache
import net.ccbluex.liquidbounce.utils.inventory.Slots
import net.ccbluex.liquidbounce.utils.inventory.findClosestSlot
import net.ccbluex.liquidbounce.utils.inventory.useHotbarSlotOrOffhand
import net.minecraft.world.item.Items

/**
 * AntiVoid UseItem mode.
 *
 * When the player is falling into the void with no way to recover, a configured item
 * from the hotbar is used. This is useful on servers with power-ups where using
 * specific items can prevent void death.
 *
 * The trigger conditions are:
 * 1. the player is likely falling.
 * 2. the player's y motion is fast enough to be below [yMotionThreshold].
 * 3. the simulation predicts the player will not reach solid ground within 40 ticks.
 */
object AntiVoidUseItemMode : AntiVoidMode("UseItem") {

    override val parent: ModeValueGroup<*>
        get() = ModuleAntiVoid.mode

    private val yMotionThreshold by float("YMotionThreshold", -0.5f, -1.0f..-0.1f)

    private val slotResetDelay by intRange("SlotResetDelay", 4..6, 0..40, "ticks")

    private val items by items("Items", itemSortedSetOf(Items.MAGMA_CREAM))
    private const val SIMULATION_TICKS = 40

    override fun rescue(): Boolean {
        val motionY = player.deltaMovement.y
        debugParameter("MotionY") { "%.3f".format(motionY) }
        debugParameter("YThreshold") { "%.3f".format(yMotionThreshold.toDouble()) }

        if (motionY >= yMotionThreshold) {
            return false
        }

        val simulation = PlayerSimulationCache.getSimulationForLocalPlayer()
        val canReachGround = simulation.getSnapshotsBetween(1..SIMULATION_TICKS).any { it.onGround }

        debugParameter("CanReachGround") { canReachGround }

        if (canReachGround) {
            return false
        }

        val slot = Slots.OffhandWithHotbar.findClosestSlot(items) ?: return false

        debugParameter("UsedItem") { slot.itemStack.hoverName.string }

        useHotbarSlotOrOffhand(slot, ticksUntilReset = slotResetDelay.random())
        return true
    }

}
