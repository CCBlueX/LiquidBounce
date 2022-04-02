/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.injection.backend

import net.ccbluex.liquidbounce.api.minecraft.client.entity.IEntityTNTPrimed
import net.minecraft.entity.item.EntityTNTPrimed

class EntityTNTPrimedImpl(wrapped: EntityTNTPrimed) : EntityImpl<EntityTNTPrimed>(wrapped), IEntityTNTPrimed {
    override val fuse: Int
        get() = wrapped.fuse
}