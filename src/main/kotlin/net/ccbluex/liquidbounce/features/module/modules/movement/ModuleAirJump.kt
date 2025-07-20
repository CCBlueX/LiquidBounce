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
package net.ccbluex.liquidbounce.features.module.modules.movement

import net.ccbluex.liquidbounce.config.types.NamedChoice
import net.ccbluex.liquidbounce.event.events.BlockShapeEvent
import net.ccbluex.liquidbounce.event.events.PlayerJumpEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.minecraft.util.shape.VoxelShapes

object ModuleAirJump : ClientModule("AirJump", Category.MOVEMENT) {

    val mode by enumChoice("Mode", Mode.JUMP_FREELY)

    private var doubleJump = true

    val allowJump: Boolean
        get() = running && (mode == Mode.JUMP_FREELY || mode == Mode.DOUBLE_JUMP && doubleJump)

    val repeatable = tickHandler {
        if (player.isOnGround) {
            doubleJump = true
        }
    }

    @Suppress("unused")
    val jumpEvent = handler<PlayerJumpEvent> {
        if (doubleJump && !player.isOnGround) {
            doubleJump = false
        }
    }

    @Suppress("unused")
    val handleBlockBox = handler<BlockShapeEvent> { event ->
        if (mode == Mode.GHOST_BLOCK && event.pos.y < player.blockPos.y && mc.options.jumpKey.isPressed) {
            event.shape = VoxelShapes.fullCube()
        }
    }

    enum class Mode(override val choiceName: String) : NamedChoice {
        JUMP_FREELY("JumpFreely"),
        DOUBLE_JUMP("DoubleJump"),
        GHOST_BLOCK("GhostBlock"),
    }

}
