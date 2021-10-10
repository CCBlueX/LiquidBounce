/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.injection.backend

import net.ccbluex.liquidbounce.api.minecraft.client.entity.IEntityCreeper
import net.minecraft.entity.monster.EntityCreeper

class EntityCreeperImpl(wrapped: EntityCreeper) : EntityImpl<EntityCreeper>(wrapped), IEntityCreeper
{
	override val ignited: Boolean
		get() = wrapped.hasIgnited()
}
