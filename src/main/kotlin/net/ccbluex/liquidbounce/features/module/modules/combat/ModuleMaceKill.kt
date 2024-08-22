/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2024 CCBlueX
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

import net.ccbluex.liquidbounce.event.events.AttackEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.client.chat
import net.minecraft.item.Items
import net.minecraft.util.shape.VoxelShapes

object ModuleMaceKill : Module("MaceKill", Category.COMBAT) {

    val fallHeight by int("FallHeight", 22, 1..170)

    @Suppress("unused")
    private val attackHandler = handler<AttackEvent> { event ->
        // Check if player is holding a mace
        val mainHandStack = player.mainHandStack

        if (mainHandStack.item != Items.MACE) {
            // TODO: Auto Select Mace
            return@handler
        }

        val target = event.enemy
        // TODO: Check if target might block the attack

        chat("Mace Kill ${determineHeight()}")

    }

    private fun determineHeight(): Int {
        val boundingBox = player.boundingBox
        for (i in fallHeight..1) {
            chat("Checking $i")
            
            val newBoundingBox = boundingBox.offset(0.0, i.toDouble(), 0.0)

            // Check if the player would collide with a block
            if (world.getBlockCollisions(player, newBoundingBox).all { shape -> shape == VoxelShapes.empty() }) {
                return i
            }
        }

        return 0
    }



}
