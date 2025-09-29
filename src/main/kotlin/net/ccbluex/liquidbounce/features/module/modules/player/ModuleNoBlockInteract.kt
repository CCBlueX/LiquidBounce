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

package net.ccbluex.liquidbounce.features.module.modules.player

import net.ccbluex.liquidbounce.event.events.GameTickEvent
import net.ccbluex.liquidbounce.event.events.MovementInputEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.injection.mixins.minecraft.client.MinecraftClientAccessor
import net.ccbluex.liquidbounce.utils.block.getBlock
import net.ccbluex.liquidbounce.utils.block.getState
import net.ccbluex.liquidbounce.utils.block.isInteractable
import net.ccbluex.liquidbounce.utils.item.isInteractable
import net.minecraft.util.hit.BlockHitResult

/**
 * NoBlockInteract module
 *
 * Allows to use items without interacting with blocks.
 */
object ModuleNoBlockInteract : ClientModule("NoBlockInteract", Category.PLAYER) {

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
            (mc as MinecraftClientAccessor).callDoItemUse()
            interacting = false
            sneaking = false
        }
    }

    fun shouldSneak(blockHitResult: BlockHitResult): Boolean {
        if (player.isSneaking) {
            return false
        }

        val blockPos = blockHitResult.blockPos
        if (!blockPos.getBlock().isInteractable(blockPos.getState())) {
            return false
        }

        return player.mainHandStack.isInteractable() || player.offHandStack.isInteractable()
    }
}
