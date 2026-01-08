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

package net.ccbluex.liquidbounce.utils.movement

import net.ccbluex.liquidbounce.utils.entity.untransformed
import net.minecraft.client.player.ClientInput
import net.minecraft.world.entity.player.Input

data class DirectionalInput(
    val forwards: Boolean,
    val backwards: Boolean,
    val left: Boolean,
    val right: Boolean,
) {

    constructor(input: ClientInput) : this(
        input.untransformed
    )

    constructor(input: Input) : this(
        input.forward,
        input.backward,
        input.left,
        input.right
    )

    constructor(movementForward: Float, movementSideways: Float) : this(
        forwards = movementForward > 0.0,
        backwards = movementForward < 0.0,
        left = movementSideways > 0.0,
        right = movementSideways < 0.0
    )

    fun invert(): DirectionalInput {
        return DirectionalInput(
            forwards = backwards,
            backwards = forwards,
            left = right,
            right = left
        )
    }

    val isMoving: Boolean
        get() = (forwards && !backwards) || (backwards && !forwards) ||
            (left && !right) || (right && !left)

    companion object {
        @JvmField
        val NONE = DirectionalInput(forwards = false, backwards = false, left = false, right = false)

        @JvmField
        val FORWARDS = DirectionalInput(forwards = true, backwards = false, left = false, right = false)

        @JvmField
        val BACKWARDS = DirectionalInput(forwards = false, backwards = true, left = false, right = false)

        @JvmField
        val LEFT = DirectionalInput(forwards = false, backwards = false, left = true, right = false)

        @JvmField
        val RIGHT = DirectionalInput(forwards = false, backwards = false, left = false, right = true)

        @JvmField
        val FORWARDS_LEFT = DirectionalInput(forwards = true, backwards = false, left = true, right = false)

        @JvmField
        val FORWARDS_RIGHT = DirectionalInput(forwards = true, backwards = false, left = false, right = true)

        @JvmField
        val BACKWARDS_LEFT = DirectionalInput(forwards = false, backwards = true, left = true, right = false)

        @JvmField
        val BACKWARDS_RIGHT = DirectionalInput(forwards = false, backwards = true, left = false, right = true)
    }
}
