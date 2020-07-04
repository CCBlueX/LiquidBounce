/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.injection.backend

import net.ccbluex.liquidbounce.api.minecraft.util.IMovementInput
import net.minecraft.util.MovementInput

class MovementInputImpl(val wrapped: MovementInput) : IMovementInput {
    override val moveForward: Float
        get() = wrapped.moveForward
    override val moveStrafe: Float
        get() = wrapped.moveStrafe
    override val jump: Boolean
        get() = wrapped.jump

}

inline fun IMovementInput.unwrap(): MovementInput = (this as MovementInputImpl).wrapped
inline fun MovementInput.wrap(): IMovementInput = MovementInputImpl(this)