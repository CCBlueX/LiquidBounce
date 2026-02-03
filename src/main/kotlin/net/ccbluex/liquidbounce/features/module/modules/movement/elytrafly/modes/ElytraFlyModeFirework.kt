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
package net.ccbluex.liquidbounce.features.module.modules.movement.elytrafly.modes

import net.ccbluex.liquidbounce.additions.shooter
import net.ccbluex.liquidbounce.config.types.group.ToggleableValueGroup
import net.ccbluex.liquidbounce.event.events.GameTickEvent
import net.ccbluex.liquidbounce.event.events.ScheduleInventoryActionEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.utils.inventory.HotbarItemSlot
import net.ccbluex.liquidbounce.utils.inventory.InventoryAction
import net.ccbluex.liquidbounce.utils.inventory.OffHandSlot
import net.ccbluex.liquidbounce.utils.inventory.PlayerInventoryConstraints
import net.ccbluex.liquidbounce.utils.inventory.Slots
import net.ccbluex.liquidbounce.utils.inventory.useHotbarSlotOrOffhand
import net.minecraft.world.entity.projectile.FireworkRocketEntity
import net.minecraft.world.item.Items

internal object ElytraFlyModeFirework : ElytraFlyMode("Firework") {

    private object ConsiderInventory : ToggleableValueGroup(this, "ConsiderInventory", enabled = false) {
        val constraints = tree(PlayerInventoryConstraints())
    }

    init {
        tree(ConsiderInventory)
    }

    private val cooldown by intRange("Cooldown", 20..20, 0..300, "ticks")

    private val ALL_WITHOUT_ARMOR = Slots.OffHand + Slots.Hotbar + Slots.Inventory
    private val slotsToSearch get() = if (ConsiderInventory.enabled) ALL_WITHOUT_ARMOR else Slots.OffhandWithHotbar

    private fun shouldUseFirework(): Boolean {
        return if (!player.isFallFlying or player.isUsingItem) {
            false
        } else {
            world.entitiesForRendering().none {
                it is FireworkRocketEntity && it.shooter === player
            }
        }
    }

    private var skipTicks = 0

    @Suppress("unused")
    private val tickHandler = handler<GameTickEvent> {
        skipTicks--
    }

    @Suppress("unused")
    private val scheduleInventoryActionHandler = handler<ScheduleInventoryActionEvent> { event ->
        if (skipTicks > 0 || !shouldUseFirework()) return@handler

        val fireworkSlot = slotsToSearch.findSlot(Items.FIREWORK_ROCKET) ?: return@handler
        if (fireworkSlot is HotbarItemSlot) {
            useHotbarSlotOrOffhand(fireworkSlot)
        } else {
            val actions = listOf<InventoryAction>(
                InventoryAction.Click.performSwap(from = fireworkSlot, to = OffHandSlot),
                InventoryAction.UseItem(OffHandSlot, this),
                InventoryAction.Click.performSwap(from = fireworkSlot, to = OffHandSlot),
            )
            event.schedule(ConsiderInventory.constraints, actions)
        }

        skipTicks = cooldown.random()
    }
}
