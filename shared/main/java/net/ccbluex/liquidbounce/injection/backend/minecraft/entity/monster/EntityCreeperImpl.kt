/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.injection.backend.minecraft.entity.monster

import net.ccbluex.liquidbounce.api.minecraft.client.entity.IEntityCreeper
import net.ccbluex.liquidbounce.injection.backend.minecraft.entity.EntityImpl
import net.minecraft.entity.monster.EntityCreeper

class EntityCreeperImpl(wrapped: EntityCreeper) : EntityImpl<EntityCreeper>(wrapped), IEntityCreeper
{
    override val creeperState: Boolean
        get() = wrapped.creeperState == 1
}
