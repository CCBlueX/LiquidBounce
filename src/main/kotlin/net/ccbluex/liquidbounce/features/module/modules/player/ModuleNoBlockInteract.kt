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

package net.ccbluex.liquidbounce.features.module.modules.player

import net.ccbluex.liquidbounce.event.events.GameTickEvent
import net.ccbluex.liquidbounce.event.events.MovementInputEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.injection.mixins.minecraft.client.MinecraftAccessor
import net.ccbluex.liquidbounce.utils.block.getBlock
import net.ccbluex.liquidbounce.utils.block.getState
import net.ccbluex.liquidbounce.utils.block.isInteractable
import net.ccbluex.liquidbounce.utils.item.isInteractable
import net.minecraft.world.phys.BlockHitResult

/**
 * NoBlockInteract module
 *
 * Allows to use items without interacting with blocks.
 */
object ModuleNoBlockInteract : ClientModule("NoBlockInteract", ModuleCategories.PLAYER) {

    private var sneaking = false
    private var interacting = false

    fun startSneaking() {
        sneaking = true
    }

    @Suppress("unused")
    private val handleMovementInput = handler<MovementInputEvent> { event ->
        if (sneaking) {
            event.sneak = true
            interacting = true
        }
    }

    @Suppress("unused")
    private val handleGameTick = handler<GameTickEvent> {
        if (interacting) {
            (mc as MinecraftAccessor).callStartUseItem()
            interacting = false
            sneaking = false
        }
    }

    fun shouldSneak(blockHitResult: BlockHitResult): Boolean {
        if (player.isShiftKeyDown) {
            return false
        }

        val blockPos = blockHitResult.blockPos
        if (!blockPos.getBlock().isInteractable(blockPos.getState())) {
            return false
        }

        return player.mainHandItem.isInteractable() || player.offhandItem.isInteractable()
    }
}
