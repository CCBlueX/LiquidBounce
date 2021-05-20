/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.injection.backend

import net.ccbluex.liquidbounce.api.minecraft.client.entity.IEntityPotion
import net.minecraft.entity.projectile.EntityPotion

class EntityPotionImpl(wrapped: EntityPotion) : EntityImpl<EntityPotion>(wrapped), IEntityPotion
{
	override val potionDamage: Int
		get() = wrapped.potionDamage
}
