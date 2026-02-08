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
@file:Suppress("NOTHING_TO_INLINE", "LongParameterList")

package net.ccbluex.liquidbounce.utils.entity

import net.minecraft.client.player.ClientInput
import net.minecraft.world.entity.player.Input

inline val Input.any: Boolean
    get() = forward || backward || left || right

inline fun Input.copy(
    forward: Boolean = this.forward,
    backward: Boolean = this.backward,
    left: Boolean = this.left,
    right: Boolean = this.right,
    jump: Boolean = this.jump,
    sneak: Boolean = this.shift,
    sprint: Boolean = this.sprint
): Input {
    return Input(
        forward,
        backward,
        left,
        right,
        jump,
        sneak,
        sprint
    )
}

inline fun ClientInput.set(
    forward: Boolean = keyPresses.forward,
    backward: Boolean = keyPresses.backward,
    left: Boolean = keyPresses.left,
    right: Boolean = keyPresses.right,
    jump: Boolean = keyPresses.jump,
    sneak: Boolean = keyPresses.shift,
    sprint: Boolean = keyPresses.sprint
) {
    this.keyPresses = Input(
        forward,
        backward,
        left,
        right,
        jump,
        sneak,
        sprint
    )
}
