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
package net.ccbluex.liquidbounce.features.module.modules.movement.elytrafly.modes

import net.ccbluex.liquidbounce.config.types.nesting.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.events.ScheduleInventoryActionEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.utils.inventory.*

import net.minecraft.entity.projectile.FireworkRocketEntity
import net.minecraft.item.Items
import java.lang.reflect.Field

internal object ElytraFlyModeFirework : ElytraFlyMode("Firework") {

    private object ConsiderInventory : ToggleableConfigurable(this, "ConsiderInventory", enabled = false) {
        val constraints = tree(PlayerInventoryConstraints())
    }

    private val cooldown by int("Cooldown", 20, 0..300, "ticks")

    private val slotsToSearch = if (ConsiderInventory.enabled) {
        Slots.OffHand + Slots.Hotbar + Slots.Inventory
    } else {
        Slots.OffHand + Slots.Hotbar
    }

    fun getShooter(firework: FireworkRocketEntity): Any? {
        val shooterField: Field = firework.javaClass.getDeclaredField("shooter")
        shooterField.isAccessible = true
        return shooterField.get(firework)
    }

    private fun shouldUseFirework(): Boolean {
        if (!player.isGliding or player.isUsingItem) return false

        for (i in world.entities) {
            if (i is FireworkRocketEntity) {
                if (getShooter(i) == player) return false
            }
        }
        return true
    }

    private var skipTicks = 0

    @Suppress("unused")
    private val scheduleInventoryActionHandler = handler<ScheduleInventoryActionEvent> { event ->
        if (skipTicks > 0) {
            skipTicks--
            return@handler
        }
        if (shouldUseFirework()) {
            val fireworkSlot = slotsToSearch.findSlot(Items.FIREWORK_ROCKET) ?: return@handler
            if (fireworkSlot is HotbarItemSlot) {
                useHotbarSlotOrOffhand(fireworkSlot)
            } else {
                val actions = listOfNotNull(
                    InventoryAction.Click.performSwap(from = fireworkSlot, to = OffHandSlot),
                    InventoryAction.UseItem(OffHandSlot),
                    InventoryAction.Click.performSwap(from = fireworkSlot, to = OffHandSlot)
                )
                event.schedule( ConsiderInventory.constraints, actions)
            }
            skipTicks = cooldown
        }
    }
}
