/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.injection.backend.minecraft.entity.projectile

import net.ccbluex.liquidbounce.api.minecraft.client.entity.IEntityArrow
import net.ccbluex.liquidbounce.injection.backend.minecraft.entity.EntityImpl
import net.minecraft.entity.projectile.EntityArrow

class EntityArrowImpl(wrapped: EntityArrow) : EntityImpl<EntityArrow>(wrapped), IEntityArrow
{
    override val inGround: Boolean
        get() = wrapped.inGround
}
