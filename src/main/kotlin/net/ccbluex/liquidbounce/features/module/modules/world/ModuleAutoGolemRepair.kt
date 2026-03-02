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

package net.ccbluex.liquidbounce.features.module.modules.world

import net.ccbluex.fastutil.filterIsInstance
import net.ccbluex.fastutil.weightedMinByOrNullAtMost
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.event.waitTicks
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.utils.block.SwingMode
import net.ccbluex.liquidbounce.utils.client.SilentHotbar
import net.ccbluex.liquidbounce.utils.entity.interactEntity
import net.ccbluex.liquidbounce.utils.entity.squaredBoxedDistanceTo
import net.ccbluex.liquidbounce.utils.inventory.Slots
import net.minecraft.world.entity.animal.golem.IronGolem
import net.minecraft.world.item.Items

/**
 * Automatically repairs nearby damaged iron golems using iron ingots.
 */
object ModuleAutoGolemRepair : ClientModule("AutoGolemRepair", ModuleCategories.WORLD) {

    private val range by float("Range", 4.5f, 1f..6f)
    private val healthThreshold by float("HealthThreshold", 75f, 1f..100f, "%")
    private val delay by intRange("Delay", 4..4, 0..40, "ticks")
    private val slotResetDelay by int("SlotResetDelay", 2, 0..20, "ticks")
    private val swingMode by enumChoice("SwingMode", SwingMode.DO_NOT_HIDE)

    @Suppress("unused")
    private val repeatable = tickHandler {
        if (player.isUsingItem || mc.screen != null) {
            return@tickHandler
        }

        val ingotSlot = Slots.OffhandWithHotbar.findSlot(Items.IRON_INGOT) ?: return@tickHandler
        val maxRange = minOf(range.toDouble(), player.entityInteractionRange())
        val maxRangeSq = maxRange * maxRange
        val minHealthRatio = (healthThreshold / 100f).coerceIn(0f, 1f)
        val eyePosition = player.eyePosition

        val target = world.entitiesForRendering()
            .filterIsInstance<IronGolem> { entity ->
                player.hasLineOfSight(entity) &&
                    entity.health < entity.maxHealth * minHealthRatio
            }.weightedMinByOrNullAtMost(maxRangeSq) {
                it.squaredBoxedDistanceTo(eyePosition)
            }
            ?: return@tickHandler

        SilentHotbar.selectSlotSilently(this, ingotSlot, slotResetDelay)

        interactEntity(
            target,
            swingMode = swingMode,
        )

        waitTicks(delay.random())
    }

    override fun onDisabled() {
        SilentHotbar.resetSlot(this)
    }

}
