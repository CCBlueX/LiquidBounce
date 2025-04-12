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
package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.event.events.AttackEntityEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.utils.client.SilentHotbar
import net.ccbluex.liquidbounce.utils.entity.warp
import net.ccbluex.liquidbounce.utils.inventory.Slots
import net.ccbluex.liquidbounce.utils.inventory.findClosestSlot
import net.minecraft.item.Items
import net.minecraft.util.shape.VoxelShapes
import kotlin.math.abs
import kotlin.math.ceil

/**
 * Makes the mace powerful by faking fall height.
 */
object ModuleMaceKill : ClientModule("MaceKill", Category.COMBAT) {

    private val fallHeight by int("FallHeight", 22, 1..170).apply { tagBy(this) }

    init {
        // This module will likely not bypass any anti-cheat, so to prevent someone using it,
        // in case they don't know what it does, we allow it to be locked by config.
        enableLock()
    }

    @Suppress("unused")
    private val attackHandler = handler<AttackEntityEvent> { event ->
        // Check if player is holding a mace
        val mainHandStack = player.mainHandStack

        if (mainHandStack.item != Items.MACE) {
            // Auto Select Mace
            val maceIndex = Slots.Hotbar.findClosestSlot(Items.MACE) ?: return@handler

            SilentHotbar.selectSlotSilently(this, maceIndex, 1)
        }

        val height = determineHeight()

        // Use Paper/Spigot teleport exploit if height is greater than 10
        if (height > 10) {
            repeat(ceil(abs(height / 10.0)).toInt()) {
                player.warp(null, onGround = false)
            }
        } else {
            // Do it at least twice to neutralize horizontal distance
            repeat(2) { player.warp(null, onGround = player.isOnGround) }
        }

        // Teleport to the calculated height
        player.warp(player.pos.add(0.0, height.toDouble(), 0.0), onGround = false)

        // Make sure we get back to the ground
        player.warp(player.pos, onGround = false)
    }

    /**
     * In this case we can easily determine the height
     * by checking if the player would collide with a block
     * from the highest possible fall height to 1.
     *
     * We do not have to check in-between heights, because
     * we immediately teleport through the block and
     * Minecraft has no collision check for that.
     */
    private fun determineHeight(): Int {
        val boundingBox = player.boundingBox
        for (i in fallHeight downTo 1) {
            // Offset bounding box by i blocks
            val newBoundingBox = boundingBox.offset(0.0, i.toDouble(), 0.0)

            // Check if the player would collide with a block
            if (world.getBlockCollisions(player, newBoundingBox).all(VoxelShapes.empty()::equals)) {
                return i
            }
        }

        return 0
    }

}
