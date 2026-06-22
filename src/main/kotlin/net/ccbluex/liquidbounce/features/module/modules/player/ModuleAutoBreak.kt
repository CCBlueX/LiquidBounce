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

import net.ccbluex.liquidbounce.event.events.KeybindIsPressedEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.utils.block.getState
import net.minecraft.client.KeyMapping
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.HitResult

/**
 * AutoBreak module
 *
 * Automatically breaks blocks.
 */
object ModuleAutoBreak : ClientModule("AutoBreak", ModuleCategories.PLAYER) {

    @Suppress("unused")
    private val keybindIsPressedHandler = handler<KeybindIsPressedEvent> { event ->
        if (event.keyBinding == mc.options.keyAttack && mc.missTime <= 0) {
            val crosshairTarget = mc.hitResult

            if (crosshairTarget is BlockHitResult && crosshairTarget.type == HitResult.Type.BLOCK) {
                val blockState = crosshairTarget.blockPos.getState() ?: return@handler
                if (blockState.isAir) {
                    return@handler
                }

                if (!interaction.isDestroying) {
                    // First click
                    KeyMapping.click(mc.options.keyAttack.key)
                }
                event.isPressed = true
            }
        }
    }

}
