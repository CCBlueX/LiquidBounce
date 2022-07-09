/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.injection.backend.minecraft.entity.projectile

import net.ccbluex.liquidbounce.api.minecraft.client.entity.IEntityThrowable
import net.ccbluex.liquidbounce.injection.backend.minecraft.entity.EntityImpl
import net.minecraft.entity.projectile.EntityThrowable

open class EntityThrowableImpl<out T : EntityThrowable>(wrapped: T) : EntityImpl<T>(wrapped), IEntityThrowable
{
    override val velocity: Float
        get() = wrapped.velocity
    override val inaccuracy: Float
        get() = wrapped.inaccuracy
    override val gravityVelocity: Float
        get() = wrapped.gravityVelocity
}
