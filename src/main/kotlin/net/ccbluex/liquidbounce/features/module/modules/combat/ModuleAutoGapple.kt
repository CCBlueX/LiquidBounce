/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2016 - 2021 CCBlueX
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

package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.event.repeatable
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.minecraft.client.util.InputUtil
import net.minecraft.item.Items

object ModuleAutoGapple : Module("AutoGapple", Category.COMBAT) {

    val health by int("Health", 18, 1..20)

    var prevSlot = -1
    var eating = false
    var saveSlot = false

    override fun disable() {
        if (!InputUtil.isKeyPressed(mc.window.handle, mc.options.keyUse.boundKey.code)) {
            mc.options.keyUse.isPressed = false
        }
    }

    val repeatable = repeatable {
        val slot = (0..8).firstOrNull {
            player.inventory.getStack(it).item == Items.GOLDEN_APPLE
        }

        if (slot == null) {
            if (eating) {
                player.inventory.selectedSlot = prevSlot
            } else {
                return@repeatable
            }
        }

        if (player.isDead) {
            return@repeatable
        }

        if (player.health < health) {
            if (!saveSlot) {
                prevSlot = player.inventory.selectedSlot
                saveSlot = true
            }
            player.inventory.selectedSlot = slot!!
            eating = true
            mc.options.keyUse.isPressed = true
        }

        if (eating && player.health + player.absorptionAmount >= health) {
            saveSlot = false
            eating = false
            mc.options.keyUse.isPressed = false
            player.inventory.selectedSlot = prevSlot
        }
    }
}
