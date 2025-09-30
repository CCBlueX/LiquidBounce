package net.ccbluex.liquidbounce.utils.movement

import net.ccbluex.liquidbounce.utils.entity.untransformed
import net.minecraft.client.input.Input
import net.minecraft.util.PlayerInput

data class DirectionalInput(
    val forwards: Boolean,
    val backwards: Boolean,
    val left: Boolean,
    val right: Boolean,
) {

    constructor(input: Input) : this(
        input.untransformed
    )

    constructor(input: PlayerInput) : this(
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
