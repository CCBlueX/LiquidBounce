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

@file:Suppress("FunctionName", "PropertyName", "NOTHING_TO_INLINE", "CAST_NEVER_SUCCEEDS")

package net.ccbluex.liquidbounce.additions

import net.minecraft.network.protocol.game.ServerboundPlayerInputPacket
import net.minecraft.world.entity.player.Input

interface ServerboundPlayerInputPacketAddition {
    var `liquidBounce$forceSneak`: Boolean

    fun `liquidBounce$getRawInput`(): Input
}

/**
 * Changes the return value of record component [ServerboundPlayerInputPacket.input].
 */
inline var ServerboundPlayerInputPacket.forceSneak: Boolean
    get() = (this as ServerboundPlayerInputPacketAddition).`liquidBounce$forceSneak`
    set(value) {
        (this as ServerboundPlayerInputPacketAddition).`liquidBounce$forceSneak` = value
    }

inline val ServerboundPlayerInputPacket.rawInput: Input
    get() = (this as ServerboundPlayerInputPacketAddition).`liquidBounce$getRawInput`()
